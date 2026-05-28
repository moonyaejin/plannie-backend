package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.in.AiChatUseCase.ExtractedPlanParams;
import com.plannie.application.port.out.AiChatWithGptPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatAdapter implements AiChatWithGptPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "chatFallback")
    @Retry(name = "openai")
    public String chat(String userMessage, List<OpenAiRequest.Message> history) {
        List<OpenAiRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAiRequest.Message("system", buildSystemPrompt(LocalDate.now().toString())));
        messages.addAll(history);
        messages.add(new OpenAiRequest.Message("user", userMessage));
        OpenAiRequest request = new OpenAiRequest(openAiProperties.model(), messages);

        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        return response.choices().get(0).message().content();
    }

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "extractFallback")
    @Retry(name = "openai")
    public ExtractedPlanParams extractPlanParams(String conversationContext) {
        String today = LocalDate.now().toString();

        OpenAiRequest request = new OpenAiRequest(
                openAiProperties.model(),
                List.of(
                        new OpenAiRequest.Message("system", buildExtractionPrompt(today)),
                        new OpenAiRequest.Message("user", conversationContext)));

        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }

        String content = response.choices().get(0).message().content().strip();
        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").strip();
        }

        try {
            GptExtractedParams gpt = objectMapper.readValue(content, GptExtractedParams.class);
            if (Boolean.TRUE.equals(gpt.cannotExtract()) || gpt.examName() == null || gpt.examDate() == null) {
                return null;
            }
            return new ExtractedPlanParams(
                    gpt.examName(),
                    LocalDate.parse(gpt.examDate()),
                    gpt.dailyHours() != null ? gpt.dailyHours() : 2,
                    gpt.focusAreas(),
                    gpt.userRequest());
        } catch (Exception e) {
            log.warn("GPT 파라미터 추출 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private ExtractedPlanParams extractFallback(String conversationContext, Throwable t) {
        log.warn("GPT 파라미터 추출 fallback: {}", t.getMessage());
        return null;
    }

    private String chatFallback(String userMessage, List<OpenAiRequest.Message> history, Throwable t) {
        if (t instanceof BusinessException e)
            throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private String buildSystemPrompt(String today) {
        return ("""
                당신은 플래니(Plannie)의 AI 플래너입니다.
                오늘 날짜: %s

                ## 역할
                사용자가 공부 계획을 요청하면 반드시 아래 단계를 순서대로 따르세요.
                일반 대화에도 자유롭게 응답하세요.

                ## 단계별 행동 지침

                ### 1단계: 정보 수집
                계획 생성에 필요한 정보가 부족하면 자연스럽게 질문하세요.
                - 필수: 시험명, 시험일(또는 기간), 하루 공부 시작·종료 시간
                - 선택: 취약 영역

                ### 2단계: 계획 제안
                오늘 날짜를 기준으로 시험일까지 정확한 기간을 계산하여 제안하세요.
                필수 정보가 모두 갖춰지면, 아래 형식으로 계획을 요약 제안하세요.
                반드시 마지막에 이 문장을 포함하세요: "이대로 생성할까요? 수정할 부분이 있으면 알려주세요 😊"
                - 정확한 총 학습 기간 (오늘 날짜~시험 전날, 일 수 포함)
                - 단계 구성 (개념→실전→마무리)
                - 하루 학습 시간과 세션 구성
                - 중점 영역 반영 여부

                ### 3단계: 수정 요청 처리
                사용자가 수정을 요청하면 수정 내용을 반영한 계획을 다시 제안하고,
                마지막에 동일하게 "이대로 생성할까요?" 문장을 포함하세요.

                ### 4단계: 생성 확인
                아래 조건이 둘 다 충족될 때만 {"action": "GENERATE_PLAN"}을 반환하세요.
                - 조건 A: 직전 당신의 응답에서 계획을 제안하고 "이대로 생성할까요?"라고 물었다
                - 조건 B: 사용자가 그 제안에 동의했다 ("좋아", "응", "ㅇㅇ", "그래", "생성해줘", "해줘", "확인" 등)

                ⛔ 절대 금지:
                - 사용자가 처음으로 계획을 요청한 메시지에 {"action": "GENERATE_PLAN"}을 반환하는 것
                - 당신이 아직 "이대로 생성할까요?"를 묻지 않은 상태에서 {"action": "GENERATE_PLAN"}을 반환하는 것
                - "계획 짜줘", "만들어줘", "생성해줘"가 계획 요청 문맥이라면 이는 확인이 아니라 요청입니다. 반드시 2단계(제안)로 응답하세요.

                ## 주의사항
                - 계획 제안은 2~5문장 요약만 (전체 스케줄 목록 나열 금지)
                - 모든 응답은 한국어, 친근한 말투
                """).formatted(today);
    }

    private String buildExtractionPrompt(String today) {
        return """
                당신은 대화에서 학습 계획 파라미터를 추출하는 시스템입니다.
                오늘 날짜: %s

                아래 대화 내용을 분석해서 학습 계획 생성에 필요한 정보를 추출하고, 반드시 아래 JSON 형식만 반환하세요 (다른 텍스트 없이):

                {
                  "examName": "시험명 (예: 토익, 수능, 공무원, 한국사)",
                  "examDate": "YYYY-MM-DD (오늘로부터 계산, '한달' = 30일, '2주' = 14일)",
                  "dailyHours": 하루_학습_시간_숫자,
                  "focusAreas": ["취약 영역"] 또는 null,
                  "userRequest": "사용자 요청 핵심 내용 한 줄 요약"
                }

                시험명이나 시험일을 파악할 수 없으면 반드시 아래만 반환:
                {"cannotExtract": true}
                """.formatted(today);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GptExtractedParams(
            Boolean cannotExtract,
            String examName,
            String examDate,
            Integer dailyHours,
            List<String> focusAreas,
            String userRequest) {
    }
}

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
        messages.add(new OpenAiRequest.Message("system", buildSystemPrompt()));
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
                        new OpenAiRequest.Message("user", conversationContext)
                )
        );

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
                    gpt.userRequest()
            );
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
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private String buildSystemPrompt() {
        return """
                당신은 플래니(Plannie)라는 한국 일정 관리·공부 계획 앱의 AI 어시스턴트입니다.
                사용자와 친근하고 자연스럽게 한국어로 대화하세요.
                일정 관리, 공부 계획, 시간 관리 주제를 중심으로 도움을 주되, 일반 대화에도 자유롭게 응답하세요.
                답변은 2~4문장으로 간결하게 작성하세요.
                """;
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
            String userRequest
    ) {}
}

package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.GptStudyPlan;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiStudyPlanAdapter implements GenerateStudyPlanWithAiPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generateFallback")
    @Retry(name = "openai")
    public AiStudyPlan generate(GenerateStudyPlanCommand command) {
        OpenAiRequest request = buildRequest(command);

        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        String content = response.choices().get(0).message().content();
        return parseResponse(content);
    }

    private AiStudyPlan generateFallback(GenerateStudyPlanCommand command, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private OpenAiRequest buildRequest(GenerateStudyPlanCommand command) {
        return new OpenAiRequest(
                openAiProperties.model(),
                List.of(
                        new OpenAiRequest.Message("system", buildSystemPrompt()),
                        new OpenAiRequest.Message("user", buildUserMessage(command))
                )
        );
    }

    private String buildSystemPrompt() {
        return """
                You are a study plan assistant for Korean students.
                Create a detailed study schedule based on the given exam info and return ONLY a JSON object.

                You MUST follow this exact JSON schema:
                {
                  "plan_summary": "전체 학습 계획 요약 (2-3문장, 한국어)",
                  "weekly_goals": ["1주차 목표", "2주차 목표", ...],
                  "schedules": [
                    {
                      "title": "학습 내용 (예: 엑셀 함수 챕터 1-3)",
                      "memo": "세부 내용 또는 null",
                      "date": "YYYY-MM-DD",
                      "start_time": "HH:mm",
                      "end_time": "HH:mm",
                      "week": 1
                    }
                  ]
                }

                Rules:
                - 모든 title, memo, plan_summary, weekly_goals는 한국어로 작성
                - daily_hours에 맞게 start_time, end_time 설정 (예: 2시간이면 20:00~22:00)
                - 주말 포함하여 매일 일정 생성
                - 시험 당일은 "최종 모의고사" 일정으로 마무리
                - Return only raw JSON, no markdown code block
                """;
    }

    private String buildUserMessage(GenerateStudyPlanCommand command) {
        String focusAreas = command.focusAreas() == null || command.focusAreas().isEmpty()
                ? "없음"
                : String.join(", ", command.focusAreas());

        return """
                시험명: %s
                교재: %s
                시작일: %s
                시험일: %s
                하루 학습 시간: %d시간
                기출문제 회독 수: %d회
                중점 학습 영역: %s
                """.formatted(
                command.examName(),
                command.textbook() != null ? command.textbook() : "미지정",
                command.startDate(),
                command.examDate(),
                command.dailyHours(),
                command.pastExamRounds(),
                focusAreas
        );
    }

    private AiStudyPlan parseResponse(String content) {
        String cleanContent = content.strip();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent
                    .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                    .replaceAll("```\\s*$", "")
                    .strip();
        }

        log.debug("GPT study plan response: {}", cleanContent);

        GptStudyPlan gpt;
        try {
            gpt = objectMapper.readValue(cleanContent, GptStudyPlan.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, "학습 계획 파싱 실패: " + e.getOriginalMessage());
        }

        List<AiScheduleItem> items = gpt.schedules().stream()
                .map(s -> new AiScheduleItem(s.title(), s.memo(), s.date(), s.startTime(), s.endTime(), s.week()))
                .toList();

        return new AiStudyPlan(gpt.planSummary(), gpt.weeklyGoals(), items);
    }
}

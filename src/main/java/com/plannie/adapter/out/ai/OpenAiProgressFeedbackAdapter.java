package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.GptProgressFeedback;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.out.GenerateProgressFeedbackWithAiPort;
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
public class OpenAiProgressFeedbackAdapter implements GenerateProgressFeedbackWithAiPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generateFallback")
    @Retry(name = "openai")
    public AiFeedback generate(FeedbackRequest request) {
        OpenAiRequest openAiRequest = new OpenAiRequest(
                openAiProperties.model(),
                List.of(
                        new OpenAiRequest.Message("system", buildSystemPrompt()),
                        new OpenAiRequest.Message("user", buildUserMessage(request))
                )
        );

        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        String content = response.choices().get(0).message().content();
        return parseResponse(content);
    }

    private AiFeedback generateFallback(FeedbackRequest request, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private String buildSystemPrompt() {
        return """
                You are a personal productivity coach for a Korean user.
                Analyze the weekly schedule data and provide detailed, actionable feedback.
                Return ONLY a JSON object with this exact schema:
                {
                  "summary": "이번 주 전체 분석 요약 (2-3문장, 한국어)",
                  "strengths": ["잘한 점1", "잘한 점2"],
                  "improvements": ["개선할 점1", "개선할 점2"],
                  "next_week_advice": "다음 주를 위한 구체적인 조언 (1-2문장, 한국어)"
                }

                Rules:
                - All text must be in Korean
                - Be specific and reference the actual schedule items
                - Completion rate guidance: ≥80% → encourage and build on success, 50-79% → balanced feedback, <50% → motivate with actionable tips
                - Return only raw JSON, no markdown code block
                """;
    }

    private String buildUserMessage(FeedbackRequest request) {
        String titleList = request.scheduleTitles().isEmpty()
                ? "이번 주 일정 없음"
                : String.join("\n", request.scheduleTitles());

        return """
                분석 기간: %s ~ %s
                전체 일정 수: %d개
                완료한 일정: %d개
                완료율: %.1f%%

                일정 목록:
                %s
                """.formatted(
                request.weekStart(),
                request.weekEnd(),
                request.totalSchedules(),
                request.completedSchedules(),
                request.completionRate(),
                titleList
        );
    }

    private AiFeedback parseResponse(String content) {
        String cleanContent = content.strip();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent
                    .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                    .replaceAll("```\\s*$", "")
                    .strip();
        }

        log.debug("GPT progress feedback response: {}", cleanContent);

        GptProgressFeedback gpt;
        try {
            gpt = objectMapper.readValue(cleanContent, GptProgressFeedback.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, "진도 피드백 파싱 실패: " + e.getOriginalMessage());
        }

        return new AiFeedback(gpt.summary(), gpt.strengths(), gpt.improvements(), gpt.nextWeekAdvice());
    }
}

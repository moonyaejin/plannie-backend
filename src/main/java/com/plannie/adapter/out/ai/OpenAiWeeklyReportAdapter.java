package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.GptWeeklyReport;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiWeeklyReportAdapter implements GenerateWeeklyReportWithAiPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generateFallback")
    @Retry(name = "openai")
    public AiWeeklyReport generate(ReportRequest request) {
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

        return parseResponse(response.choices().get(0).message().content());
    }

    private AiWeeklyReport generateFallback(ReportRequest request, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private String buildSystemPrompt() {
        return """
                You are a personal productivity coach for a Korean user.
                Analyze the weekly activity data including schedule completion and study time, then provide detailed, actionable feedback.
                Return ONLY a JSON object with this exact schema:
                {
                  "summary": "이번 주 전체 분석 요약 (2-3문장, 한국어)",
                  "strengths": ["잘한 점1", "잘한 점2"],
                  "improvements": ["개선할 점1", "개선할 점2"],
                  "next_week_advice": "다음 주를 위한 구체적인 조언 (1-2문장, 한국어)"
                }

                Rules:
                - All text must be in Korean
                - Reference both schedule completion AND study time in your analysis
                - Completion rate guidance: ≥80% → encourage, 50-79% → balanced, <50% → motivate
                - If study time data exists, mention specific subjects and time invested
                - Return only raw JSON, no markdown code block
                """;
    }

    private String buildUserMessage(ReportRequest request) {
        String scheduleTitles = request.scheduleTitles().isEmpty()
                ? "이번 주 일정 없음"
                : String.join("\n", request.scheduleTitles());

        String studySummary = request.studyBySubject().isEmpty()
                ? "이번 주 공부 기록 없음"
                : request.studyBySubject().stream()
                        .map(s -> "  - %s: %d분".formatted(s.subjectName(), s.totalMinutes()))
                        .collect(Collectors.joining("\n"));

        return """
                분석 기간: %s ~ %s

                [일정 현황]
                전체 일정: %d개 / 완료: %d개 / 완료율: %.1f%%
                일정 목록:
                %s

                [공부 시간]
                총 공부 시간: %d분 (%d시간 %d분)
                과목별 공부 시간:
                %s
                """.formatted(
                request.weekStart(), request.weekEnd(),
                request.totalSchedules(), request.completedSchedules(), request.completionRate(),
                scheduleTitles,
                request.totalStudyMinutes(),
                request.totalStudyMinutes() / 60, request.totalStudyMinutes() % 60,
                studySummary
        );
    }

    private AiWeeklyReport parseResponse(String content) {
        String clean = content.strip();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("(?s)^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").strip();
        }

        log.debug("GPT weekly report response: {}", clean);

        try {
            GptWeeklyReport gpt = objectMapper.readValue(clean, GptWeeklyReport.class);
            return new AiWeeklyReport(gpt.summary(), gpt.strengths(), gpt.improvements(), gpt.nextWeekAdvice());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, "주간 리포트 파싱 실패: " + e.getOriginalMessage());
        }
    }
}

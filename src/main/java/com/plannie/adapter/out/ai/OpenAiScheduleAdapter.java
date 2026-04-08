package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.GptParsedSchedule;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.out.ParseScheduleWithAiPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.RepeatRule;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiScheduleAdapter implements ParseScheduleWithAiPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "parseFallback")
    @Retry(name = "openai")
    public ParsedScheduleResult parse(String naturalLanguage, LocalDate today) {
        OpenAiRequest request = buildRequest(naturalLanguage, today);

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
        return toResult(content, today);
    }

    private ParsedScheduleResult parseFallback(String naturalLanguage, LocalDate today, Throwable t) {
        if (t instanceof BusinessException e) {
            throw e;
        }
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR,
                    "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private OpenAiRequest buildRequest(String naturalLanguage, LocalDate today) {
        return new OpenAiRequest(
                openAiProperties.model(),
                List.of(
                        new OpenAiRequest.Message("system", buildSystemPrompt(today)),
                        new OpenAiRequest.Message("user", naturalLanguage)
                )
        );
    }

    private String buildSystemPrompt(LocalDate today) {
        return """
                You are a schedule parsing assistant for a Korean calendar app.
                Extract schedule information from Korean natural language input and return ONLY a JSON object.

                Today's date: %s

                Rules:
                - "내일" = today + 1 day, "다음 주" = next week Monday
                - "오후 N시" = (N+12):00, "오전 N시" = N:00, "정오" = 12:00
                - If end time is not mentioned, set end_time = start_time + 1 hour
                - If date is not mentioned, assume today
                - If time is not mentioned, set start_time and end_time to null
                - Return only raw JSON, no explanation, no markdown code block
                - If you cannot parse the input, return: {"parse_failed": true, "reason": "<reason in Korean>"}

                You MUST follow this exact JSON schema:
                {
                  "title": string,
                  "memo": string | null,
                  "start_date": "YYYY-MM-DD",
                  "end_date": "YYYY-MM-DD",
                  "start_time": "HH:mm" | null,
                  "end_time": "HH:mm" | null,
                  "repeat_rule": {
                    "type": "NONE" | "DAILY" | "WEEKLY" | "MONTHLY",
                    "days_of_week": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"] | null,
                    "day_of_month": number | null,
                    "end_date": "YYYY-MM-DD" | null
                  }
                }

                Examples:
                Input: "내일 오후 3시 팀 회의"
                Output: {"title":"팀 회의","memo":null,"start_date":"%s","end_date":"%s","start_time":"15:00","end_time":"16:00","repeat_rule":{"type":"NONE","days_of_week":null,"day_of_month":null,"end_date":null}}

                Input: "매주 월요일 오전 10시 스탠드업"
                Output: {"title":"스탠드업","memo":null,"start_date":"%s","end_date":"%s","start_time":"10:00","end_time":"11:00","repeat_rule":{"type":"WEEKLY","days_of_week":["MONDAY"],"day_of_month":null,"end_date":null}}
                """.formatted(today,
                today.plusDays(1), today.plusDays(1),
                today, today);
    }

    private ParsedScheduleResult toResult(String content, LocalDate today) {
        // GPT가 마크다운 코드블록으로 감쌀 때 제거 (```json ... ``` 또는 ``` ... ```)
        String cleanContent = content.strip();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent
                    .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                    .replaceAll("```\\s*$", "")
                    .strip();
        }
        log.debug("GPT raw response: {}", cleanContent);

        GptParsedSchedule gpt;
        try {
            gpt = objectMapper.readValue(cleanContent, GptParsedSchedule.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR,
                    "GPT 응답 형식 오류: " + e.getOriginalMessage());
        }

        if (Boolean.TRUE.equals(gpt.parseFailed())) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, gpt.reason());
        }

        if (gpt.title() == null || gpt.title().isBlank()) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, "일정 제목을 인식할 수 없습니다.");
        }

        LocalDate startDate = parseDate(gpt.startDate(), today);
        // end_date null이면 start_date로 채움
        LocalDate endDate = gpt.endDate() != null ? parseDate(gpt.endDate(), today) : startDate;

        return new ParsedScheduleResult(
                gpt.title(),
                gpt.memo(),
                startDate,
                endDate,
                parseTime(gpt.startTime()),
                parseTime(gpt.endTime()),
                parseRepeatType(gpt.repeatRule()),
                parseDaysOfWeek(gpt.repeatRule()),
                gpt.repeatRule() != null ? gpt.repeatRule().dayOfMonth() : null,
                parseRepeatEndDate(gpt.repeatRule())
        );
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private RepeatRule.RepeatType parseRepeatType(GptParsedSchedule.RepeatRuleDto dto) {
        if (dto == null || dto.type() == null) return RepeatRule.RepeatType.NONE;
        try {
            return RepeatRule.RepeatType.valueOf(dto.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown repeat type from GPT: {}", dto.type());
            return RepeatRule.RepeatType.NONE;
        }
    }

    private Set<DayOfWeek> parseDaysOfWeek(GptParsedSchedule.RepeatRuleDto dto) {
        if (dto == null || dto.daysOfWeek() == null || dto.daysOfWeek().isEmpty()) return null;
        return dto.daysOfWeek().stream()
                .map(d -> DayOfWeek.valueOf(d.toUpperCase()))
                .collect(Collectors.toSet());
    }

    private LocalDate parseRepeatEndDate(GptParsedSchedule.RepeatRuleDto dto) {
        if (dto == null || dto.endDate() == null) return null;
        try {
            return LocalDate.parse(dto.endDate());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

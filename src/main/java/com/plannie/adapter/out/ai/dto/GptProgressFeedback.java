package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GPT가 반환하는 주간 진도 피드백 JSON DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GptProgressFeedback(
        String summary,
        List<String> strengths,
        List<String> improvements,
        @JsonProperty("next_week_advice") String nextWeekAdvice
) {}

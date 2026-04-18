package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GptWeeklyReport(
        String summary,
        List<String> strengths,
        List<String> improvements,
        @JsonProperty("next_week_advice") String nextWeekAdvice
) {}

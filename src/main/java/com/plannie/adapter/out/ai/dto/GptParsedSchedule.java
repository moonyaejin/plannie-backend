package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GPT가 반환하는 JSON을 역직렬화하는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GptParsedSchedule(
        @JsonProperty("parse_failed") Boolean parseFailed,
        String reason,
        String title,
        String memo,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("end_date") String endDate,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("end_time") String endTime,
        @JsonProperty("repeat_rule") RepeatRuleDto repeatRule
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepeatRuleDto(
            String type,
            @JsonProperty("days_of_week") List<String> daysOfWeek,
            @JsonProperty("day_of_month") Integer dayOfMonth,
            @JsonProperty("end_date") String endDate
    ) {}
}

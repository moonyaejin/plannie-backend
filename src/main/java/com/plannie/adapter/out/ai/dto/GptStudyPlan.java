package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GPT가 반환하는 학습 계획 JSON DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GptStudyPlan(
        @JsonProperty("plan_summary") String planSummary,
        @JsonProperty("weekly_goals") List<String> weeklyGoals,
        List<ScheduleItem> schedules
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleItem(
            String title,
            String memo,
            String date,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("end_time") String endTime,
            int week
    ) {}
}

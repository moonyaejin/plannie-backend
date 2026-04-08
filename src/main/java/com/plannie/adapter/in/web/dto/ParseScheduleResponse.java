package com.plannie.adapter.in.web.dto;

import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ParseScheduleResponse(
        Long id,
        Long userId,
        String title,
        String memo,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean completed,
        RepeatRuleResponse repeatRule
) {
    public static ParseScheduleResponse from(Schedule schedule) {
        return new ParseScheduleResponse(
                schedule.getId(),
                schedule.getUserId(),
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isCompleted(),
                RepeatRuleResponse.from(schedule.getRepeatRule())
        );
    }

    public record RepeatRuleResponse(
            RepeatRule.RepeatType type,
            Set<DayOfWeek> daysOfWeek,
            Integer dayOfMonth,
            LocalDate endDate
    ) {
        public static RepeatRuleResponse from(RepeatRule rule) {
            if (rule == null) return new RepeatRuleResponse(RepeatRule.RepeatType.NONE, null, null, null);
            return new RepeatRuleResponse(rule.getType(), rule.getDaysOfWeek(), rule.getDayOfMonth(), rule.getEndDate());
        }
    }
}

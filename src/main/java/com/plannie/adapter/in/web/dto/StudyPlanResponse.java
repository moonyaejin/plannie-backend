package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.GenerateStudyPlanUseCase.StudyPlanResult;
import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record StudyPlanResponse(
        String planSummary,
        List<String> weeklyGoals,
        int totalSchedules,
        List<ScheduleItem> schedules
) {
    public static StudyPlanResponse from(StudyPlanResult result) {
        List<ScheduleItem> items = result.schedules().stream()
                .map(ScheduleItem::from)
                .toList();

        return new StudyPlanResponse(
                result.planSummary(),
                result.weeklyGoals(),
                items.size(),
                items
        );
    }

    public record ScheduleItem(
            Long id,
            String title,
            String memo,
            LocalDate startDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public static ScheduleItem from(Schedule schedule) {
            return new ScheduleItem(
                    schedule.getId(),
                    schedule.getTitle(),
                    schedule.getMemo(),
                    schedule.getStartDate(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            );
        }
    }
}

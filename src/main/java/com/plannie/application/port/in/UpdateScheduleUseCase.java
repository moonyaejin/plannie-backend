package com.plannie.application.port.in;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 수정 유스케이스
 */
public interface UpdateScheduleUseCase {

    Schedule updateSchedule(UpdateScheduleCommand command);

    void toggleComplete(Long scheduleId, Long userId);

    void toggleRecurringComplete(Long scheduleId, LocalDate date, Long userId);

    record UpdateScheduleCommand(
            Long scheduleId,
            Long userId,
            String title,
            String memo,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            Long categoryId
    ) {}
}

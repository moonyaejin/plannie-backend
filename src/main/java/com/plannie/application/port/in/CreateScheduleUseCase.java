package com.plannie.application.port.in;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 생성 유스케이스
 */
public interface CreateScheduleUseCase {

    Schedule createSchedule(CreateScheduleCommand command);

    record CreateScheduleCommand(
            Long userId,
            String title,
            String memo,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            Long categoryId,
            String repeatType,
            String repeatDays,  // "MON,TUE,WED" 형식
            LocalDate repeatEndDate,
            Integer reminderMinutes  // null = 알림 없음, 5/10/30 = X분 전 알림
    ) {}
}

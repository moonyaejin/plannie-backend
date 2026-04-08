package com.plannie.application.port.in;

import com.plannie.adapter.in.web.dto.ScheduleView;
import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일정 조회 유스케이스
 */
public interface GetScheduleUseCase {

    /**
     * 일정 단건 조회
     */
    Optional<Schedule> getSchedule(Long scheduleId, Long userId);

    /**
     * 특정 날짜의 일정 목록 조회
     */
    List<Schedule> getSchedulesByDate(Long userId, LocalDate date);

    /**
     * 월별 일정 목록 조회
     */
    List<ScheduleView> getSchedulesByMonth(Long userId, int year, int month);

    /**
     * 기간별 일정 목록 조회
     */
    List<ScheduleView> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}

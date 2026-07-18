package com.plannie.application.port.in;

import java.time.LocalDate;

/**
 * 일정 삭제 유스케이스
 */
public interface DeleteScheduleUseCase {

    /**
     * @param occurrenceDate null = 시리즈 전체 삭제, non-null = 반복 일정의 해당 날짜 occurrence만 삭제
     */
    void deleteSchedule(Long scheduleId, Long userId, LocalDate occurrenceDate);

    void bulkDeleteSchedules(Long userId, Integer year, Integer month);
}
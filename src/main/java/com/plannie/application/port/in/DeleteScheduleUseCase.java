package com.plannie.application.port.in;

/**
 * 일정 삭제 유스케이스
 */
public interface DeleteScheduleUseCase {

    void deleteSchedule(Long scheduleId, Long userId);

    void bulkDeleteSchedules(Long userId, Integer year, Integer month);
}
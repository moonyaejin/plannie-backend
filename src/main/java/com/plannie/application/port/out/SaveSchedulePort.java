package com.plannie.application.port.out;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 저장 포트
 * - Persistence Adapter가 구현
 */
public interface SaveSchedulePort {

    Schedule save(Schedule schedule);

    void delete(Long scheduleId);

    void toggleComplete(Long scheduleId);

    void toggleCompletion(Long scheduleId, LocalDate date);

    void bulkDelete(Long userId, Integer year, Integer month);
}

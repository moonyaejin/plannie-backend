package com.plannie.application.port.out;

import com.plannie.domain.schedule.Schedule;

/**
 * 일정 저장 포트
 * - Persistence Adapter가 구현
 */
public interface SaveSchedulePort {

    Schedule save(Schedule schedule);

    void delete(Long scheduleId);
}

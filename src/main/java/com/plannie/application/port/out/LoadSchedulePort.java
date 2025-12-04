package com.plannie.application.port.out;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일정 조회 포트
 * - Persistence Adapter가 구현
 */
public interface LoadSchedulePort {

    Optional<Schedule> findById(Long id);

    Optional<Schedule> findByIdAndUserId(Long id, Long userId);

    List<Schedule> findByUserIdAndDate(Long userId, LocalDate date);

    List<Schedule> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 시간대에 겹치는 일정 조회 (충돌 감지용)
     */
    List<Schedule> findConflictingSchedules(Long userId, LocalDate date, 
                                             java.time.LocalTime startTime, 
                                             java.time.LocalTime endTime);

    /**
     * 비관적 락을 사용한 조회 (동시성 제어)
     */
    Optional<Schedule> findByIdWithLock(Long id);
}

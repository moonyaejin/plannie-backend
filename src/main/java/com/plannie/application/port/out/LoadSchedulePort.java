package com.plannie.application.port.out;

import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadSchedulePort {
    Optional<Schedule> findById(Long id);
    Optional<Schedule> findByIdAndUserId(Long id, Long userId);
    List<Schedule> findByUserIdAndDate(Long userId, LocalDate date);
    List<Schedule> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    List<Schedule> findConflictingSchedules(Long userId, LocalDate date, LocalTime startTime, LocalTime endTime);
    Optional<Schedule> findByIdWithLock(Long id);
    List<Schedule> findRepeatingSchedules(Long userId);
    List<Schedule> findOneTimeSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    // 반복 일정 예외/완료 조회
    Map<String, ScheduleException> findExceptions(List<Long> scheduleIds, LocalDate startDate, LocalDate endDate);
    Map<String, Boolean> findCompletions(List<Long> scheduleIds, LocalDate startDate, LocalDate endDate);
}
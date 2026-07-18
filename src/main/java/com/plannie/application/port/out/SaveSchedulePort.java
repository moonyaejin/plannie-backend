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

    /**
     * 반복 일정의 특정 occurrence만 수정 (ScheduleException 생성/갱신)
     */
    void saveOccurrenceModification(Long scheduleId, LocalDate date, String title, String memo,
                                    LocalTime startTime, LocalTime endTime);

    /**
     * 반복 일정의 특정 occurrence만 삭제 (ScheduleException 생성/갱신)
     */
    void deleteOccurrence(Long scheduleId, LocalDate date);
}

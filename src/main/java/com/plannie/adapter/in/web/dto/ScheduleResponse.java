package com.plannie.adapter.in.web.dto;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 일정 응답 DTO
 *
 * 도메인 객체(Schedule)를 API 응답용으로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 내부 구현 숨김
 */
public record ScheduleResponse(
        Long id,
        String title,
        String memo,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean completed,
        Long categoryId,
        String repeatType,
        String repeatDays,
        Integer reminderMinutes
) {
    /**
     * 도메인 객체 → DTO 변환
     *
     * 정적 팩토리 메서드 패턴:
     * - new 대신 의미있는 이름으로 객체 생성
     * - from, of, valueOf 등 관례적인 이름 사용
     */
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isCompleted(),
                schedule.getCategoryId(),
                schedule.getRepeatRule() != null ?
                        schedule.getRepeatRule().getType().name() : "NONE",
                formatRepeatDays(schedule),
                schedule.getReminderMinutes()
        );
    }

    private static String formatRepeatDays(Schedule schedule) {
        if (schedule.getRepeatRule() == null ||
                schedule.getRepeatRule().getDaysOfWeek() == null) {
            return null;
        }

        return schedule.getRepeatRule().getDaysOfWeek().stream()
                .map(day -> day.name().substring(0, 3))  // MONDAY → MON
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}
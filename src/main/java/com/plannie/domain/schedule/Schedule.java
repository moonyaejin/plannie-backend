package com.plannie.domain.schedule;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Schedule 도메인 모델
 * - JPA 의존성 없음 (순수 도메인 객체)
 * - 비즈니스 로직 포함
 */
@Getter
public class Schedule {

    private final Long id;
    private final Long userId;
    private String title;
    private String memo;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean completed;
    private RepeatRule repeatRule;
    private Long categoryId;
    private Integer reminderMinutes;  // null = 알림 없음, 5/10/30 = X분 전 알림

    @Builder
    public Schedule(Long id, Long userId, String title, String memo,
                    LocalDate startDate, LocalDate endDate,
                    LocalTime startTime, LocalTime endTime,
                    boolean completed, RepeatRule repeatRule, Long categoryId,
                    Integer reminderMinutes) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = completed;
        this.repeatRule = repeatRule;
        this.categoryId = categoryId;
        this.reminderMinutes = reminderMinutes;
    }

    public boolean conflictsWith(Schedule other) {
        if (!this.startDate.equals(other.startDate)) {
            return false;
        }

        return !(this.endTime.isBefore(other.startTime) || 
                 this.startTime.isAfter(other.endTime));
    }

    public void update(String title, String memo,
                       LocalDate startDate, LocalDate endDate,
                       LocalTime startTime, LocalTime endTime,
                       Long categoryId, Integer reminderMinutes) {
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.categoryId = categoryId;
        this.reminderMinutes = reminderMinutes;
    }

}

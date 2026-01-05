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
@Builder
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

    @Builder
    public Schedule(Long id, Long userId, String title, String memo,
                    LocalDate startDate, LocalDate endDate,
                    LocalTime startTime, LocalTime endTime,
                    boolean completed, RepeatRule repeatRule, Long categoryId) {
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
    }

    /**
     * 일정 시간이 다른 일정과 겹치는지 확인
     */
    public boolean conflictsWith(Schedule other) {
        if (!this.startDate.equals(other.startDate)) {
            return false;
        }

        return !(this.endTime.isBefore(other.startTime) || 
                 this.startTime.isAfter(other.endTime));
    }

    /**
     * 일정 완료 처리
     */
    public void complete() {
        this.completed = true;
    }

    /**
     * 일정 완료 취소
     */
    public void uncomplete() {
        this.completed = false;
    }

    /**
     * 일정 정보 수정
     */
    public void update(String title, String memo, 
                       LocalDate startDate, LocalDate endDate,
                       LocalTime startTime, LocalTime endTime,
                       Long categoryId) {
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.categoryId = categoryId;
    }

    /**
     * 반복 일정 설정
     */
    public void setRepeatRule(RepeatRule repeatRule) {
        this.repeatRule = repeatRule;
    }

    /**
     * 유효한 시간 범위인지 검증
     */
    public boolean isValidTimeRange() {
        return startTime.isBefore(endTime) || startTime.equals(endTime);
    }
}

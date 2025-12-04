package com.plannie.domain.schedule;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 반복 일정 규칙 Value Object
 */
@Getter
public class RepeatRule {

    private final RepeatType type;
    private final Set<DayOfWeek> daysOfWeek;  // 매주 반복 시 요일들
    private final Integer dayOfMonth;          // 매월 반복 시 일자
    private final LocalDate endDate;           // 반복 종료일

    private RepeatRule(RepeatType type, Set<DayOfWeek> daysOfWeek, 
                       Integer dayOfMonth, LocalDate endDate) {
        this.type = type;
        this.daysOfWeek = daysOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.endDate = endDate;
    }

    /**
     * 반복 없음
     */
    public static RepeatRule none() {
        return new RepeatRule(RepeatType.NONE, null, null, null);
    }

    /**
     * 매일 반복
     */
    public static RepeatRule daily(LocalDate endDate) {
        return new RepeatRule(RepeatType.DAILY, null, null, endDate);
    }

    /**
     * 매주 특정 요일 반복
     */
    public static RepeatRule weekly(Set<DayOfWeek> daysOfWeek, LocalDate endDate) {
        return new RepeatRule(RepeatType.WEEKLY, daysOfWeek, null, endDate);
    }

    /**
     * 매월 특정 일자 반복
     */
    public static RepeatRule monthly(int dayOfMonth, LocalDate endDate) {
        return new RepeatRule(RepeatType.MONTHLY, null, dayOfMonth, endDate);
    }

    /**
     * 특정 날짜가 반복 규칙에 해당하는지 확인
     */
    public boolean appliesTo(LocalDate date) {
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        return switch (type) {
            case NONE -> false;
            case DAILY -> true;
            case WEEKLY -> daysOfWeek != null && daysOfWeek.contains(date.getDayOfWeek());
            case MONTHLY -> dayOfMonth != null && date.getDayOfMonth() == dayOfMonth;
        };
    }

    public boolean isRepeating() {
        return type != RepeatType.NONE;
    }

    public enum RepeatType {
        NONE,
        DAILY,
        WEEKLY,
        MONTHLY
    }
}

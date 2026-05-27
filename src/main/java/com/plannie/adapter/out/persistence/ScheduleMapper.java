package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.ScheduleCompletionEntity;
import com.plannie.adapter.out.persistence.entity.ScheduleExceptionEntity;
import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleCompletion;
import com.plannie.domain.schedule.ScheduleException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schedule 도메인 ↔ JPA Entity 변환 Mapper
 * 
 * 왜 필요한가?
 * - 도메인 모델은 JPA에 의존하지 않아야 함 (순수 자바)
 * - JPA Entity는 DB 매핑에만 집중
 * - 이 Mapper가 두 세계를 연결해줌
 */
@Component
public class ScheduleMapper {

    /**
     * JPA Entity → 도메인 모델 변환
     * DB에서 조회한 데이터를 비즈니스 로직에서 사용할 수 있게 변환
     */
    public Schedule toDomain(ScheduleJpaEntity entity) {
        return Schedule.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .memo(entity.getMemo())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .completed(entity.isCompleted())
                .categoryId(entity.getCategoryId())
                .repeatRule(toRepeatRule(entity))
                .reminderMinutes(entity.getReminderMinutes())
                .build();
    }

    /**
     * 도메인 모델 → JPA Entity 변환
     * 비즈니스 로직 처리 후 DB에 저장할 때 사용
     */
    public ScheduleJpaEntity toEntity(Schedule schedule) {
        LocalTime startTime = schedule.getStartTime() != null ? schedule.getStartTime() : LocalTime.of(0, 0);
        LocalTime endTime = schedule.getEndTime() != null ? schedule.getEndTime() : startTime.plusHours(1);
        ScheduleJpaEntity entity = ScheduleJpaEntity.builder()
                .id(schedule.getId())
                .userId(schedule.getUserId())
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .startTime(startTime)
                .endTime(endTime)
                .completed(schedule.isCompleted())
                .categoryId(schedule.getCategoryId())
                .repeatType(toRepeatType(schedule.getRepeatRule()))
                .repeatDays(toRepeatDays(schedule.getRepeatRule()))
                .repeatDayOfMonth(getRepeatDayOfMonth(schedule.getRepeatRule()))
                .repeatEndDate(schedule.getRepeatRule() != null ?
                        schedule.getRepeatRule().getEndDate() : null)
                .reminderMinutes(schedule.getReminderMinutes())
                .build();

        return entity;
    }

    /**
     * JPA Entity의 반복 관련 필드들 → RepeatRule 도메인 객체로 변환
     */
    private RepeatRule toRepeatRule(ScheduleJpaEntity entity) {
        if (entity.getRepeatType() == null || 
            entity.getRepeatType() == ScheduleJpaEntity.RepeatType.NONE) {
            return RepeatRule.none();
        }

        return switch (entity.getRepeatType()) {
            case DAILY -> RepeatRule.daily(entity.getRepeatEndDate());
            case WEEKLY -> RepeatRule.weekly(
                    parseDaysOfWeek(entity.getRepeatDays()),
                    entity.getRepeatEndDate()
            );
            case MONTHLY -> RepeatRule.monthly(
                    entity.getRepeatDayOfMonth(),
                    entity.getRepeatEndDate()
            );
            default -> RepeatRule.none();
        };
    }

    /**
     * "MON,TUE,WED" 문자열 → Set<DayOfWeek> 변환
     */
    private Set<DayOfWeek> parseDaysOfWeek(String repeatDays) {
        if (repeatDays == null || repeatDays.isBlank()) {
            return Set.of();
        }
        
        return Arrays.stream(repeatDays.split(","))
                .map(String::trim)
                .map(this::toDayOfWeek)
                .collect(Collectors.toSet());
    }

    private DayOfWeek toDayOfWeek(String day) {
        return switch (day.toUpperCase()) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day: " + day);
        };
    }

    /**
     * RepeatRule → JPA Entity의 RepeatType으로 변환
     */
    private ScheduleJpaEntity.RepeatType toRepeatType(RepeatRule rule) {
        if (rule == null || !rule.isRepeating()) {
            return ScheduleJpaEntity.RepeatType.NONE;
        }

        return switch (rule.getType()) {
            case DAILY -> ScheduleJpaEntity.RepeatType.DAILY;
            case WEEKLY -> ScheduleJpaEntity.RepeatType.WEEKLY;
            case MONTHLY -> ScheduleJpaEntity.RepeatType.MONTHLY;
            default -> ScheduleJpaEntity.RepeatType.NONE;
        };
    }

    /**
     * RepeatRule의 요일들 → "MON,TUE,WED" 문자열로 변환
     */
    private String toRepeatDays(RepeatRule rule) {
        if (rule == null || rule.getDaysOfWeek() == null) {
            return null;
        }

        return rule.getDaysOfWeek().stream()
                .map(this::toShortDay)
                .collect(Collectors.joining(","));
    }

    public ScheduleException toDomain(ScheduleExceptionEntity entity) {
        if (entity == null) return null;

        return ScheduleException.builder()
                .id(entity.getId())
                .scheduleId(entity.getScheduleId())
                .exceptionDate(entity.getExceptionDate())
                .exceptionType(mapExceptionType(entity.getExceptionType()))
                .modifiedTitle(entity.getModifiedTitle())
                .modifiedMemo(entity.getModifiedMemo())
                .modifiedStartTime(entity.getModifiedStartTime())
                .modifiedEndTime(entity.getModifiedEndTime())
                .build();
    }

    private ScheduleException.ExceptionType mapExceptionType(ScheduleExceptionEntity.ExceptionType type) {
        return switch (type) {
            case DELETED -> ScheduleException.ExceptionType.DELETED;
            case MODIFIED -> ScheduleException.ExceptionType.MODIFIED;
        };
    }

    public ScheduleCompletion toDomain(ScheduleCompletionEntity entity) {
        if (entity == null) return null;

        return ScheduleCompletion.builder()
                .id(entity.getId())
                .scheduleId(entity.getScheduleId())
                .completionDate(entity.getCompletionDate())
                .completed(entity.isCompleted())
                .build();
    }

    private String toShortDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private Integer getRepeatDayOfMonth(RepeatRule rule) {
        return rule != null ? rule.getDayOfMonth() : null;
    }
}

package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity;
import com.plannie.adapter.out.persistence.repository.ScheduleJpaRepository;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SchedulePersistenceAdapter implements SaveSchedulePort, LoadSchedulePort {

    private final ScheduleJpaRepository scheduleJpaRepository;

    // ── SaveSchedulePort ──────────────────────────────────────────────────────

    @Override
    public Schedule save(Schedule schedule) {
        ScheduleJpaEntity entity = toEntity(schedule);
        ScheduleJpaEntity saved = scheduleJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(Long scheduleId) {
        scheduleJpaRepository.deleteById(scheduleId);
    }

    // ── LoadSchedulePort ──────────────────────────────────────────────────────

    @Override
    public Optional<Schedule> findById(Long id) {
        return scheduleJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Schedule> findByIdAndUserId(Long id, Long userId) {
        return scheduleJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Schedule> findByUserIdAndDate(Long userId, LocalDate date) {
        return scheduleJpaRepository.findByUserIdAndStartDate(userId, date)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Schedule> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return scheduleJpaRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Schedule> findConflictingSchedules(Long userId, LocalDate date,
                                                    LocalTime startTime, LocalTime endTime) {
        return scheduleJpaRepository.findConflictingSchedules(userId, date, startTime, endTime, 0L)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Schedule> findByIdWithLock(Long id) {
        return scheduleJpaRepository.findByIdWithLock(id).map(this::toDomain);
    }

    // ── 매핑 ──────────────────────────────────────────────────────────────────

    private ScheduleJpaEntity toEntity(Schedule schedule) {
        RepeatRule rule = schedule.getRepeatRule();
        return ScheduleJpaEntity.builder()
                .id(schedule.getId())
                .userId(schedule.getUserId())
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .startTime(schedule.getStartTime() != null ? schedule.getStartTime() : LocalTime.of(0, 0))
                .endTime(schedule.getEndTime() != null ? schedule.getEndTime() : LocalTime.of(23, 59))
                .completed(schedule.isCompleted())
                .categoryId(schedule.getCategoryId())
                .repeatType(toEntityRepeatType(rule))
                .repeatDays(toRepeatDaysString(rule))
                .repeatDayOfMonth(rule != null ? rule.getDayOfMonth() : null)
                .repeatEndDate(rule != null ? rule.getEndDate() : null)
                .build();
    }

    private Schedule toDomain(ScheduleJpaEntity entity) {
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
                .build();
    }

    private ScheduleJpaEntity.RepeatType toEntityRepeatType(RepeatRule rule) {
        if (rule == null) return ScheduleJpaEntity.RepeatType.NONE;
        return ScheduleJpaEntity.RepeatType.valueOf(rule.getType().name());
    }

    private String toRepeatDaysString(RepeatRule rule) {
        if (rule == null || rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isEmpty()) return null;
        return rule.getDaysOfWeek().stream()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }

    private RepeatRule toRepeatRule(ScheduleJpaEntity entity) {
        return switch (entity.getRepeatType()) {
            case NONE -> RepeatRule.none();
            case DAILY -> RepeatRule.daily(entity.getRepeatEndDate());
            case WEEKLY -> RepeatRule.weekly(parseRepeatDays(entity.getRepeatDays()), entity.getRepeatEndDate());
            case MONTHLY -> RepeatRule.monthly(entity.getRepeatDayOfMonth(), entity.getRepeatEndDate());
        };
    }

    private Set<DayOfWeek> parseRepeatDays(String repeatDays) {
        if (repeatDays == null || repeatDays.isBlank()) return Set.of();
        return Arrays.stream(repeatDays.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }
}

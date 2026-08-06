package com.plannie.application.service;

import com.plannie.application.port.in.ScheduleView;
import com.plannie.application.port.in.CreateScheduleUseCase;
import com.plannie.application.port.in.DeleteScheduleUseCase;
import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.in.UpdateScheduleUseCase;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.common.exception.ScheduleConflictException;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import com.plannie.domain.schedule.RepeatRule.RepeatType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService implements CreateScheduleUseCase, GetScheduleUseCase,
        UpdateScheduleUseCase, DeleteScheduleUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final SaveSchedulePort saveSchedulePort;

    // ==================== CreateScheduleUseCase 구현 ====================

    @Override
    @Transactional
    @CacheEvict(value = "schedules:monthly", key = "#command.userId() + ':' + #command.startDate().getYear() + ':' + #command.startDate().getMonthValue()")
    public Schedule createSchedule(CreateScheduleCommand command) {
        // 1. 시간 유효성 검증
        validateTimeRange(command.startTime(), command.endTime());

        // 2. 시간 충돌 검사
        checkScheduleConflict(
                command.userId(),
                command.startDate(),
                command.startTime(),
                command.endTime()
        );

        // 3. 도메인 객체 생성
        Schedule schedule = Schedule.builder()
                .userId(command.userId())
                .title(command.title())
                .memo(command.memo())
                .startDate(command.startDate())
                .endDate(command.endDate() != null ? command.endDate() : command.startDate())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .completed(false)
                .categoryId(command.categoryId())
                .reminderMinutes(command.reminderMinutes())
                .repeatRule(createRepeatRule(
                        command.repeatType(),
                        command.repeatDays(),
                        command.repeatEndDate(),
                        command.startDate()
                ))
                .build();

        // 4. DB 저장 및 반환
        return saveSchedulePort.save(schedule);
    }

    // ==================== GetScheduleUseCase 구현 ====================

    @Override
    public Optional<Schedule> getSchedule(Long scheduleId, Long userId) {
        return loadSchedulePort.findByIdAndUserId(scheduleId, userId);
    }

    @Override
    public List<ScheduleView> getSchedulesByDate(Long userId, LocalDate date) {
        return buildScheduleViews(userId, date, date);
    }

    @Override
    @Cacheable(value = "schedules:monthly", key = "#userId + ':' + #year + ':' + #month")
    public List<ScheduleView> getSchedulesByMonth(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return buildScheduleViews(userId, startDate, endDate);
    }

    @Override
    public List<ScheduleView> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return buildScheduleViews(userId, startDate, endDate);
    }

    private List<ScheduleView> buildScheduleViews(Long userId, LocalDate startDate, LocalDate endDate) {
        // 1. 일회성 일정 조회
        List<Schedule> oneTimeSchedules = loadSchedulePort.findOneTimeSchedulesByDateRange(
                userId, startDate, endDate
        );

        // 2. 반복 일정 조회
        List<Schedule> repeatingSchedules = loadSchedulePort.findRepeatingSchedules(userId);

        // 3. 예외사항과 완료 상태 조회
        List<Long> scheduleIds = repeatingSchedules.stream()
                .map(Schedule::getId)
                .toList();

        Map<String, ScheduleException> exceptions = scheduleIds.isEmpty()
                ? new HashMap<>()
                : loadSchedulePort.findExceptions(scheduleIds, startDate, endDate);

        Map<String, Boolean> completions = scheduleIds.isEmpty()
                ? new HashMap<>()
                : loadSchedulePort.findCompletions(scheduleIds, startDate, endDate);

        // 4. 일회성 일정을 View로 변환
        List<ScheduleView> views = new ArrayList<>();
        for (Schedule schedule : oneTimeSchedules) {
            views.add(ScheduleView.builder()
                    .id(schedule.getId())
                    .instanceId(schedule.getId().toString())
                    .title(schedule.getTitle())
                    .memo(schedule.getMemo())
                    .startDate(schedule.getStartDate())
                    .endDate(schedule.getEndDate())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .completed(schedule.isCompleted())
                    .categoryId(schedule.getCategoryId())
                    .isRecurring(false)
                    .repeatType("NONE")
                    .build());
        }

        // 5. 반복 일정 확장
        for (Schedule repeating : repeatingSchedules) {
            views.addAll(expandRepeatScheduleToViews(repeating, startDate, endDate, exceptions, completions));
        }

        // 6. 날짜순 정렬
        views.sort((a, b) -> {
            int dateCompare = a.getStartDate().compareTo(b.getStartDate());
            if (dateCompare != 0) return dateCompare;
            return a.getStartTime().compareTo(b.getStartTime());
        });

        return views;
    }

    // ==================== UpdateScheduleUseCase 구현 ====================

    @Override
    @Transactional
    @CacheEvict(value = "schedules:monthly", allEntries = true)
    public Schedule updateSchedule(UpdateScheduleCommand command) {
        // 1. 기존 일정 조회 + 권한 확인
        Schedule existingSchedule = loadSchedulePort
                .findByIdAndUserId(command.scheduleId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 2. 시간 유효성 검증
        validateTimeRange(command.startTime(), command.endTime());

        // 3. 반복 일정의 occurrence 하나만 수정하는 경우 — 시리즈 원본은 그대로 두고 예외만 기록
        if (command.occurrenceDate() != null && existingSchedule.getRepeatRule().isRepeating()) {
            saveSchedulePort.saveOccurrenceModification(
                    existingSchedule.getId(),
                    command.occurrenceDate(),
                    command.title(),
                    command.memo(),
                    command.startTime(),
                    command.endTime()
            );
            return existingSchedule;
        }

        // 4. 시리즈 전체(또는 반복 없는 일정) 수정
        existingSchedule.update(
                command.title(),
                command.memo(),
                command.startDate(),
                command.endDate() != null ? command.endDate() : command.startDate(),
                command.startTime(),
                command.endTime(),
                command.categoryId(),
                command.reminderMinutes()
        );

        return saveSchedulePort.save(existingSchedule);
    }

    @Override
    @Transactional
    @CacheEvict(value = "schedules:monthly", allEntries = true)
    public void toggleComplete(Long scheduleId, Long userId) {
        // 권한 확인
        Schedule schedule = loadSchedulePort
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 반복 일정은 날짜별 완료 처리(toggleRecurringComplete)를 써야 함 — 여기서 토글하면
        // 시리즈 전체가 한꺼번에 완료된 것처럼 보이는 버그로 이어짐
        if (schedule.getRepeatRule().isRepeating()) {
            throw new BusinessException(ErrorCode.RECURRING_SCHEDULE_TOGGLE_NOT_ALLOWED);
        }

        // 일회성 일정 완료 토글
        saveSchedulePort.toggleComplete(scheduleId);
    }

    // 반복 일정의 특정 날짜 완료 토글
    @Transactional
    @CacheEvict(value = "schedules:monthly", allEntries = true)
    public void toggleRecurringComplete(Long scheduleId, LocalDate date, Long userId) {
        // 권한 확인
        Schedule schedule = loadSchedulePort
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 반복 일정의 특정 날짜 완료 토글
        saveSchedulePort.toggleCompletion(scheduleId, date);
    }

    // ==================== DeleteScheduleUseCase 구현 ====================

    @Override
    @Transactional
    @CacheEvict(value = "schedules:monthly", allEntries = true)
    public void deleteSchedule(Long scheduleId, Long userId, LocalDate occurrenceDate) {
        Schedule schedule = loadSchedulePort
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 반복 일정의 occurrence 하나만 삭제하는 경우 — 시리즈는 남기고 해당 날짜만 예외 처리
        if (occurrenceDate != null && schedule.getRepeatRule().isRepeating()) {
            saveSchedulePort.deleteOccurrence(scheduleId, occurrenceDate);
            log.info("Deleted occurrence {} of schedule {} for user {}", occurrenceDate, scheduleId, userId);
            return;
        }

        saveSchedulePort.delete(schedule.getId());
        log.info("Deleted schedule {} for user {}", scheduleId, userId);
    }

    @Override
    @Transactional
    public void bulkDeleteSchedules(Long userId, Integer year, Integer month) {
        saveSchedulePort.bulkDelete(userId, year, month);
        log.info("Bulk deleted schedules for user {} year={} month={}", userId, year, month);
    }

    // ==================== Private Helper Methods ====================

    private List<ScheduleView> expandRepeatScheduleToViews(Schedule schedule,
                                                           LocalDate monthStart,
                                                           LocalDate monthEnd,
                                                           Map<String, ScheduleException> exceptions,
                                                           Map<String, Boolean> completions) {
        List<ScheduleView> views = new ArrayList<>();
        RepeatRule rule = schedule.getRepeatRule();

        LocalDate currentDate = schedule.getStartDate().isBefore(monthStart)
                ? monthStart : schedule.getStartDate();

        LocalDate repeatEndDate = rule.getEndDate() != null && rule.getEndDate().isBefore(monthEnd)
                ? rule.getEndDate() : monthEnd;

        // MONTHLY 타입일 때 특별 처리
        if (rule.getType() == RepeatType.MONTHLY) {
            Integer targetDayOfMonth = rule.getDayOfMonth();
            LocalDate current = currentDate.withDayOfMonth(1); // 월 초부터 시작

            while (!current.isAfter(repeatEndDate)) {
                // 해당 월의 실제 날짜 계산 (월말 조정)
                int lastDayOfMonth = current.lengthOfMonth();
                int actualDay = Math.min(targetDayOfMonth, lastDayOfMonth);
                LocalDate targetDate = current.withDayOfMonth(actualDay);

                // 범위 체크
                if (!targetDate.isBefore(monthStart) &&
                        !targetDate.isAfter(monthEnd) &&
                        !targetDate.isBefore(schedule.getStartDate()) &&
                        !targetDate.isAfter(repeatEndDate)) {

                    String key = schedule.getId() + "_" + targetDate;

                    // 삭제된 날짜는 건너뛰기
                    ScheduleException exception = exceptions.get(key);
                    if (exception != null && exception.isDeleted()) {
                        current = current.plusMonths(1);
                        continue;
                    }

                    // View 생성
                    ScheduleView view = createScheduleView(
                            schedule, targetDate, key, exception, completions, rule
                    );
                    views.add(view);
                }

                current = current.plusMonths(1);
            }
        } else {
            // DAILY, WEEKLY는 기존 로직 유지
            while (!currentDate.isAfter(repeatEndDate)) {
                if (rule.appliesTo(currentDate)) {
                    String key = schedule.getId() + "_" + currentDate;

                    // 삭제된 날짜는 건너뛰기
                    ScheduleException exception = exceptions.get(key);
                    if (exception != null && exception.isDeleted()) {
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }

                    // View 생성
                    ScheduleView view = createScheduleView(
                            schedule, currentDate, key, exception, completions, rule
                    );
                    views.add(view);
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        return views;
    }

    // View 생성 로직을 별도 메서드로 분리 (중복 제거)
    private ScheduleView createScheduleView(Schedule schedule,
                                            LocalDate date,
                                            String key,
                                            ScheduleException exception,
                                            Map<String, Boolean> completions,
                                            RepeatRule rule) {
        return ScheduleView.builder()
                .id(schedule.getId())
                .instanceId(key)
                .title(exception != null && exception.getModifiedTitle() != null
                        ? exception.getModifiedTitle() : schedule.getTitle())
                .memo(exception != null && exception.getModifiedMemo() != null
                        ? exception.getModifiedMemo() : schedule.getMemo())
                .startDate(date)
                .endDate(date)
                .startTime(exception != null && exception.getModifiedStartTime() != null
                        ? exception.getModifiedStartTime() : schedule.getStartTime())
                .endTime(exception != null && exception.getModifiedEndTime() != null
                        ? exception.getModifiedEndTime() : schedule.getEndTime())
                .completed(completions.getOrDefault(key, false))
                .categoryId(schedule.getCategoryId())
                .isRecurring(true)
                .repeatType(rule.getType().name())
                .repeatDays(rule.getDaysOfWeek() != null
                        ? rule.getDaysOfWeek().stream()
                        .map(day -> day.name().substring(0, 3))
                        .collect(Collectors.joining(","))
                        : null)
                .build();
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    private void checkScheduleConflict(Long userId, LocalDate date,
                                       LocalTime startTime, LocalTime endTime) {
        List<Schedule> conflictingSchedules = loadSchedulePort
                .findConflictingSchedules(userId, date, startTime, endTime);

        if (!conflictingSchedules.isEmpty()) {
            throw new ScheduleConflictException(conflictingSchedules);
        }
    }

    private RepeatRule createRepeatRule(String repeatType, String repeatDays,
                                        LocalDate repeatEndDate, LocalDate startDate) {  // startDate 파라미터 추가
        if (repeatType == null || repeatType.equalsIgnoreCase("NONE")) {
            return RepeatRule.none();
        }

        return switch (repeatType.toUpperCase()) {
            case "DAILY" -> RepeatRule.daily(repeatEndDate);
            case "WEEKLY" -> RepeatRule.weekly(parseDaysOfWeek(repeatDays), repeatEndDate);
            case "MONTHLY" -> RepeatRule.monthly(startDate.getDayOfMonth(), repeatEndDate);  // startDate 사용!
            default -> RepeatRule.none();
        };
    }

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
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }
}
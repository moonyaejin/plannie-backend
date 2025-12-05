package com.plannie.application.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schedule Service
 *
 * 역할:
 * - CreateScheduleUseCase, GetScheduleUseCase, UpdateScheduleUseCase, DeleteScheduleUseCase 구현
 * - 비즈니스 로직 처리 (유효성 검증, 충돌 감지 등)
 * - 트랜잭션 관리
 *
 * 의존성:
 * - Port 인터페이스만 의존 (LoadSchedulePort, SaveSchedulePort)
 * - JPA Repository를 직접 사용하지 않음!
 * - 이렇게 하면 테스트할 때 Port를 Mock으로 쉽게 대체 가능
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 (조회 성능 최적화)
public class ScheduleService implements CreateScheduleUseCase, GetScheduleUseCase,
        UpdateScheduleUseCase, DeleteScheduleUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final SaveSchedulePort saveSchedulePort;

    // ==================== CreateScheduleUseCase 구현 ====================

    /**
     * 일정 생성
     *
     * 처리 순서:
     * 1. 시간 유효성 검증 (시작 시간 < 종료 시간)
     * 2. 시간 충돌 검사 (같은 시간대에 이미 일정이 있는지)
     * 3. 도메인 객체 생성
     * 4. DB 저장
     */
    @Override
    @Transactional  // 쓰기 작업이므로 readOnly = false
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
                .repeatRule(createRepeatRule(command.repeatType(), command.repeatDays()))
                .build();

        // 4. DB 저장 및 반환
        return saveSchedulePort.save(schedule);
    }

    // ==================== GetScheduleUseCase 구현 ====================

    /**
     * 일정 단건 조회
     * - userId를 함께 검증해서 다른 사용자의 일정은 조회 불가
     */
    @Override
    public Optional<Schedule> getSchedule(Long scheduleId, Long userId) {
        return loadSchedulePort.findByIdAndUserId(scheduleId, userId);
    }

    /**
     * 특정 날짜의 일정 목록 조회
     */
    @Override
    public List<Schedule> getSchedulesByDate(Long userId, LocalDate date) {
        return loadSchedulePort.findByUserIdAndDate(userId, date);
    }

    /**
     * 월별 일정 목록 조회
     * - 해당 월의 1일부터 말일까지 조회
     */
    @Override
    public List<Schedule> getSchedulesByMonth(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 기간별 일정 목록 조회
     */
    @Override
    public List<Schedule> getSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return loadSchedulePort.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    // ==================== UpdateScheduleUseCase 구현 ====================

    /**
     * 일정 수정
     *
     * 처리 순서:
     * 1. 기존 일정 조회 (본인 일정인지 확인)
     * 2. 시간 유효성 검증
     * 3. 도메인 객체 업데이트
     * 4. DB 저장
     */
    @Override
    @Transactional
    public Schedule updateSchedule(UpdateScheduleCommand command) {
        // 1. 기존 일정 조회 + 권한 확인
        Schedule existingSchedule = loadSchedulePort
                .findByIdAndUserId(command.scheduleId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 2. 시간 유효성 검증
        validateTimeRange(command.startTime(), command.endTime());

        // 3. 도메인 객체 업데이트 (도메인 메서드 사용)
        existingSchedule.update(
                command.title(),
                command.memo(),
                command.startDate(),
                command.endDate() != null ? command.endDate() : command.startDate(),
                command.startTime(),
                command.endTime(),
                command.categoryId()
        );

        // 4. DB 저장
        return saveSchedulePort.save(existingSchedule);
    }

    /**
     * 일정 완료 토글
     * - 체크박스 ON/OFF 기능
     * - 기존 Express API: PUT /planner/{id} with { check_box: true/false }
     */
    @Override
    @Transactional
    public void toggleComplete(Long scheduleId, Long userId) {
        Schedule schedule = loadSchedulePort
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 도메인 메서드로 상태 변경
        if (schedule.isCompleted()) {
            schedule.uncomplete();
        } else {
            schedule.complete();
        }

        saveSchedulePort.save(schedule);
    }

    // ==================== DeleteScheduleUseCase 구현 ====================

    /**
     * 일정 삭제
     * - 본인 일정만 삭제 가능
     */
    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        // 존재 여부 + 권한 확인
        Schedule schedule = loadSchedulePort
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        saveSchedulePort.delete(schedule.getId());
    }

    // ==================== 비즈니스 로직 (Private Methods) ====================

    /**
     * 시간 유효성 검증
     * - 시작 시간이 종료 시간보다 늦으면 안 됨
     */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    /**
     * 일정 충돌 검사
     * - 같은 날짜, 겹치는 시간대에 이미 일정이 있으면 예외 발생
     * - 충돌하는 일정 목록을 예외에 담아서 클라이언트에게 알려줌
     */
    private void checkScheduleConflict(Long userId, LocalDate date,
                                       LocalTime startTime, LocalTime endTime) {
        List<Schedule> conflictingSchedules = loadSchedulePort
                .findConflictingSchedules(userId, date, startTime, endTime);

        if (!conflictingSchedules.isEmpty()) {
            // 충돌하는 일정 정보를 담아서 예외 발생
            // → 클라이언트에서 "이 시간에 OOO 일정이 있습니다" 표시 가능
            throw new ScheduleConflictException(conflictingSchedules);
        }
    }

    /**
     * 반복 규칙 생성
     * - repeatType: "NONE", "DAILY", "WEEKLY", "MONTHLY"
     * - repeatDays: "MON,TUE,WED" (WEEKLY일 때만 사용)
     */
    private RepeatRule createRepeatRule(String repeatType, String repeatDays) {
        if (repeatType == null || repeatType.equalsIgnoreCase("NONE")) {
            return RepeatRule.none();
        }

        return switch (repeatType.toUpperCase()) {
            case "DAILY" -> RepeatRule.daily(null);  // 종료일 미지정
            case "WEEKLY" -> RepeatRule.weekly(parseDaysOfWeek(repeatDays), null);
            case "MONTHLY" -> RepeatRule.monthly(LocalDate.now().getDayOfMonth(), null);
            default -> RepeatRule.none();
        };
    }

    /**
     * "MON,TUE,WED" → Set<DayOfWeek> 변환
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
}
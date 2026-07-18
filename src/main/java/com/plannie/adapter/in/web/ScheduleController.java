package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.ParseScheduleRequest;
import com.plannie.adapter.in.web.dto.ParseScheduleResponse;
import com.plannie.adapter.in.web.dto.ScheduleRequest;
import com.plannie.adapter.in.web.dto.ScheduleResponse;
import com.plannie.application.port.in.ScheduleView;
import com.plannie.application.port.in.CreateScheduleUseCase;
import com.plannie.application.port.in.CreateScheduleUseCase.CreateScheduleCommand;
import com.plannie.application.port.in.DeleteScheduleUseCase;
import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.in.ParseScheduleUseCase;
import com.plannie.application.port.in.UpdateScheduleUseCase;
import com.plannie.application.port.in.UpdateScheduleUseCase.UpdateScheduleCommand;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Schedule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Schedule REST Controller
 *
 * 역할:
 * - HTTP 요청 수신 및 응답
 * - 요청 데이터 검증 (@Valid)
 * - DTO ↔ Command 변환
 * - UseCase 호출
 *
 * 의존성:
 * - UseCase 인터페이스만 의존 (Service 구현체 직접 의존 X)
 * - 이렇게 하면 Controller 테스트할 때 UseCase Mock 주입 가능
 *
 * REST API 설계 원칙:
 * - GET: 조회 (멱등성 O)
 * - POST: 생성 (멱등성 X)
 * - PUT: 수정 (멱등성 O)
 * - DELETE: 삭제 (멱등성 O)
 */
@Tag(name = "Schedule", description = "일정 관리 API")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final CreateScheduleUseCase createScheduleUseCase;
    private final GetScheduleUseCase getScheduleUseCase;
    private final UpdateScheduleUseCase updateScheduleUseCase;
    private final DeleteScheduleUseCase deleteScheduleUseCase;
    private final ParseScheduleUseCase parseScheduleUseCase;


    // ── AI 자연어 파싱 ────────────────────────────────────────────────────────

    @Operation(summary = "자연어 일정 생성", description = "자연어 입력으로 AI가 일정을 파싱하여 생성합니다")
    @PostMapping("/parse")
    @ResponseStatus(HttpStatus.CREATED)
    public ParseScheduleResponse parseAndCreate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ParseScheduleRequest request) {
        Schedule schedule = parseScheduleUseCase.parseAndCreate(
                new ParseScheduleUseCase.ParseScheduleCommand(userId, request.text())
        );
        return ParseScheduleResponse.from(schedule);
    }

    // ── 일정 생성 ─────────────────────────────────────────────────────────────

    @Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다")
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScheduleRequest request) {

        CreateScheduleCommand command = new CreateScheduleCommand(
                userId,
                request.title(),
                request.memo(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.categoryId(),
                request.repeatType(),
                request.repeatDays(),
                request.repeatEndDate(),
                request.reminderMinutes()
        );

        Schedule created = createScheduleUseCase.createSchedule(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(created));
    }

    // ── 일정 조회 ─────────────────────────────────────────────────────────────

    @Operation(summary = "일정 단건 조회", description = "ID로 일정을 조회합니다")
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        Schedule schedule = getScheduleUseCase.getSchedule(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @Operation(summary = "날짜별 일정 조회", description = "특정 날짜의 일정 목록을 조회합니다")
    @GetMapping("/date")
    public ResponseEntity<List<ScheduleView>> getSchedulesByDate(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)")
            @RequestParam LocalDate date) {

        List<ScheduleView> schedules = getScheduleUseCase.getSchedulesByDate(userId, date);
        return ResponseEntity.ok(schedules);
    }

    @Operation(summary = "월별 일정 조회", description = "특정 월의 일정 목록을 조회합니다")
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<List<ScheduleView>> getSchedulesByMonth(
            @AuthenticationPrincipal Long userId,
            @PathVariable int year,
            @PathVariable int month) {

        List<ScheduleView> schedules = getScheduleUseCase.getSchedulesByMonth(userId, year, month);
        return ResponseEntity.ok(schedules);
    }

    @Operation(summary = "기간별 일정 조회", description = "특정 기간의 일정 목록을 조회합니다")
    @GetMapping("/range")
    public ResponseEntity<List<ScheduleView>> getSchedulesByDateRange(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "시작 날짜") @RequestParam LocalDate startDate,
            @Parameter(description = "종료 날짜") @RequestParam LocalDate endDate) {

        List<ScheduleView> schedules = getScheduleUseCase.getSchedulesByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    // ── 일정 수정 ─────────────────────────────────────────────────────────────

    @Operation(summary = "일정 수정", description = "일정을 수정합니다")
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request) {

        UpdateScheduleCommand command = new UpdateScheduleCommand(
                id,
                userId,
                request.title(),
                request.memo(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.categoryId(),
                request.reminderMinutes()
        );

        Schedule updated = updateScheduleUseCase.updateSchedule(command);
        return ResponseEntity.ok(ScheduleResponse.from(updated));
    }

    @Operation(summary = "일정 완료 토글", description = "일정의 완료 상태를 토글합니다")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleComplete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        updateScheduleUseCase.toggleComplete(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "반복 일정 개별 완료", description = "반복 일정 중 하나를 개별 완료합니다")
    @PatchMapping("/{id}/complete/{date}")
    public ResponseEntity<Void> toggleRecurringComplete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        updateScheduleUseCase.toggleRecurringComplete(id, date, userId);
        return ResponseEntity.ok().build();
    }

    // ── 일정 삭제 ─────────────────────────────────────────────────────────────

    @Operation(summary = "일정 삭제", description = "일정을 삭제합니다")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        deleteScheduleUseCase.deleteSchedule(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "일정 일괄 삭제", description = "year+month 지정 시 해당 월, 미지정 시 전체 삭제")
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteSchedules(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        deleteScheduleUseCase.bulkDeleteSchedules(userId, year, month);
        return ResponseEntity.noContent().build();
    }
}

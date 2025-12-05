package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.ScheduleRequest;
import com.plannie.adapter.in.web.dto.ScheduleResponse;
import com.plannie.application.port.in.CreateScheduleUseCase;
import com.plannie.application.port.in.CreateScheduleUseCase.CreateScheduleCommand;
import com.plannie.application.port.in.DeleteScheduleUseCase;
import com.plannie.application.port.in.GetScheduleUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // TODO: JWT에서 userId 추출하도록 수정 필요
    // 지금은 임시로 헤더에서 받음
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 일정 생성
     * POST /api/schedules
     *
     * @param request 일정 생성 요청 DTO
     * @return 생성된 일정 정보
     */
    @Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다")
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody ScheduleRequest request) {

        // 1. DTO → Command 변환
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
                request.repeatDays()
        );

        // 2. UseCase 호출
        Schedule created = createScheduleUseCase.createSchedule(command);

        // 3. Domain → DTO 변환 후 응답
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(created));
    }

    /**
     * 일정 단건 조회
     * GET /api/schedules/{id}
     */
    @Operation(summary = "일정 단건 조회", description = "ID로 일정을 조회합니다")
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        Schedule schedule = getScheduleUseCase.getSchedule(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    /**
     * 특정 날짜의 일정 목록 조회
     * GET /api/schedules/date?date=2024-01-15
     *
     * 기존 Express API: GET /planner/date/?date=2024.01.15
     */
    @Operation(summary = "날짜별 일정 조회", description = "특정 날짜의 일정 목록을 조회합니다")
    @GetMapping("/date")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByDate(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)")
            @RequestParam LocalDate date) {

        List<Schedule> schedules = getScheduleUseCase.getSchedulesByDate(userId, date);

        List<ScheduleResponse> response = schedules.stream()
                .map(ScheduleResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 월별 일정 목록 조회
     * GET /api/schedules/monthly?year=2024&month=1
     *
     * 기존 Express API: GET /planner/monthly?year=2024&month=6
     */
    @Operation(summary = "월별 일정 조회", description = "특정 월의 일정 목록을 조회합니다")
    @GetMapping("/monthly")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByMonth(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "연도") @RequestParam int year,
            @Parameter(description = "월 (1-12)") @RequestParam int month) {

        List<Schedule> schedules = getScheduleUseCase.getSchedulesByMonth(userId, year, month);

        List<ScheduleResponse> response = schedules.stream()
                .map(ScheduleResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 기간별 일정 목록 조회
     * GET /api/schedules/range?startDate=2024-01-01&endDate=2024-01-31
     */
    @Operation(summary = "기간별 일정 조회", description = "특정 기간의 일정 목록을 조회합니다")
    @GetMapping("/range")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByDateRange(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "시작 날짜") @RequestParam LocalDate startDate,
            @Parameter(description = "종료 날짜") @RequestParam LocalDate endDate) {

        List<Schedule> schedules = getScheduleUseCase
                .getSchedulesByDateRange(userId, startDate, endDate);

        List<ScheduleResponse> response = schedules.stream()
                .map(ScheduleResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 일정 수정
     * PUT /api/schedules/{id}
     *
     * 기존 Express API: PUT /planner/{id}
     */
    @Operation(summary = "일정 수정", description = "일정을 수정합니다")
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @RequestHeader(USER_ID_HEADER) Long userId,
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
                request.categoryId()
        );

        Schedule updated = updateScheduleUseCase.updateSchedule(command);

        return ResponseEntity.ok(ScheduleResponse.from(updated));
    }

    /**
     * 일정 완료 토글
     * PATCH /api/schedules/{id}/toggle
     *
     * 기존 Express API: PUT /planner/{id} with { check_box: true/false }
     * → PATCH로 변경 (리소스 부분 수정)
     */
    @Operation(summary = "일정 완료 토글", description = "일정의 완료 상태를 토글합니다")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleComplete(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        updateScheduleUseCase.toggleComplete(id, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * 일정 삭제
     * DELETE /api/schedules/{id}
     */
    @Operation(summary = "일정 삭제", description = "일정을 삭제합니다")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Parameter(description = "일정 ID") @PathVariable Long id) {

        deleteScheduleUseCase.deleteSchedule(id, userId);

        return ResponseEntity.noContent().build();  // 204 No Content
    }
}
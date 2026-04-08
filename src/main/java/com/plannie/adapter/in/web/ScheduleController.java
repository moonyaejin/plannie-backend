package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.ParseScheduleRequest;
import com.plannie.adapter.in.web.dto.ParseScheduleResponse;
import com.plannie.application.port.in.GetScheduleUseCase;
import com.plannie.application.port.in.ParseScheduleUseCase;
import com.plannie.domain.schedule.Schedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ParseScheduleUseCase parseScheduleUseCase;
    private final GetScheduleUseCase getScheduleUseCase;

    // ── 생성 ──────────────────────────────────────────────────────────────────

    @PostMapping("/parse")
    @ResponseStatus(HttpStatus.CREATED)
    public ParseScheduleResponse parseAndCreate(@Valid @RequestBody ParseScheduleRequest request) {
        Schedule schedule = parseScheduleUseCase.parseAndCreate(
                new ParseScheduleUseCase.ParseScheduleCommand(request.userId(), request.text())
        );
        return ParseScheduleResponse.from(schedule);
    }

    // ── 조회 ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ParseScheduleResponse getSchedule(
            @PathVariable Long id,
            @RequestParam("user_id") @NotNull Long userId) {
        return getScheduleUseCase.getSchedule(id, userId)
                .map(ParseScheduleResponse::from)
                .orElseThrow();
    }

    @GetMapping
    public List<ParseScheduleResponse> getSchedules(
            @RequestParam("user_id") @NotNull Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        if (date != null) {
            return getScheduleUseCase.getSchedulesByDate(userId, date)
                    .stream().map(ParseScheduleResponse::from).toList();
        }
        if (year != null && month != null) {
            return getScheduleUseCase.getSchedulesByMonth(userId, year, month)
                    .stream().map(ParseScheduleResponse::from).toList();
        }
        // 기본값: 오늘 날짜
        return getScheduleUseCase.getSchedulesByDate(userId, LocalDate.now())
                .stream().map(ParseScheduleResponse::from).toList();
    }
}

package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.StudySessionRequest;
import com.plannie.adapter.in.web.dto.StudySessionResponse;
import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.in.StudySessionUseCase.SubjectSummary;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "StudySession", description = "공부 타이머 API")
@RestController
@RequestMapping("/api/study-sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionUseCase studySessionUseCase;

    @Operation(summary = "타이머 시작")
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse start(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudySessionRequest request) {

        return StudySessionResponse.from(
                studySessionUseCase.start(new StudySessionUseCase.StartCommand(userId, request.subjectId()))
        );
    }

    @Operation(summary = "타이머 종료")
    @PostMapping("/{id}/stop")
    public StudySessionResponse stop(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        return StudySessionResponse.from(studySessionUseCase.stop(id, userId));
    }

    @Operation(summary = "진행 중인 세션 조회")
    @GetMapping("/active")
    public ResponseEntity<StudySessionResponse> getActive(@AuthenticationPrincipal Long userId) {
        return studySessionUseCase.getActive(userId)
                .map(s -> ResponseEntity.ok(StudySessionResponse.from(s)))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "날짜별 세션 목록")
    @GetMapping("/daily")
    public List<StudySessionResponse> getByDate(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return studySessionUseCase.getByDate(userId, date)
                .stream().map(StudySessionResponse::from).toList();
    }

    @Operation(summary = "기간별 과목별 요약")
    @GetMapping("/summary")
    public List<SubjectSummary> getSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return studySessionUseCase.getSummary(userId, startDate, endDate);
    }
}

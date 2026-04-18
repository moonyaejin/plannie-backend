package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.WeeklyReportRequest;
import com.plannie.adapter.in.web.dto.WeeklyReportResponse;
import com.plannie.application.port.in.GenerateWeeklyReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WeeklyReport", description = "주간 리포트 API")
@RestController
@RequestMapping("/api/weekly-report")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final GenerateWeeklyReportUseCase generateWeeklyReportUseCase;

    @Operation(summary = "주간 리포트 생성", description = "해당 주의 일정 완료율과 공부 시간을 AI가 분석하여 주간 리포트를 생성합니다.")
    @PostMapping("/generate")
    public WeeklyReportResponse generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WeeklyReportRequest request) {

        return WeeklyReportResponse.from(
                generateWeeklyReportUseCase.generate(userId, request.weekStart(), request.weekEnd())
        );
    }
}

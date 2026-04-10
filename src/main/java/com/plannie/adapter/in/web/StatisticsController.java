package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.MonthlyStatsResponse;
import com.plannie.adapter.in.web.dto.WeeklyFeedbackRequest;
import com.plannie.adapter.in.web.dto.WeeklyFeedbackResponse;
import com.plannie.application.port.in.GenerateProgressFeedbackUseCase;
import com.plannie.application.port.in.GetStatisticsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Statistics", description = "통계 및 진도 분석 API")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final GetStatisticsUseCase getStatisticsUseCase;
    private final GenerateProgressFeedbackUseCase generateProgressFeedbackUseCase;

    @Operation(summary = "월별 통계 조회",
               description = "해당 월의 일정 완료율 및 카테고리별 통계를 반환합니다. Redis에 1시간 캐싱됩니다.")
    @GetMapping("/monthly")
    public MonthlyStatsResponse getMonthlyStats(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam int year,
            @RequestParam int month) {

        return MonthlyStatsResponse.from(getStatisticsUseCase.getMonthlyStats(userId, year, month));
    }

    @Operation(summary = "주간 진도 AI 피드백",
               description = "지정한 기간의 일정 데이터를 분석하여 AI가 주간 리포트를 생성합니다.")
    @PostMapping("/weekly-feedback")
    public WeeklyFeedbackResponse getWeeklyFeedback(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody WeeklyFeedbackRequest request) {

        return WeeklyFeedbackResponse.from(
                generateProgressFeedbackUseCase.generateWeeklyFeedback(
                        userId, request.weekStart(), request.weekEnd()
                )
        );
    }
}

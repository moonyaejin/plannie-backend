package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.GenerateProgressFeedbackUseCase.WeeklyFeedback;

import java.time.LocalDate;
import java.util.List;

public record WeeklyFeedbackResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalSchedules,
        int completedSchedules,
        double completionRate,
        String summary,
        List<String> strengths,
        List<String> improvements,
        String nextWeekAdvice
) {
    public static WeeklyFeedbackResponse from(WeeklyFeedback feedback) {
        return new WeeklyFeedbackResponse(
                feedback.weekStart(), feedback.weekEnd(),
                feedback.totalSchedules(), feedback.completedSchedules(),
                feedback.completionRate(), feedback.summary(),
                feedback.strengths(), feedback.improvements(),
                feedback.nextWeekAdvice()
        );
    }
}

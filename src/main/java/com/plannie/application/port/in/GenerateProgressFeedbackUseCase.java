package com.plannie.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GenerateProgressFeedbackUseCase {

    WeeklyFeedback generateWeeklyFeedback(Long userId, LocalDate weekStart, LocalDate weekEnd);

    record WeeklyFeedback(
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalSchedules,
            int completedSchedules,
            double completionRate,
            String summary,
            List<String> strengths,
            List<String> improvements,
            String nextWeekAdvice
    ) {}
}

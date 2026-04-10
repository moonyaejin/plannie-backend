package com.plannie.application.port.out;

import java.time.LocalDate;
import java.util.List;

public interface GenerateProgressFeedbackWithAiPort {

    AiFeedback generate(FeedbackRequest request);

    record FeedbackRequest(
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalSchedules,
            int completedSchedules,
            double completionRate,
            List<String> scheduleTitles
    ) {}

    record AiFeedback(
            String summary,
            List<String> strengths,
            List<String> improvements,
            String nextWeekAdvice
    ) {}
}

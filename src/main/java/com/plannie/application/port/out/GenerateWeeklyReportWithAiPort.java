package com.plannie.application.port.out;

import java.time.LocalDate;
import java.util.List;

public interface GenerateWeeklyReportWithAiPort {

    AiWeeklyReport generate(ReportRequest request);

    record ReportRequest(
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalSchedules,
            int completedSchedules,
            double completionRate,
            List<String> scheduleTitles,
            int totalStudyMinutes,
            List<CategoryTime> studyByCategory
    ) {}

    record CategoryTime(String categoryName, int totalMinutes) {}

    record AiWeeklyReport(
            String summary,
            List<String> strengths,
            List<String> improvements,
            String nextWeekAdvice
    ) {}
}

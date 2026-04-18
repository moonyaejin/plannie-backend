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
            List<SubjectTime> studyBySubject
    ) {}

    record SubjectTime(String subjectName, int totalMinutes) {}

    record AiWeeklyReport(
            String summary,
            List<String> strengths,
            List<String> improvements,
            String nextWeekAdvice
    ) {}
}

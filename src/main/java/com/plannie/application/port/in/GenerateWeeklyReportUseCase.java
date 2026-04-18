package com.plannie.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GenerateWeeklyReportUseCase {

    WeeklyReport generate(Long userId, LocalDate weekStart, LocalDate weekEnd);

    record WeeklyReport(
            LocalDate weekStart,
            LocalDate weekEnd,
            // 일정
            int totalSchedules,
            int completedSchedules,
            double completionRate,
            // 공부 시간
            int totalStudyMinutes,
            List<SubjectTime> studyBySubject,
            // AI 분석
            String summary,
            List<String> strengths,
            List<String> improvements,
            String nextWeekAdvice
    ) {}

    record SubjectTime(String subjectName, int totalMinutes) {}
}

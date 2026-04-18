package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.GenerateWeeklyReportUseCase.WeeklyReport;

import java.time.LocalDate;
import java.util.List;

public record WeeklyReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        // 일정
        int totalSchedules,
        int completedSchedules,
        double completionRate,
        // 공부 시간
        int totalStudyMinutes,
        List<SubjectTimeDto> studyBySubject,
        // AI 분석
        String summary,
        List<String> strengths,
        List<String> improvements,
        String nextWeekAdvice
) {
    public record SubjectTimeDto(String subjectName, int totalMinutes) {}

    public static WeeklyReportResponse from(WeeklyReport report) {
        return new WeeklyReportResponse(
                report.weekStart(), report.weekEnd(),
                report.totalSchedules(), report.completedSchedules(), report.completionRate(),
                report.totalStudyMinutes(),
                report.studyBySubject().stream()
                        .map(s -> new SubjectTimeDto(s.subjectName(), s.totalMinutes()))
                        .toList(),
                report.summary(), report.strengths(),
                report.improvements(), report.nextWeekAdvice()
        );
    }
}

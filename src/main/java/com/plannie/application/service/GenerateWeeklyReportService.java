package com.plannie.application.service;

import com.plannie.application.port.in.GenerateWeeklyReportUseCase;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.studysession.StudySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenerateWeeklyReportService implements GenerateWeeklyReportUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final StudySessionPort studySessionPort;
    private final LoadCategoryPort loadCategoryPort;
    private final GenerateWeeklyReportWithAiPort reportAiPort;

    @Override
    @Transactional(readOnly = true)
    public WeeklyReport generate(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        // 1. 일정 데이터 집계
        List<Schedule> schedules = loadSchedulePort.findByUserIdAndDateRange(userId, weekStart, weekEnd);
        int total = schedules.size();
        int completed = (int) schedules.stream().filter(Schedule::isCompleted).count();
        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;
        List<String> scheduleTitles = schedules.stream()
                .map(s -> s.getTitle() + (s.isCompleted() ? " [완료]" : " [미완료]"))
                .toList();

        // 2. 공부 시간 집계
        List<StudySession> sessions = studySessionPort.findByUserIdAndDateRange(userId, weekStart, weekEnd);
        Map<Long, Category> categoryMap = loadCategoryPort.findAllByUserIdOrDefault(userId).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, Integer> minutesByCategory = sessions.stream()
                .filter(s -> !s.isActive() && s.getDurationMinutes() != null)
                .collect(Collectors.groupingBy(
                        StudySession::getCategoryId,
                        Collectors.summingInt(StudySession::getDurationMinutes)
                ));

        List<CategoryTime> studyByCategory = minutesByCategory.entrySet().stream()
                .map(e -> {
                    Category category = categoryMap.get(e.getKey());
                    String name = category != null ? category.getName() : "삭제된 카테고리";
                    return new CategoryTime(name, e.getValue());
                })
                .sorted((a, b) -> b.totalMinutes() - a.totalMinutes())
                .toList();

        int totalStudyMinutes = minutesByCategory.values().stream().mapToInt(Integer::intValue).sum();

        // 3. AI 분석
        GenerateWeeklyReportWithAiPort.AiWeeklyReport aiReport = reportAiPort.generate(
                new GenerateWeeklyReportWithAiPort.ReportRequest(
                        weekStart, weekEnd,
                        total, completed, completionRate, scheduleTitles,
                        totalStudyMinutes,
                        studyByCategory.stream()
                                .map(s -> new GenerateWeeklyReportWithAiPort.CategoryTime(s.categoryName(), s.totalMinutes()))
                                .toList()
                )
        );

        return new WeeklyReport(
                weekStart, weekEnd,
                total, completed, completionRate,
                totalStudyMinutes, studyByCategory,
                aiReport.summary(), aiReport.strengths(),
                aiReport.improvements(), aiReport.nextWeekAdvice()
        );
    }
}

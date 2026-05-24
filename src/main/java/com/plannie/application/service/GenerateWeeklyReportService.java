package com.plannie.application.service;

import com.plannie.application.port.in.GenerateWeeklyReportUseCase;
import com.plannie.application.port.out.GenerateWeeklyReportWithAiPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.application.port.out.StudySubjectPort;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import com.plannie.domain.studysession.StudySession;
import com.plannie.domain.studysession.StudySubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenerateWeeklyReportService implements GenerateWeeklyReportUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final StudySessionPort studySessionPort;
    private final StudySubjectPort studySubjectPort;
    private final GenerateWeeklyReportWithAiPort reportAiPort;

    @Override
    @Transactional(readOnly = true)
    public WeeklyReport generate(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        // 1. 일정 데이터 집계
        List<Schedule> oneTimeSchedules = loadSchedulePort.findOneTimeSchedulesByDateRange(userId, weekStart, weekEnd);
        int total = oneTimeSchedules.size();
        int completed = (int) oneTimeSchedules.stream().filter(Schedule::isCompleted).count();
        List<String> scheduleTitles = new ArrayList<>(oneTimeSchedules.stream()
                .map(s -> s.getTitle() + (s.isCompleted() ? " [완료]" : " [미완료]"))
                .toList());

        // 반복 일정 확장
        List<Schedule> repeatingSchedules = loadSchedulePort.findRepeatingSchedules(userId);
        if (!repeatingSchedules.isEmpty()) {
            List<Long> repeatingIds = repeatingSchedules.stream().map(Schedule::getId).toList();
            Map<String, ScheduleException> exceptions = loadSchedulePort.findExceptions(repeatingIds, weekStart, weekEnd);
            Map<String, Boolean> completions = loadSchedulePort.findCompletions(repeatingIds, weekStart, weekEnd);

            for (Schedule s : repeatingSchedules) {
                LocalDate current = s.getStartDate().isBefore(weekStart) ? weekStart : s.getStartDate();
                LocalDate repeatEnd = s.getRepeatRule().getEndDate() != null && s.getRepeatRule().getEndDate().isBefore(weekEnd)
                        ? s.getRepeatRule().getEndDate() : weekEnd;
                for (LocalDate date = current; !date.isAfter(repeatEnd); date = date.plusDays(1)) {
                    if (!s.getRepeatRule().appliesTo(date)) continue;
                    String key = s.getId() + "_" + date;
                    ScheduleException ex = exceptions.get(key);
                    if (ex != null && ex.isDeleted()) continue;
                    total++;
                    boolean isCompleted = Boolean.TRUE.equals(completions.get(key));
                    if (isCompleted) completed++;
                    scheduleTitles.add(s.getTitle() + (isCompleted ? " [완료]" : " [미완료]"));
                }
            }
        }

        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;

        // 2. 공부 시간 집계
        List<StudySession> sessions = studySessionPort.findByUserIdAndDateRange(userId, weekStart, weekEnd);
        Map<Long, StudySubject> subjectMap = studySubjectPort.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(StudySubject::getId, s -> s));

        Map<Long, Integer> minutesBySubject = sessions.stream()
                .filter(s -> !s.isActive() && s.getDurationMinutes() != null)
                .collect(Collectors.groupingBy(
                        StudySession::getSubjectId,
                        Collectors.summingInt(StudySession::getDurationMinutes)
                ));

        List<SubjectTime> studyBySubject = minutesBySubject.entrySet().stream()
                .map(e -> {
                    StudySubject subject = subjectMap.get(e.getKey());
                    String name = subject != null ? subject.getName() : "삭제된 과목";
                    return new SubjectTime(name, e.getValue());
                })
                .sorted((a, b) -> b.totalMinutes() - a.totalMinutes())
                .toList();

        int totalStudyMinutes = minutesBySubject.values().stream().mapToInt(Integer::intValue).sum();

        // 3. AI 분석
        GenerateWeeklyReportWithAiPort.AiWeeklyReport aiReport = reportAiPort.generate(
                new GenerateWeeklyReportWithAiPort.ReportRequest(
                        weekStart, weekEnd,
                        total, completed, completionRate, scheduleTitles,
                        totalStudyMinutes,
                        studyBySubject.stream()
                                .map(s -> new GenerateWeeklyReportWithAiPort.SubjectTime(s.subjectName(), s.totalMinutes()))
                                .toList()
                )
        );

        return new WeeklyReport(
                weekStart, weekEnd,
                total, completed, completionRate,
                totalStudyMinutes, studyBySubject,
                aiReport.summary(), aiReport.strengths(),
                aiReport.improvements(), aiReport.nextWeekAdvice()
        );
    }
}

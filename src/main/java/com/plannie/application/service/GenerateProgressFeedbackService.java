package com.plannie.application.service;

import com.plannie.application.port.in.GenerateProgressFeedbackUseCase;
import com.plannie.application.port.out.GenerateProgressFeedbackWithAiPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenerateProgressFeedbackService implements GenerateProgressFeedbackUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final GenerateProgressFeedbackWithAiPort feedbackAiPort;

    @Override
    @Transactional(readOnly = true)
    public WeeklyFeedback generateWeeklyFeedback(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        List<Schedule> oneTimeSchedules = loadSchedulePort.findOneTimeSchedulesByDateRange(userId, weekStart, weekEnd);
        int total = oneTimeSchedules.size();
        int completed = (int) oneTimeSchedules.stream().filter(Schedule::isCompleted).count();
        List<String> titles = new ArrayList<>(oneTimeSchedules.stream()
                .map(s -> s.getTitle() + (s.isCompleted() ? " [완료]" : " [미완료]"))
                .toList());

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
                    titles.add(s.getTitle() + (isCompleted ? " [완료]" : " [미완료]"));
                }
            }
        }

        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;

        GenerateProgressFeedbackWithAiPort.FeedbackRequest request =
                new GenerateProgressFeedbackWithAiPort.FeedbackRequest(
                        weekStart, weekEnd, total, completed, completionRate, titles
                );

        GenerateProgressFeedbackWithAiPort.AiFeedback aiFeedback = feedbackAiPort.generate(request);

        return new WeeklyFeedback(
                weekStart, weekEnd, total, completed, completionRate,
                aiFeedback.summary(), aiFeedback.strengths(),
                aiFeedback.improvements(), aiFeedback.nextWeekAdvice()
        );
    }
}

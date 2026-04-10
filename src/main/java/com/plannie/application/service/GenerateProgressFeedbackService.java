package com.plannie.application.service;

import com.plannie.application.port.in.GenerateProgressFeedbackUseCase;
import com.plannie.application.port.out.GenerateProgressFeedbackWithAiPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateProgressFeedbackService implements GenerateProgressFeedbackUseCase {

    private final LoadSchedulePort loadSchedulePort;
    private final GenerateProgressFeedbackWithAiPort feedbackAiPort;

    @Override
    @Transactional(readOnly = true)
    public WeeklyFeedback generateWeeklyFeedback(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        List<Schedule> schedules = loadSchedulePort.findByUserIdAndDateRange(userId, weekStart, weekEnd);

        int total = schedules.size();
        int completed = (int) schedules.stream().filter(Schedule::isCompleted).count();
        double completionRate = total == 0 ? 0.0
                : Math.round(completed * 100.0 / total * 10) / 10.0;

        List<String> titles = schedules.stream()
                .map(s -> s.getTitle() + (s.isCompleted() ? " [완료]" : " [미완료]"))
                .toList();

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

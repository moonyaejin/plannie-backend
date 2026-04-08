package com.plannie.application.service;

import com.plannie.application.port.in.GenerateStudyPlanUseCase;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.RepeatRule;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateStudyPlanService implements GenerateStudyPlanUseCase {

    private final GenerateStudyPlanWithAiPort generateStudyPlanWithAiPort;
    private final SaveSchedulePort saveSchedulePort;

    @Override
    @Transactional
    public StudyPlanResult generate(GenerateStudyPlanCommand command) {
        // 1. AI로 학습 계획 생성
        GenerateStudyPlanWithAiPort.AiStudyPlan aiPlan =
                generateStudyPlanWithAiPort.generate(command);

        // 2. AI 응답 → Schedule 도메인 객체로 변환 후 일괄 저장
        List<Schedule> savedSchedules = aiPlan.schedules().stream()
                .map(item -> buildSchedule(item, command.userId()))
                .map(saveSchedulePort::save)
                .toList();

        return new StudyPlanResult(
                aiPlan.planSummary(),
                aiPlan.weeklyGoals(),
                savedSchedules
        );
    }

    private Schedule buildSchedule(GenerateStudyPlanWithAiPort.AiScheduleItem item, Long userId) {
        LocalDate date = parseDate(item.date());
        LocalTime startTime = parseTime(item.startTime());
        LocalTime endTime = parseTime(item.endTime());

        return Schedule.builder()
                .userId(userId)
                .title(item.title())
                .memo(item.memo())
                .startDate(date)
                .endDate(date)
                .startTime(startTime != null ? startTime : LocalTime.of(20, 0))
                .endTime(endTime != null ? endTime : LocalTime.of(22, 0))
                .completed(false)
                .repeatRule(RepeatRule.none())
                .build();
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

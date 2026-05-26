package com.plannie.application.port.in;

import com.plannie.domain.schedule.Schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 학습 계획 생성 유스케이스
 * - AI가 시험/교재 정보를 기반으로 일정 계획을 생성하고 자동 등록
 */
public interface GenerateStudyPlanUseCase {

    StudyPlanResult generate(GenerateStudyPlanCommand command);

    record GenerateStudyPlanCommand(
            Long userId,
            String examName,
            LocalDate startDate,
            LocalDate examDate,
            String textbook,
            int dailyHours,
            int pastExamRounds,
            List<String> focusAreas
    ) {}

    record StudyPlanResult(
            String planSummary,
            List<String> weeklyGoals,
            List<Schedule> schedules
    ) {}
}

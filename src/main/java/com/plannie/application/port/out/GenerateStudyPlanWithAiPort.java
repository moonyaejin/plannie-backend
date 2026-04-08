package com.plannie.application.port.out;

import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;

import java.util.List;

/**
 * AI 학습 계획 생성 포트
 * - OpenAI Adapter가 구현
 */
public interface GenerateStudyPlanWithAiPort {

    AiStudyPlan generate(GenerateStudyPlanCommand command);

    record AiStudyPlan(
            String planSummary,
            List<String> weeklyGoals,
            List<AiScheduleItem> schedules
    ) {}

    record AiScheduleItem(
            String title,
            String memo,
            String date,       // "yyyy-MM-dd"
            String startTime,  // "HH:mm"
            String endTime,    // "HH:mm"
            int week
    ) {}
}

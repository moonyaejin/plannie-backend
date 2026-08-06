package com.plannie.application.port.in;

import com.plannie.domain.studysession.StudySession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySessionUseCase {

    StudySession start(StartCommand command);

    StudySession stop(Long sessionId, Long userId);

    Optional<StudySession> getActive(Long userId);

    List<StudySession> getByDate(Long userId, LocalDate date);

    List<CategorySummary> getSummary(Long userId, LocalDate startDate, LocalDate endDate);

    List<ScheduleSummary> getScheduleSummary(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * @param categoryId scheduleId가 없을 때 필수 (카테고리 직접 타이머)
     * @param scheduleId 있으면 categoryId는 무시되고 해당 일정의 카테고리로 자동 설정됨 (일정 단위 타이머)
     */
    record StartCommand(Long userId, Long categoryId, Long scheduleId) {}

    record CategorySummary(
            Long categoryId,
            String categoryName,
            String categoryColor,
            int totalMinutes,
            int sessionCount
    ) {}

    record ScheduleSummary(
            Long scheduleId,
            String scheduleTitle,
            Long categoryId,
            int totalMinutes,
            int sessionCount
    ) {}
}

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

    List<SubjectSummary> getSummary(Long userId, LocalDate startDate, LocalDate endDate);

    record StartCommand(Long userId, Long subjectId) {}

    record SubjectSummary(
            Long subjectId,
            String subjectName,
            String subjectColor,
            int totalMinutes,
            int sessionCount
    ) {}
}

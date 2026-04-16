package com.plannie.application.port.in;

import com.plannie.domain.studysession.StudySession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySessionUseCase {

    // 세션 시작
    StudySession start(StartCommand command);

    // 세션 종료
    StudySession stop(Long sessionId, Long userId);

    // 현재 진행 중인 세션 조회
    Optional<StudySession> getActive(Long userId);

    // 날짜별 세션 목록 조회
    List<StudySession> getByDate(Long userId, LocalDate date);

    // 기간별 과목별 합계
    List<SubjectSummary> getSummary(Long userId, LocalDate startDate, LocalDate endDate);

    record StartCommand(
            Long userId,
            String subject,
            Long categoryId  // null 가능
    ) {}

    record SubjectSummary(
            String subject,
            Long categoryId,
            int totalMinutes,
            int sessionCount
    ) {}
}

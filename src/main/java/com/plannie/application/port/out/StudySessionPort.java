package com.plannie.application.port.out;

import com.plannie.domain.studysession.StudySession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySessionPort {

    StudySession save(StudySession session);

    Optional<StudySession> findById(Long id);

    Optional<StudySession> findActiveByUserId(Long userId);

    List<StudySession> findByUserIdAndDate(Long userId, LocalDate date);

    List<StudySession> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    List<StudySession> findByUserIdAndCategoryId(Long userId, Long categoryId);
}

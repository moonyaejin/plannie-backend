package com.plannie.adapter.in.web.dto;

import com.plannie.domain.studysession.StudySession;

import java.time.LocalDateTime;

public record StudySessionResponse(
        Long id,
        String subject,
        Long categoryId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        boolean active
) {
    public static StudySessionResponse from(StudySession session) {
        return new StudySessionResponse(
                session.getId(),
                session.getSubject(),
                session.getCategoryId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationMinutes(),
                session.isActive()
        );
    }
}

package com.plannie.domain.studysession;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudySession {

    private final Long id;
    private final Long userId;
    private final Long categoryId;
    private final Long scheduleId;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;

    public boolean isActive() {
        return endedAt == null;
    }

    public void stop(LocalDateTime endedAt) {
        this.endedAt = endedAt;
        long minutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
        this.durationMinutes = (int) Math.max(minutes, 0);
    }
}

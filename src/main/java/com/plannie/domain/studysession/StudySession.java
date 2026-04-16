package com.plannie.domain.studysession;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudySession {

    private final Long id;
    private final Long userId;
    private final String subject;    // 과목명 (유저 자유 입력)
    private final Long categoryId;   // 선택 연결
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes; // 종료 시 자동 계산

    public boolean isActive() {
        return endedAt == null;
    }

    public void stop(LocalDateTime endedAt) {
        this.endedAt = endedAt;
        long minutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
        this.durationMinutes = (int) Math.max(minutes, 0);
    }
}

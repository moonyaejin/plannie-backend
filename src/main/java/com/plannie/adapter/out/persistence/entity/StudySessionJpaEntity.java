package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_sessions", indexes = {
        @Index(name = "idx_study_session_user_started", columnList = "user_id, started_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudySessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder
    public StudySessionJpaEntity(Long id, Long userId, Long categoryId,
                                  LocalDateTime startedAt, LocalDateTime endedAt,
                                  Integer durationMinutes) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationMinutes = durationMinutes;
    }

    public void stop(LocalDateTime endedAt, int durationMinutes) {
        this.endedAt = endedAt;
        this.durationMinutes = durationMinutes;
    }
}

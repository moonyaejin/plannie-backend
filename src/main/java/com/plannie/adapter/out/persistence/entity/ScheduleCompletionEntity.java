package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "completion_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public ScheduleCompletionEntity(Long scheduleId, LocalDate completionDate,
                                    boolean completed) {
        this.scheduleId = scheduleId;
        this.completionDate = completionDate;
        this.completed = completed;
        if (completed) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void toggleComplete() {
        this.completed = !this.completed;
        this.completedAt = this.completed ? LocalDateTime.now() : null;
    }
}
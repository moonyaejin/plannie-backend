package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.*;
        import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_exceptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "exception_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 20)
    private ExceptionType exceptionType;

    @Column(name = "modified_title", length = 200)
    private String modifiedTitle;

    @Column(name = "modified_memo", columnDefinition = "TEXT")
    private String modifiedMemo;

    @Column(name = "modified_start_time")
    private LocalTime modifiedStartTime;

    @Column(name = "modified_end_time")
    private LocalTime modifiedEndTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ScheduleExceptionEntity(Long scheduleId, LocalDate exceptionDate,
                                   ExceptionType exceptionType, String modifiedTitle,
                                   String modifiedMemo, LocalTime modifiedStartTime,
                                   LocalTime modifiedEndTime) {
        this.scheduleId = scheduleId;
        this.exceptionDate = exceptionDate;
        this.exceptionType = exceptionType;
        this.modifiedTitle = modifiedTitle;
        this.modifiedMemo = modifiedMemo;
        this.modifiedStartTime = modifiedStartTime;
        this.modifiedEndTime = modifiedEndTime;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ExceptionType {
        DELETED,
        MODIFIED
    }
}
package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Schedule JPA 엔티티
 * - 도메인 Schedule과 분리
 * - DB 매핑 전용
 */
@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_schedule_user_date", columnList = "user_id, start_date"),
        @Index(name = "idx_schedule_user_date_time", columnList = "user_id, start_date, start_time, end_time")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "category_id")
    private Long categoryId;

    // 반복 일정 관련 필드
    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", length = 20)
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "repeat_days", length = 50)
    private String repeatDays;  // "MON,TUE,WED" 형식

    @Column(name = "repeat_day_of_month")
    private Integer repeatDayOfMonth;

    @Column(name = "repeat_end_date")
    private LocalDate repeatEndDate;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;  // 낙관적 락용

    @Builder
    public ScheduleJpaEntity(Long id, Long userId, String title, String memo,
                             LocalDate startDate, LocalDate endDate,
                             LocalTime startTime, LocalTime endTime,
                             boolean completed, Long categoryId,
                             RepeatType repeatType, String repeatDays,
                             Integer repeatDayOfMonth, LocalDate repeatEndDate,
                             Integer reminderMinutes) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = completed;
        this.categoryId = categoryId;
        this.repeatType = repeatType != null ? repeatType : RepeatType.NONE;
        this.repeatDays = repeatDays;
        this.repeatDayOfMonth = repeatDayOfMonth;
        this.repeatEndDate = repeatEndDate;
        this.reminderMinutes = reminderMinutes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String memo, LocalDate startDate, LocalDate endDate,
                       LocalTime startTime, LocalTime endTime, Long categoryId,
                       Integer reminderMinutes) {
        this.title = title;
        this.memo = memo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.categoryId = categoryId;
        this.reminderMinutes = reminderMinutes;
    }

    public void toggleComplete() {
        this.completed = !this.completed;
    }

    public enum RepeatType {
        NONE, DAILY, WEEKLY, MONTHLY
    }
}

package com.plannie.domain.notification;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Notification 도메인 모델
 * - JPA 의존성 없음 (순수 도메인 객체)
 */
@Getter
@Builder
public class Notification {

    private final Long id;
    private final Long userId;
    private final Long scheduleId;
    private final String message;
    private final LocalDateTime scheduledAt;
    private boolean read;
    private final LocalDateTime createdAt;

    public void markAsRead() {
        this.read = true;
    }
}

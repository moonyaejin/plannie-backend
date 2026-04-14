package com.plannie.adapter.in.web.dto;

import com.plannie.domain.notification.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long scheduleId,
        String message,
        LocalDateTime scheduledAt,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getScheduleId(),
                notification.getMessage(),
                notification.getScheduledAt(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

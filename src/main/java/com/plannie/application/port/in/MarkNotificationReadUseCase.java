package com.plannie.application.port.in;

public interface MarkNotificationReadUseCase {

    /**
     * 알림 읽음 처리
     */
    void markAsRead(Long notificationId, Long userId);
}

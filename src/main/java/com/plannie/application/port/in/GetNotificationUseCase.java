package com.plannie.application.port.in;

import com.plannie.domain.notification.Notification;

import java.util.List;

public interface GetNotificationUseCase {

    /**
     * 읽지 않은 알림 목록 조회
     */
    List<Notification> getUnreadNotifications(Long userId);
}

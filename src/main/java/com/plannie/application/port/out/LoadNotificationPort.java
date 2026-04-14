package com.plannie.application.port.out;

import com.plannie.domain.notification.Notification;

import java.util.List;
import java.util.Optional;

public interface LoadNotificationPort {

    List<Notification> findUnreadByUserId(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /**
     * 특정 일정에 대한 알림이 이미 생성됐는지 확인 (중복 발송 방지)
     */
    boolean existsByScheduleId(Long scheduleId);
}

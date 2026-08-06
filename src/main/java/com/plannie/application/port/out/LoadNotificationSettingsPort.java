package com.plannie.application.port.out;

import com.plannie.domain.notification.NotificationSettings;

import java.util.Optional;

public interface LoadNotificationSettingsPort {
    Optional<NotificationSettings> findByUserId(Long userId);
}

package com.plannie.application.port.out;

import com.plannie.domain.notification.NotificationSettings;

public interface SaveNotificationSettingsPort {
    NotificationSettings save(NotificationSettings settings);
}

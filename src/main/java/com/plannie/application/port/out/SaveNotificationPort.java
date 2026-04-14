package com.plannie.application.port.out;

import com.plannie.domain.notification.Notification;

public interface SaveNotificationPort {

    Notification save(Notification notification);
}

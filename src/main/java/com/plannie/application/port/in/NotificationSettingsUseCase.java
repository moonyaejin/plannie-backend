package com.plannie.application.port.in;

import com.plannie.domain.notification.NotificationSettings;

/**
 * 알림 설정 조회/수정 유스케이스
 */
public interface NotificationSettingsUseCase {

    NotificationSettings getSettings(Long userId);

    NotificationSettings updateSettings(UpdateNotificationSettingsCommand command);

    record UpdateNotificationSettingsCommand(
            Long userId,
            boolean receiveAlarm,
            boolean scheduleAlarm,
            boolean noticeAlarm,
            boolean smsAlarm,
            boolean emailAlarm
    ) {}
}

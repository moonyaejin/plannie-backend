package com.plannie.adapter.in.web.dto;

import com.plannie.domain.notification.NotificationSettings;

public record NotificationSettingsResponse(
        boolean receiveAlarm,
        boolean scheduleAlarm,
        boolean noticeAlarm,
        boolean smsAlarm,
        boolean emailAlarm
) {
    public static NotificationSettingsResponse from(NotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isReceiveAlarm(),
                settings.isScheduleAlarm(),
                settings.isNoticeAlarm(),
                settings.isSmsAlarm(),
                settings.isEmailAlarm()
        );
    }
}

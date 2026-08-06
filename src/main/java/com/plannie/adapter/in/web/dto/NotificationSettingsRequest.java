package com.plannie.adapter.in.web.dto;

public record NotificationSettingsRequest(
        boolean receiveAlarm,
        boolean scheduleAlarm,
        boolean noticeAlarm,
        boolean smsAlarm,
        boolean emailAlarm
) {}

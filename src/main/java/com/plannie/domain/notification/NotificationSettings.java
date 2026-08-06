package com.plannie.domain.notification;

import lombok.Builder;
import lombok.Getter;

/**
 * NotificationSettings 도메인 모델
 * - 유저별 알림 수신 설정 (1:1)
 */
@Getter
@Builder
public class NotificationSettings {

    private final Long userId;
    private boolean receiveAlarm;
    private boolean scheduleAlarm;
    private boolean noticeAlarm;
    private boolean smsAlarm;
    private boolean emailAlarm;

    public static NotificationSettings defaultsFor(Long userId) {
        return NotificationSettings.builder()
                .userId(userId)
                .receiveAlarm(true)
                .scheduleAlarm(true)
                .noticeAlarm(false)
                .smsAlarm(true)
                .emailAlarm(true)
                .build();
    }

    public void update(boolean receiveAlarm, boolean scheduleAlarm, boolean noticeAlarm,
                       boolean smsAlarm, boolean emailAlarm) {
        this.receiveAlarm = receiveAlarm;
        this.scheduleAlarm = scheduleAlarm;
        this.noticeAlarm = noticeAlarm;
        this.smsAlarm = smsAlarm;
        this.emailAlarm = emailAlarm;
    }
}

package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettingsJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "receive_alarm", nullable = false)
    private boolean receiveAlarm;

    @Column(name = "schedule_alarm", nullable = false)
    private boolean scheduleAlarm;

    @Column(name = "notice_alarm", nullable = false)
    private boolean noticeAlarm;

    @Column(name = "sms_alarm", nullable = false)
    private boolean smsAlarm;

    @Column(name = "email_alarm", nullable = false)
    private boolean emailAlarm;

    @Builder
    public NotificationSettingsJpaEntity(Long userId, boolean receiveAlarm, boolean scheduleAlarm,
                                         boolean noticeAlarm, boolean smsAlarm, boolean emailAlarm) {
        this.userId = userId;
        this.receiveAlarm = receiveAlarm;
        this.scheduleAlarm = scheduleAlarm;
        this.noticeAlarm = noticeAlarm;
        this.smsAlarm = smsAlarm;
        this.emailAlarm = emailAlarm;
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

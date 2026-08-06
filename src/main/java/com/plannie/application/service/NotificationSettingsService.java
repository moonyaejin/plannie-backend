package com.plannie.application.service;

import com.plannie.application.port.in.NotificationSettingsUseCase;
import com.plannie.application.port.out.LoadNotificationSettingsPort;
import com.plannie.application.port.out.SaveNotificationSettingsPort;
import com.plannie.domain.notification.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService implements NotificationSettingsUseCase {

    private final LoadNotificationSettingsPort loadNotificationSettingsPort;
    private final SaveNotificationSettingsPort saveNotificationSettingsPort;

    @Override
    @Transactional
    public NotificationSettings getSettings(Long userId) {
        return loadNotificationSettingsPort.findByUserId(userId)
                .orElseGet(() -> saveNotificationSettingsPort.save(NotificationSettings.defaultsFor(userId)));
    }

    @Override
    @Transactional
    public NotificationSettings updateSettings(UpdateNotificationSettingsCommand command) {
        NotificationSettings settings = loadNotificationSettingsPort.findByUserId(command.userId())
                .orElseGet(() -> NotificationSettings.defaultsFor(command.userId()));

        settings.update(
                command.receiveAlarm(),
                command.scheduleAlarm(),
                command.noticeAlarm(),
                command.smsAlarm(),
                command.emailAlarm()
        );

        return saveNotificationSettingsPort.save(settings);
    }
}

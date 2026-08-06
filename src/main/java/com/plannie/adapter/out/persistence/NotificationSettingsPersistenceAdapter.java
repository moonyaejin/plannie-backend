package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.NotificationSettingsJpaEntity;
import com.plannie.adapter.out.persistence.repository.NotificationSettingsJpaRepository;
import com.plannie.application.port.out.LoadNotificationSettingsPort;
import com.plannie.application.port.out.SaveNotificationSettingsPort;
import com.plannie.domain.notification.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationSettingsPersistenceAdapter
        implements LoadNotificationSettingsPort, SaveNotificationSettingsPort {

    private final NotificationSettingsJpaRepository notificationSettingsRepository;

    @Override
    public Optional<NotificationSettings> findByUserId(Long userId) {
        return notificationSettingsRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public NotificationSettings save(NotificationSettings settings) {
        NotificationSettingsJpaEntity entity = notificationSettingsRepository.findById(settings.getUserId())
                .orElse(null);

        if (entity != null) {
            entity.update(
                    settings.isReceiveAlarm(),
                    settings.isScheduleAlarm(),
                    settings.isNoticeAlarm(),
                    settings.isSmsAlarm(),
                    settings.isEmailAlarm()
            );
            return toDomain(notificationSettingsRepository.save(entity));
        }

        return toDomain(notificationSettingsRepository.save(toEntity(settings)));
    }

    private NotificationSettings toDomain(NotificationSettingsJpaEntity entity) {
        return NotificationSettings.builder()
                .userId(entity.getUserId())
                .receiveAlarm(entity.isReceiveAlarm())
                .scheduleAlarm(entity.isScheduleAlarm())
                .noticeAlarm(entity.isNoticeAlarm())
                .smsAlarm(entity.isSmsAlarm())
                .emailAlarm(entity.isEmailAlarm())
                .build();
    }

    private NotificationSettingsJpaEntity toEntity(NotificationSettings settings) {
        return NotificationSettingsJpaEntity.builder()
                .userId(settings.getUserId())
                .receiveAlarm(settings.isReceiveAlarm())
                .scheduleAlarm(settings.isScheduleAlarm())
                .noticeAlarm(settings.isNoticeAlarm())
                .smsAlarm(settings.isSmsAlarm())
                .emailAlarm(settings.isEmailAlarm())
                .build();
    }
}

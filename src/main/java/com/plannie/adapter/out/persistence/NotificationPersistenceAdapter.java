package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.NotificationJpaEntity;
import com.plannie.adapter.out.persistence.repository.NotificationJpaRepository;
import com.plannie.application.port.out.LoadNotificationPort;
import com.plannie.application.port.out.SaveNotificationPort;
import com.plannie.domain.notification.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort {

    private final NotificationJpaRepository notificationRepository;

    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Notification> findByIdAndUserId(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByScheduleIdAndScheduledDate(Long scheduleId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        return notificationRepository.existsByScheduleIdAndScheduledDate(scheduleId, dayStart, dayEnd);
    }

    @Override
    public Notification save(Notification notification) {
        if (notification.getId() != null) {
            // 기존 엔티티 조회 후 도메인 상태를 그대로 반영
            NotificationJpaEntity entity = notificationRepository.findById(notification.getId())
                    .orElseThrow();
            entity.updateReadStatus(notification.isRead());
            return toDomain(notificationRepository.save(entity));
        }
        return toDomain(notificationRepository.save(toEntity(notification)));
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .scheduleId(entity.getScheduleId())
                .message(entity.getMessage())
                .scheduledAt(entity.getScheduledAt())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private NotificationJpaEntity toEntity(Notification notification) {
        return NotificationJpaEntity.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .scheduleId(notification.getScheduleId())
                .message(notification.getMessage())
                .scheduledAt(notification.getScheduledAt())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

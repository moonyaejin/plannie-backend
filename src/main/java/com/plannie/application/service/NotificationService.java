package com.plannie.application.service;

import com.plannie.application.port.in.GetNotificationUseCase;
import com.plannie.application.port.in.MarkNotificationReadUseCase;
import com.plannie.application.port.out.LoadNotificationPort;
import com.plannie.application.port.out.SaveNotificationPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.notification.Notification;
import com.plannie.domain.schedule.Schedule;
import com.plannie.adapter.out.persistence.repository.ScheduleJpaRepository;
import com.plannie.adapter.out.persistence.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements GetNotificationUseCase, MarkNotificationReadUseCase {

    private final LoadNotificationPort loadNotificationPort;
    private final SaveNotificationPort saveNotificationPort;
    private final ScheduleJpaRepository scheduleJpaRepository;
    private final ScheduleMapper scheduleMapper;

    /**
     * 매 분마다 30분 후 시작하는 일정을 조회하여 알림 생성
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void createUpcomingNotifications() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime thirtyMinutesLater = now.plusMinutes(30);

        // 30분 후 시작하는 일정 조회 (±1분 범위로 정확히 한 번만 발송)
        LocalTime from = thirtyMinutesLater.minusSeconds(30);
        LocalTime to = thirtyMinutesLater.plusSeconds(30);

        List<Schedule> upcomingSchedules = scheduleJpaRepository
                .findByStartDateAndStartTimeBetween(today, from, to)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();

        for (Schedule schedule : upcomingSchedules) {
            if (loadNotificationPort.existsByScheduleId(schedule.getId())) {
                continue;
            }

            Notification notification = Notification.builder()
                    .userId(schedule.getUserId())
                    .scheduleId(schedule.getId())
                    .message("30분 후 일정이 시작됩니다: " + schedule.getTitle())
                    .scheduledAt(LocalDateTime.of(today, schedule.getStartTime()))
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            saveNotificationPort.save(notification);
            log.debug("알림 생성 - userId: {}, scheduleId: {}, title: {}",
                    schedule.getUserId(), schedule.getId(), schedule.getTitle());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return loadNotificationPort.findUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = loadNotificationPort.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        saveNotificationPort.save(notification);
    }
}

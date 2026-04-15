package com.plannie.application.service;

import com.plannie.application.port.in.GetNotificationUseCase;
import com.plannie.application.port.in.MarkNotificationReadUseCase;
import com.plannie.application.port.out.LoadNotificationPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveNotificationPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.notification.Notification;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements GetNotificationUseCase, MarkNotificationReadUseCase {

    private final LoadNotificationPort loadNotificationPort;
    private final SaveNotificationPort saveNotificationPort;
    private final LoadSchedulePort loadSchedulePort;

    private static final List<Integer> SUPPORTED_REMINDER_MINUTES = List.of(5, 10, 30);

    /**
     * 매 분마다 각 알림 설정(5/10/30분 전)에 맞는 일정을 조회하여 알림 생성
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void createUpcomingNotifications() {
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(seoulZone);
        LocalTime now = LocalTime.now(seoulZone);

        for (int minutes : SUPPORTED_REMINDER_MINUTES) {
            LocalTime targetTime = now.plusMinutes(minutes);
            LocalTime from = targetTime.minusSeconds(30);
            LocalTime to = targetTime.plusSeconds(30);

            List<Schedule> upcomingSchedules = loadSchedulePort
                    .findByDateAndStartTimeBetweenAndReminderMinutes(today, from, to, minutes);

            for (Schedule schedule : upcomingSchedules) {
                if (loadNotificationPort.existsByScheduleIdAndScheduledDate(schedule.getId(), today)) {
                    continue;
                }

                Notification notification = Notification.builder()
                        .userId(schedule.getUserId())
                        .scheduleId(schedule.getId())
                        .message(minutes + "분 후 일정이 시작됩니다: " + schedule.getTitle())
                        .scheduledAt(LocalDateTime.of(today, schedule.getStartTime()))
                        .read(false)
                        .createdAt(LocalDateTime.now(seoulZone))
                        .build();

                saveNotificationPort.save(notification);
                log.debug("알림 생성 - userId: {}, scheduleId: {}, reminderMinutes: {}, title: {}",
                        schedule.getUserId(), schedule.getId(), minutes, schedule.getTitle());
            }
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

package com.plannie.application.port.out;

import com.plannie.domain.notification.Notification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadNotificationPort {

    List<Notification> findUnreadByUserId(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /**
     * 특정 일정 + 날짜 조합으로 알림 중복 확인
     * scheduleId만으로는 반복 일정의 매 회차 알림이 차단됨
     */
    boolean existsByScheduleIdAndScheduledDate(Long scheduleId, LocalDate date);
}

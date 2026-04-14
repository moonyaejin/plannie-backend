package com.plannie.application.service;

import com.plannie.application.port.out.LoadNotificationPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveNotificationPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.domain.notification.Notification;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private LoadNotificationPort loadNotificationPort;
    @Mock private SaveNotificationPort saveNotificationPort;
    @Mock private LoadSchedulePort loadSchedulePort;

    @InjectMocks
    private NotificationService notificationService;

    private Schedule schedule(Long id, Long userId, String title, LocalTime startTime) {
        return Schedule.builder()
                .id(id)
                .userId(userId)
                .title(title)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .completed(false)
                .build();
    }

    @Test
    @DisplayName("30분 후 일정이 있고 알림이 없으면 알림을 생성한다")
    void 알림_생성() {
        Schedule upcoming = schedule(1L, 10L, "스프링 공부", LocalTime.now().plusMinutes(30));
        given(loadSchedulePort.findByDateAndStartTimeBetween(any(), any(), any()))
                .willReturn(List.of(upcoming));
        given(loadNotificationPort.existsByScheduleId(1L)).willReturn(false);

        notificationService.createUpcomingNotifications();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getScheduleId()).isEqualTo(1L);
        assertThat(saved.getMessage()).contains("스프링 공부");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    @DisplayName("이미 알림이 있는 일정은 중복 생성하지 않는다")
    void 중복_알림_방지() {
        Schedule upcoming = schedule(1L, 10L, "스프링 공부", LocalTime.now().plusMinutes(30));
        given(loadSchedulePort.findByDateAndStartTimeBetween(any(), any(), any()))
                .willReturn(List.of(upcoming));
        given(loadNotificationPort.existsByScheduleId(1L)).willReturn(true);

        notificationService.createUpcomingNotifications();

        verify(saveNotificationPort, never()).save(any());
    }

    @Test
    @DisplayName("읽지 않은 알림 목록을 조회한다")
    void 읽지않은_알림_조회() {
        Notification notification = Notification.builder()
                .id(1L).userId(10L).scheduleId(1L)
                .message("30분 후 일정이 시작됩니다: 스프링 공부")
                .scheduledAt(LocalDateTime.now())
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        given(loadNotificationPort.findUnreadByUserId(10L)).willReturn(List.of(notification));

        List<Notification> result = notificationService.getUnreadNotifications(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).contains("스프링 공부");
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
    void 없는_알림_읽음처리_예외() {
        given(loadNotificationPort.findByIdAndUserId(999L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 10L))
                .isInstanceOf(BusinessException.class);
    }
}

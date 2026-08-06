package com.plannie.application.service;

import com.plannie.application.port.in.NotificationSettingsUseCase.UpdateNotificationSettingsCommand;
import com.plannie.application.port.out.LoadNotificationSettingsPort;
import com.plannie.application.port.out.SaveNotificationSettingsPort;
import com.plannie.domain.notification.NotificationSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    @Mock
    private LoadNotificationSettingsPort loadNotificationSettingsPort;

    @Mock
    private SaveNotificationSettingsPort saveNotificationSettingsPort;

    @InjectMocks
    private NotificationSettingsService notificationSettingsService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("설정이 없으면 기본값을 생성해서 반환한다")
    void 설정_없으면_기본값_생성() {
        given(loadNotificationSettingsPort.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(saveNotificationSettingsPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        NotificationSettings result = notificationSettingsService.getSettings(USER_ID);

        assertThat(result.isReceiveAlarm()).isTrue();
        assertThat(result.isScheduleAlarm()).isTrue();
        assertThat(result.isNoticeAlarm()).isFalse();
        assertThat(result.isSmsAlarm()).isTrue();
        assertThat(result.isEmailAlarm()).isTrue();
        verify(saveNotificationSettingsPort).save(any());
    }

    @Test
    @DisplayName("설정이 있으면 저장된 값을 그대로 반환한다")
    void 설정_있으면_그대로_반환() {
        NotificationSettings existing = NotificationSettings.builder()
                .userId(USER_ID)
                .receiveAlarm(false).scheduleAlarm(false).noticeAlarm(true)
                .smsAlarm(false).emailAlarm(false)
                .build();
        given(loadNotificationSettingsPort.findByUserId(USER_ID)).willReturn(Optional.of(existing));

        NotificationSettings result = notificationSettingsService.getSettings(USER_ID);

        assertThat(result.isReceiveAlarm()).isFalse();
        assertThat(result.isNoticeAlarm()).isTrue();
    }

    @Test
    @DisplayName("설정 수정 시 변경된 값으로 저장된다")
    void 설정_수정() {
        NotificationSettings existing = NotificationSettings.defaultsFor(USER_ID);
        given(loadNotificationSettingsPort.findByUserId(USER_ID)).willReturn(Optional.of(existing));
        given(saveNotificationSettingsPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        UpdateNotificationSettingsCommand command = new UpdateNotificationSettingsCommand(
                USER_ID, false, false, true, false, false
        );

        NotificationSettings result = notificationSettingsService.updateSettings(command);

        assertThat(result.isReceiveAlarm()).isFalse();
        assertThat(result.isNoticeAlarm()).isTrue();
        assertThat(result.isEmailAlarm()).isFalse();
    }

    @Test
    @DisplayName("설정이 없는 유저가 수정 요청하면 기본값에서 시작해 새로 생성된다")
    void 설정_없는_유저_수정시_신규생성() {
        given(loadNotificationSettingsPort.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(saveNotificationSettingsPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        UpdateNotificationSettingsCommand command = new UpdateNotificationSettingsCommand(
                USER_ID, true, true, true, true, true
        );

        NotificationSettings result = notificationSettingsService.updateSettings(command);

        assertThat(result.isNoticeAlarm()).isTrue();
        verify(saveNotificationSettingsPort).save(any());
    }
}

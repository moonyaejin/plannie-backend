package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.NotificationSettingsRequest;
import com.plannie.adapter.in.web.dto.NotificationSettingsResponse;
import com.plannie.application.port.in.NotificationSettingsUseCase;
import com.plannie.application.port.in.NotificationSettingsUseCase.UpdateNotificationSettingsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "NotificationSettings", description = "알림 설정 API")
@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsUseCase notificationSettingsUseCase;

    @Operation(summary = "알림 설정 조회", description = "로그인한 사용자의 알림 설정을 조회합니다 (없으면 기본값으로 생성)")
    @GetMapping
    public NotificationSettingsResponse getSettings(@AuthenticationPrincipal Long userId) {
        return NotificationSettingsResponse.from(notificationSettingsUseCase.getSettings(userId));
    }

    @Operation(summary = "알림 설정 수정", description = "알림 설정 전체를 갱신합니다")
    @PutMapping
    public NotificationSettingsResponse updateSettings(
            @AuthenticationPrincipal Long userId,
            @RequestBody NotificationSettingsRequest request) {

        NotificationSettingsUseCase.UpdateNotificationSettingsCommand command =
                new UpdateNotificationSettingsCommand(
                        userId,
                        request.receiveAlarm(),
                        request.scheduleAlarm(),
                        request.noticeAlarm(),
                        request.smsAlarm(),
                        request.emailAlarm()
                );

        return NotificationSettingsResponse.from(notificationSettingsUseCase.updateSettings(command));
    }
}

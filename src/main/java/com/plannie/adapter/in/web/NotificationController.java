package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.NotificationResponse;
import com.plannie.application.port.in.GetNotificationUseCase;
import com.plannie.application.port.in.MarkNotificationReadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationUseCase getNotificationUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    @Operation(summary = "읽지 않은 알림 목록 조회", description = "로그인한 사용자의 읽지 않은 알림을 조회합니다")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal Long userId) {

        List<NotificationResponse> responses = getNotificationUseCase.getUnreadNotifications(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        markNotificationReadUseCase.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
}

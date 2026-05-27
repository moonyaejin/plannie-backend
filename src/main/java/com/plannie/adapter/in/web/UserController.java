package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.UserProfileRequest;
import com.plannie.adapter.in.web.dto.UserProfileResponse;
import com.plannie.application.port.in.UserProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileUseCase userProfileUseCase;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal Long userId) {
        return UserProfileResponse.from(userProfileUseCase.getProfile(userId));
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        userProfileUseCase.updateProfile(new UserProfileUseCase.UpdateProfileCommand(
                userId,
                request.nickname(),
                request.phone(),
                request.address(),
                request.birth(),
                request.gender(),
                request.password()
        ));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal Long userId) {
        userProfileUseCase.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}

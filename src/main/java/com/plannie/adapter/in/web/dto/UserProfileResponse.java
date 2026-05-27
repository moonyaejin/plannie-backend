package com.plannie.adapter.in.web.dto;

import com.plannie.application.port.in.UserProfileUseCase.ProfileResult;

import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String name,
        String phone,
        String address,
        LocalDate birth,
        String gender,
        String profileImage
) {
    public static UserProfileResponse from(ProfileResult result) {
        return new UserProfileResponse(
                result.id(),
                result.email(),
                result.nickname(),
                result.name(),
                result.phone(),
                result.address(),
                result.birth(),
                result.gender(),
                result.profileImage()
        );
    }
}

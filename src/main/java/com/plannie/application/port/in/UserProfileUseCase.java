package com.plannie.application.port.in;

import java.time.LocalDate;

public interface UserProfileUseCase {

    ProfileResult getProfile(Long userId);

    void updateProfile(UpdateProfileCommand command);

    void deleteAccount(Long userId);

    record ProfileResult(
            Long id,
            String email,
            String nickname,
            String name,
            String phone,
            String address,
            LocalDate birth,
            String gender,
            String profileImage
    ) {}

    record UpdateProfileCommand(
            Long userId,
            String nickname,
            String phone,
            String address,
            LocalDate birth,
            String gender,
            String rawPassword
    ) {}
}

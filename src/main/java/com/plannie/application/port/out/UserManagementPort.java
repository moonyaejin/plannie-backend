package com.plannie.application.port.out;

import java.time.LocalDate;

public interface UserManagementPort {
    void updateProfile(Long userId, String nickname, String phone, String address,
                       LocalDate birth, String gender, String profileImage, String encodedPassword);
    void deleteById(Long userId);
}

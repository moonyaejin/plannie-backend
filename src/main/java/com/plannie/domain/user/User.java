package com.plannie.domain.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User 도메인 모델
 */
@Getter
public class User {

    private final Long id;
    private final String email;
    private String password;
    private String nickname;
    private String name;
    private String phone;
    private String address;
    private LocalDate birth;
    private String gender;
    private String profileImage;
    private final LocalDateTime createdAt;

    @Builder
    public User(Long id, String email, String password, String nickname,
                String name, String phone, String address, LocalDate birth,
                String gender, String profileImage, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.birth = birth;
        this.gender = gender;
        this.profileImage = profileImage;
        this.createdAt = createdAt;
    }

    /**
     * 프로필 정보 수정
     */
    public void updateProfile(String nickname, String phone, String address,
                              LocalDate birth, String gender, String profileImage) {
        this.nickname = nickname;
        this.phone = phone;
        this.address = address;
        this.birth = birth;
        this.gender = gender;
        this.profileImage = profileImage;
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}

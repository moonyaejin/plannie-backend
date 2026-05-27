package com.plannie.application.service;

import com.plannie.application.port.in.UserProfileUseCase;
import com.plannie.application.port.out.LoadUserPort;
import com.plannie.application.port.out.UserManagementPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService implements UserProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final UserManagementPort userManagementPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ProfileResult getProfile(Long userId) {
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toResult(user);
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileCommand command) {
        loadUserPort.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = (command.rawPassword() != null && !command.rawPassword().isBlank())
                ? passwordEncoder.encode(command.rawPassword())
                : null;

        userManagementPort.updateProfile(
                command.userId(),
                command.nickname(),
                command.phone(),
                command.address(),
                command.birth(),
                command.gender(),
                null,
                encodedPassword
        );
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        loadUserPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userManagementPort.deleteById(userId);
    }

    private ProfileResult toResult(User user) {
        return new ProfileResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getBirth(),
                user.getGender(),
                user.getProfileImage()
        );
    }
}

package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.UserJpaEntity;
import com.plannie.adapter.out.persistence.repository.UserJpaRepository;
import com.plannie.application.port.out.LoadUserPort;
import com.plannie.application.port.out.SaveUserPort;
import com.plannie.application.port.out.UserManagementPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort, UserManagementPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .build();
        return toDomain(userJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String nickname, String phone, String address,
                              LocalDate birth, String gender, String profileImage, String encodedPassword) {
        UserJpaEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        entity.updateProfile(nickname, phone, address, birth, gender, profileImage);
        if (encodedPassword != null) {
            entity.changePassword(encodedPassword);
        }
    }

    @Override
    public void deleteById(Long userId) {
        userJpaRepository.deleteById(userId);
    }

    private User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .nickname(entity.getNickname())
                .name(entity.getName())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .birth(entity.getBirth())
                .gender(entity.getGender())
                .profileImage(entity.getProfileImage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

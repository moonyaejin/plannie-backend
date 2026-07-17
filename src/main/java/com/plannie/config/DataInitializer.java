package com.plannie.config;

import com.plannie.adapter.out.persistence.entity.UserJpaEntity;
import com.plannie.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminProperties.email()) || !StringUtils.hasText(adminProperties.password())) {
            log.warn("관리자 계정 환경변수(app.admin.email/password)가 설정되지 않아 관리자 계정 생성을 건너뜁니다.");
            return;
        }

        if (!userJpaRepository.existsByEmail(adminProperties.email())) {
            UserJpaEntity admin = UserJpaEntity.builder()
                    .email(adminProperties.email())
                    .password(passwordEncoder.encode(adminProperties.password()))
                    .nickname(adminProperties.nickname())
                    .build();
            userJpaRepository.save(admin);
            log.info("관리자 계정 생성 완료: {}", adminProperties.email());
        }
    }
}

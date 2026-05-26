package com.plannie.config;

import com.plannie.adapter.out.persistence.entity.UserJpaEntity;
import com.plannie.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    // 관리자 계정 정보 — 운영 환경에선 환경변수로 관리 권장
    private static final String ADMIN_EMAIL    = "admin@plannie.com";
    private static final String ADMIN_PASSWORD = "***REDACTED-CREDENTIAL***";
    private static final String ADMIN_NICKNAME = "관리자";

    @Override
    public void run(ApplicationArguments args) {
        if (!userJpaRepository.existsByEmail(ADMIN_EMAIL)) {
            UserJpaEntity admin = UserJpaEntity.builder()
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .nickname(ADMIN_NICKNAME)
                    .build();
            userJpaRepository.save(admin);
            log.info("관리자 계정 생성 완료: {}", ADMIN_EMAIL);
        }
    }
}

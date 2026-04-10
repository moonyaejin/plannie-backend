package com.plannie.application.service;

import com.plannie.application.port.in.AuthUseCase;
import com.plannie.application.port.out.LoadUserPort;
import com.plannie.application.port.out.SaveUserPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.user.User;
import com.plannie.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public TokenResult register(RegisterCommand command) {
        if (loadUserPort.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(command.email())
                .password(passwordEncoder.encode(command.password()))
                .nickname(command.nickname())
                .build();

        User saved = saveUserPort.save(user);
        String token = jwtProvider.generateToken(saved.getId(), saved.getEmail());

        return new TokenResult(saved.getId(), saved.getEmail(), saved.getNickname(), token);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResult login(LoginCommand command) {
        User user = loadUserPort.findByEmail(command.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail());

        return new TokenResult(user.getId(), user.getEmail(), user.getNickname(), token);
    }
}

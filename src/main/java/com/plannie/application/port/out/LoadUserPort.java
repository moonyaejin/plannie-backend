package com.plannie.application.port.out;

import com.plannie.domain.user.User;

import java.util.Optional;

public interface LoadUserPort {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

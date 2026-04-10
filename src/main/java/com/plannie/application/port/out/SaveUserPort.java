package com.plannie.application.port.out;

import com.plannie.domain.user.User;

public interface SaveUserPort {
    User save(User user);
}

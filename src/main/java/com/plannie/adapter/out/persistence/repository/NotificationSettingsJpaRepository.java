package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.NotificationSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsJpaRepository extends JpaRepository<NotificationSettingsJpaEntity, Long> {
}

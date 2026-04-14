package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    List<NotificationJpaEntity> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<NotificationJpaEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByScheduleId(Long scheduleId);
}

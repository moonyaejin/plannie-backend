package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    List<NotificationJpaEntity> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<NotificationJpaEntity> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(n) > 0 FROM NotificationJpaEntity n " +
           "WHERE n.scheduleId = :scheduleId " +
           "AND n.scheduledAt >= :dayStart AND n.scheduledAt < :dayEnd")
    boolean existsByScheduleIdAndScheduledDate(
            @Param("scheduleId") Long scheduleId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );
}

package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.ScheduleCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleCompletionRepository extends JpaRepository<ScheduleCompletionEntity, Long> {

    Optional<ScheduleCompletionEntity> findByScheduleIdAndCompletionDate(
            Long scheduleId, LocalDate completionDate);

    void deleteByScheduleId(Long scheduleId);

    @Query("SELECT c FROM ScheduleCompletionEntity c WHERE c.scheduleId IN :ids " +
            "AND c.completionDate BETWEEN :startDate AND :endDate")
    List<ScheduleCompletionEntity> findByScheduleIdsAndDateRange(
            @Param("ids") List<Long> scheduleIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
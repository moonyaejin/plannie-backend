package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.ScheduleExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleExceptionEntity, Long> {

    List<ScheduleExceptionEntity> findByScheduleIdIn(List<Long> scheduleIds);

    Optional<ScheduleExceptionEntity> findByScheduleIdAndExceptionDate(
            Long scheduleId, LocalDate exceptionDate);

    @Query("SELECT e FROM ScheduleExceptionEntity e WHERE e.scheduleId IN :ids " +
            "AND e.exceptionDate BETWEEN :startDate AND :endDate")
    List<ScheduleExceptionEntity> findByScheduleIdsAndDateRange(
            @Param("ids") List<Long> scheduleIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
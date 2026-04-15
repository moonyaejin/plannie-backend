package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity;
import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity.RepeatType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Schedule JPA Repository
 */
public interface ScheduleJpaRepository extends JpaRepository<ScheduleJpaEntity, Long> {

    Optional<ScheduleJpaEntity> findByIdAndUserId(Long id, Long userId);

    List<ScheduleJpaEntity> findByUserIdAndStartDate(Long userId, LocalDate startDate);

    @Query("SELECT s FROM ScheduleJpaEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.startDate >= :startDate AND s.startDate <= :endDate " +
           "ORDER BY s.startDate, s.startTime")
    List<ScheduleJpaEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 시간 충돌 조회
     * - 같은 날짜에 시간이 겹치는 일정 찾기
     */
    @Query("SELECT s FROM ScheduleJpaEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.startDate = :date " +
           "AND s.id != :excludeId " +
           "AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)")
    List<ScheduleJpaEntity> findConflictingSchedules(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * 비관적 락을 사용한 조회 (동시성 제어)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleJpaEntity s WHERE s.id = :id")
    Optional<ScheduleJpaEntity> findByIdWithLock(@Param("id") Long id);

    /**
     * 월별 일정 조회
     */
    @Query("SELECT s FROM ScheduleJpaEntity s " +
           "WHERE s.userId = :userId " +
           "AND YEAR(s.startDate) = :year " +
           "AND MONTH(s.startDate) = :month " +
           "ORDER BY s.startDate, s.startTime")
    List<ScheduleJpaEntity> findByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // ScheduleJpaRepository에 추가
    List<ScheduleJpaEntity> findByUserIdAndStartDateBetweenAndRepeatType(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            RepeatType repeatType
    );

    // userId의 반복 일정 조회 (repeatType이 NONE이 아닌 것)
    List<ScheduleJpaEntity> findByUserIdAndRepeatTypeNot(Long userId, ScheduleJpaEntity.RepeatType repeatType);

    // 알림용: 특정 날짜에 시작 시간이 범위 안에 있는 일정 조회
    List<ScheduleJpaEntity> findByStartDateAndStartTimeBetween(LocalDate startDate, LocalTime from, LocalTime to);

    // 알림용: reminderMinutes 설정된 일정만 조회
    List<ScheduleJpaEntity> findByStartDateAndStartTimeBetweenAndReminderMinutes(
            LocalDate startDate, LocalTime from, LocalTime to, int reminderMinutes);
}

package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.StudySessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudySessionJpaRepository extends JpaRepository<StudySessionJpaEntity, Long> {

    // 진행 중인 세션 조회 (endedAt이 null)
    Optional<StudySessionJpaEntity> findByUserIdAndEndedAtIsNull(Long userId);

    // 날짜별 조회
    @Query("SELECT s FROM StudySessionJpaEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.startedAt >= :dayStart AND s.startedAt < :dayEnd " +
           "ORDER BY s.startedAt DESC")
    List<StudySessionJpaEntity> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    // 기간별 조회
    @Query("SELECT s FROM StudySessionJpaEntity s " +
           "WHERE s.userId = :userId " +
           "AND s.startedAt >= :from AND s.startedAt < :to " +
           "ORDER BY s.startedAt DESC")
    List<StudySessionJpaEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // 카테고리별 전체 세션 조회
    List<StudySessionJpaEntity> findByUserIdAndCategoryId(Long userId, Long categoryId);
}

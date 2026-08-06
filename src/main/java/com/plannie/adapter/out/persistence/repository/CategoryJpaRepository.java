package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.CategoryJpaEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    Optional<CategoryJpaEntity> findByIdAndUserId(Long id, Long userId);
    /**
     * 기본 카테고리(userId = null) + 해당 유저의 카테고리 조회
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.userId IS NULL OR c.userId = :userId")
    List<CategoryJpaEntity> findAllByUserIdOrDefault(@Param("userId") Long userId);

    /**
     * 기본 카테고리(userId = null) 또는 해당 유저 소유 카테고리 단건 조회
     */
    @Query("SELECT c FROM CategoryJpaEntity c WHERE c.id = :id AND (c.userId IS NULL OR c.userId = :userId)")
    Optional<CategoryJpaEntity> findByIdAndUserIdOrDefault(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndCategoryName(Long userId, String categoryName);
}
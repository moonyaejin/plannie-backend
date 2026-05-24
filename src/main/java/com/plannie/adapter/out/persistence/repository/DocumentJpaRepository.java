package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, Long> {

    List<DocumentJpaEntity> findByUserId(Long userId);

    Optional<DocumentJpaEntity> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}

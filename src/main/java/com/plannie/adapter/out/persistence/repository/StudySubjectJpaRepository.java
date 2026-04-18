package com.plannie.adapter.out.persistence.repository;

import com.plannie.adapter.out.persistence.entity.StudySubjectJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudySubjectJpaRepository extends JpaRepository<StudySubjectJpaEntity, Long> {

    Optional<StudySubjectJpaEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    List<StudySubjectJpaEntity> findAllByUserId(Long userId);
}

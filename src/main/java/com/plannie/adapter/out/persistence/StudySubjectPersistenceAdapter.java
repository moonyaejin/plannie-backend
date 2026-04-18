package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.StudySubjectJpaEntity;
import com.plannie.adapter.out.persistence.repository.StudySubjectJpaRepository;
import com.plannie.application.port.out.StudySubjectPort;
import com.plannie.domain.studysession.StudySubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StudySubjectPersistenceAdapter implements StudySubjectPort {

    private final StudySubjectJpaRepository repository;

    @Override
    public StudySubject save(StudySubject subject) {
        if (subject.getId() != null) {
            StudySubjectJpaEntity entity = repository.findById(subject.getId()).orElseThrow();
            entity.update(subject.getName(), subject.getColor());
            return toDomain(repository.save(entity));
        }
        return toDomain(repository.save(StudySubjectJpaEntity.builder()
                .userId(subject.getUserId())
                .name(subject.getName())
                .color(subject.getColor())
                .build()));
    }

    @Override
    public Optional<StudySubject> findByIdAndUserId(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name) {
        return repository.existsByUserIdAndName(userId, name);
    }

    @Override
    public List<StudySubject> findAllByUserId(Long userId) {
        return repository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private StudySubject toDomain(StudySubjectJpaEntity entity) {
        return StudySubject.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .color(entity.getColor())
                .build();
    }
}

package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.StudySessionJpaEntity;
import com.plannie.adapter.out.persistence.repository.StudySessionJpaRepository;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.domain.studysession.StudySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StudySessionPersistenceAdapter implements StudySessionPort {

    private final StudySessionJpaRepository repository;

    @Override
    public StudySession save(StudySession session) {
        if (session.getId() != null) {
            StudySessionJpaEntity entity = repository.findById(session.getId()).orElseThrow();
            if (!session.isActive()) {
                entity.stop(session.getEndedAt(), session.getDurationMinutes());
            }
            return toDomain(repository.save(entity));
        }
        return toDomain(repository.save(StudySessionJpaEntity.builder()
                .userId(session.getUserId())
                .subjectId(session.getSubjectId())
                .startedAt(session.getStartedAt())
                .build()));
    }

    @Override
    public Optional<StudySession> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StudySession> findActiveByUserId(Long userId) {
        return repository.findByUserIdAndEndedAtIsNull(userId).map(this::toDomain);
    }

    @Override
    public List<StudySession> findByUserIdAndDate(Long userId, LocalDate date) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime dayStart = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay(zone).toLocalDateTime();
        return repository.findByUserIdAndDate(userId, dayStart, dayEnd)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudySession> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime from = startDate.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay(zone).toLocalDateTime();
        return repository.findByUserIdAndDateRange(userId, from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<StudySession> findByUserIdAndSubjectId(Long userId, Long subjectId) {
        return repository.findByUserIdAndSubjectId(userId, subjectId)
                .stream().map(this::toDomain).toList();
    }

    private StudySession toDomain(StudySessionJpaEntity entity) {
        return StudySession.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .subjectId(entity.getSubjectId())
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .durationMinutes(entity.getDurationMinutes())
                .build();
    }
}

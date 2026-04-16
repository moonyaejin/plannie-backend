package com.plannie.application.port.out;

import com.plannie.domain.studysession.StudySubject;

import java.util.List;
import java.util.Optional;

public interface StudySubjectPort {

    StudySubject save(StudySubject subject);

    Optional<StudySubject> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    List<StudySubject> findAllByUserId(Long userId);

    void delete(Long id);
}

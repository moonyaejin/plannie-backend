package com.plannie.application.port.in;

import com.plannie.domain.studysession.StudySubject;

import java.util.List;

public interface StudySubjectUseCase {

    StudySubject create(CreateCommand command);

    StudySubject update(UpdateCommand command);

    void delete(Long subjectId, Long userId);

    List<StudySubject> getAll(Long userId);

    record CreateCommand(Long userId, String name, String color) {}

    record UpdateCommand(Long subjectId, Long userId, String name, String color) {}
}

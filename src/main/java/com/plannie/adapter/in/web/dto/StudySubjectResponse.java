package com.plannie.adapter.in.web.dto;

import com.plannie.domain.studysession.StudySubject;

public record StudySubjectResponse(Long id, String name, String color) {

    public static StudySubjectResponse from(StudySubject subject) {
        return new StudySubjectResponse(subject.getId(), subject.getName(), subject.getColor());
    }
}

package com.plannie.application.service;

import com.plannie.application.port.in.StudySubjectUseCase;
import com.plannie.application.port.out.StudySubjectPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.studysession.StudySubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySubjectService implements StudySubjectUseCase {

    private final StudySubjectPort studySubjectPort;

    @Override
    @Transactional
    public StudySubject create(CreateCommand command) {
        if (studySubjectPort.existsByUserIdAndName(command.userId(), command.name())) {
            throw new BusinessException(ErrorCode.STUDY_SUBJECT_DUPLICATE);
        }

        return studySubjectPort.save(StudySubject.builder()
                .userId(command.userId())
                .name(command.name())
                .color(command.color())
                .build());
    }

    @Override
    @Transactional
    public StudySubject update(UpdateCommand command) {
        StudySubject subject = studySubjectPort
                .findByIdAndUserId(command.subjectId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_SUBJECT_NOT_FOUND));

        subject.update(command.name(), command.color());
        return studySubjectPort.save(subject);
    }

    @Override
    @Transactional
    public void delete(Long subjectId, Long userId) {
        studySubjectPort.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_SUBJECT_NOT_FOUND));

        studySubjectPort.delete(subjectId);
    }

    @Override
    public List<StudySubject> getAll(Long userId) {
        return studySubjectPort.findAllByUserId(userId);
    }
}

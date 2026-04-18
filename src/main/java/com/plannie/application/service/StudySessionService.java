package com.plannie.application.service;

import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.application.port.out.StudySubjectPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.studysession.StudySession;
import com.plannie.domain.studysession.StudySubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySessionService implements StudySessionUseCase {

    private final StudySessionPort studySessionPort;
    private final StudySubjectPort studySubjectPort;

    @Override
    @Transactional
    public StudySession start(StartCommand command) {
        // 과목 존재 + 권한 확인
        studySubjectPort.findByIdAndUserId(command.subjectId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_SUBJECT_NOT_FOUND));

        // 이미 진행 중인 세션 확인
        studySessionPort.findActiveByUserId(command.userId()).ifPresent(s -> {
            throw new BusinessException(ErrorCode.STUDY_SESSION_ALREADY_ACTIVE);
        });

        StudySession session = StudySession.builder()
                .userId(command.userId())
                .subjectId(command.subjectId())
                .startedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();

        return studySessionPort.save(session);
    }

    @Override
    @Transactional
    public StudySession stop(Long sessionId, Long userId) {
        StudySession session = studySessionPort.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_SESSION_NOT_FOUND));

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.STUDY_SESSION_NOT_ACTIVE);
        }

        session.stop(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        return studySessionPort.save(session);
    }

    @Override
    public Optional<StudySession> getActive(Long userId) {
        return studySessionPort.findActiveByUserId(userId);
    }

    @Override
    public List<StudySession> getByDate(Long userId, LocalDate date) {
        return studySessionPort.findByUserIdAndDate(userId, date);
    }

    @Override
    public List<SubjectSummary> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<StudySession> sessions = studySessionPort.findByUserIdAndDateRange(userId, startDate, endDate);

        // 과목 정보 조회 (이름, 색상)
        Map<Long, StudySubject> subjectMap = studySubjectPort.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(StudySubject::getId, s -> s));

        // subjectId 기준으로 그룹핑 후 합산
        Map<Long, List<StudySession>> grouped = sessions.stream()
                .filter(s -> !s.isActive())
                .collect(Collectors.groupingBy(StudySession::getSubjectId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long subjectId = entry.getKey();
                    List<StudySession> group = entry.getValue();
                    int totalMinutes = group.stream()
                            .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                            .sum();
                    StudySubject subject = subjectMap.get(subjectId);
                    String name = subject != null ? subject.getName() : "삭제된 과목";
                    String color = subject != null ? subject.getColor() : null;
                    return new SubjectSummary(subjectId, name, color, totalMinutes, group.size());
                })
                .sorted((a, b) -> b.totalMinutes() - a.totalMinutes())
                .toList();
    }
}

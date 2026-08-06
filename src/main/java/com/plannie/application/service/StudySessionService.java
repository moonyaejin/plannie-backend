package com.plannie.application.service;

import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.studysession.StudySession;
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
    private final LoadCategoryPort loadCategoryPort;

    @Override
    @Transactional
    public StudySession start(StartCommand command) {
        // 카테고리 존재(기본 카테고리 포함) + 권한 확인
        loadCategoryPort.findByIdAndUserIdOrDefault(command.categoryId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 이미 진행 중인 세션 확인
        studySessionPort.findActiveByUserId(command.userId()).ifPresent(s -> {
            throw new BusinessException(ErrorCode.STUDY_SESSION_ALREADY_ACTIVE);
        });

        StudySession session = StudySession.builder()
                .userId(command.userId())
                .categoryId(command.categoryId())
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
    public List<CategorySummary> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<StudySession> sessions = studySessionPort.findByUserIdAndDateRange(userId, startDate, endDate);

        // 카테고리 정보 조회 (이름, 색상)
        Map<Long, Category> categoryMap = loadCategoryPort.findAllByUserIdOrDefault(userId).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // categoryId 기준으로 그룹핑 후 합산
        Map<Long, List<StudySession>> grouped = sessions.stream()
                .filter(s -> !s.isActive())
                .collect(Collectors.groupingBy(StudySession::getCategoryId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long categoryId = entry.getKey();
                    List<StudySession> group = entry.getValue();
                    int totalMinutes = group.stream()
                            .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                            .sum();
                    Category category = categoryMap.get(categoryId);
                    String name = category != null ? category.getName() : "삭제된 카테고리";
                    String color = category != null ? category.getColor() : null;
                    return new CategorySummary(categoryId, name, color, totalMinutes, group.size());
                })
                .sorted((a, b) -> b.totalMinutes() - a.totalMinutes())
                .toList();
    }
}

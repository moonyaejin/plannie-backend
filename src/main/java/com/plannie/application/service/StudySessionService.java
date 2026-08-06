package com.plannie.application.service;

import com.plannie.application.port.in.StudySessionUseCase;
import com.plannie.application.port.out.LoadCategoryPort;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.StudySessionPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Category;
import com.plannie.domain.schedule.Schedule;
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
    private final LoadSchedulePort loadSchedulePort;

    @Override
    @Transactional
    public StudySession start(StartCommand command) {
        Long resolvedCategoryId = resolveCategoryId(command);

        // 카테고리 존재(기본 카테고리 포함) + 권한 확인
        loadCategoryPort.findByIdAndUserIdOrDefault(resolvedCategoryId, command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 이미 진행 중인 세션 확인
        studySessionPort.findActiveByUserId(command.userId()).ifPresent(s -> {
            throw new BusinessException(ErrorCode.STUDY_SESSION_ALREADY_ACTIVE);
        });

        StudySession session = StudySession.builder()
                .userId(command.userId())
                .categoryId(resolvedCategoryId)
                .scheduleId(command.scheduleId())
                .startedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();

        return studySessionPort.save(session);
    }

    // scheduleId가 있으면 그 일정의 카테고리를 그대로 쓰고(클라이언트가 보낸 categoryId는 무시),
    // 없으면 클라이언트가 보낸 categoryId를 그대로 사용
    private Long resolveCategoryId(StartCommand command) {
        if (command.scheduleId() == null) {
            if (command.categoryId() == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return command.categoryId();
        }

        Schedule schedule = loadSchedulePort.findByIdAndUserId(command.scheduleId(), command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.SCHEDULE_CATEGORY_REQUIRED);
        }

        return schedule.getCategoryId();
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

    @Override
    public List<ScheduleSummary> getScheduleSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<StudySession> sessions = studySessionPort.findByUserIdAndDateRange(userId, startDate, endDate);

        // 일정 단위로 재기록된(scheduleId가 있는) 세션만 대상 — 카테고리 직접 타이머는 제외
        Map<Long, List<StudySession>> grouped = sessions.stream()
                .filter(s -> !s.isActive() && s.getScheduleId() != null)
                .collect(Collectors.groupingBy(StudySession::getScheduleId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long scheduleId = entry.getKey();
                    List<StudySession> group = entry.getValue();
                    int totalMinutes = group.stream()
                            .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                            .sum();
                    Optional<Schedule> schedule = loadSchedulePort.findByIdAndUserId(scheduleId, userId);
                    String title = schedule.map(Schedule::getTitle).orElse("삭제된 일정");
                    Long categoryId = schedule.map(Schedule::getCategoryId).orElse(null);
                    return new ScheduleSummary(scheduleId, title, categoryId, totalMinutes, group.size());
                })
                .sorted((a, b) -> b.totalMinutes() - a.totalMinutes())
                .toList();
    }
}

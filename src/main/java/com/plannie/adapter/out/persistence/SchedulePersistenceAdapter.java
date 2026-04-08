package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.ScheduleCompletionEntity;
import com.plannie.adapter.out.persistence.entity.ScheduleExceptionEntity;
import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity;
import com.plannie.adapter.out.persistence.repository.ScheduleCompletionRepository;
import com.plannie.adapter.out.persistence.repository.ScheduleExceptionRepository;
import com.plannie.adapter.out.persistence.repository.ScheduleJpaRepository;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.schedule.Schedule;
import com.plannie.domain.schedule.ScheduleException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Schedule Persistence Adapter
 *
 * 역할:
 * - LoadSchedulePort, SaveSchedulePort 인터페이스 구현
 * - 도메인 계층과 영속성 계층을 연결
 * - JPA Repository를 감싸서 도메인 객체로 변환
 *
 * - Service는 JPA를 몰라도 됨 (Port 인터페이스만 알면 됨)
 * - 나중에 JPA → MongoDB로 바꿔도 이 Adapter만 교체하면 됨
 * - 테스트할 때 이 Adapter를 Mock으로 쉽게 대체 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulePersistenceAdapter implements LoadSchedulePort, SaveSchedulePort {

    private final ScheduleJpaRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final ScheduleCompletionRepository scheduleCompletionRepository;

    // ==================== LoadSchedulePort 구현 ====================

    @Override
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id)
                .map(scheduleMapper::toDomain);
    }

    @Override
    public Optional<Schedule> findByIdAndUserId(Long id, Long userId) {
        return scheduleRepository.findByIdAndUserId(id, userId)
                .map(scheduleMapper::toDomain);
    }

    @Override
    public List<Schedule> findByUserIdAndDate(Long userId, LocalDate date) {
        return scheduleRepository.findByUserIdAndStartDate(userId, date)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Schedule> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Schedule> findConflictingSchedules(Long userId, LocalDate date,
                                                   LocalTime startTime, LocalTime endTime) {
        return scheduleRepository.findConflictingSchedules(userId, date, startTime, endTime, -1L)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Schedule> findByIdWithLock(Long id) {
        return scheduleRepository.findByIdWithLock(id)
                .map(scheduleMapper::toDomain);
    }

    @Override
    public List<Schedule> findRepeatingSchedules(Long userId) {
        return scheduleRepository.findByUserIdAndRepeatTypeNot(userId, ScheduleJpaEntity.RepeatType.NONE)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Schedule> findOneTimeSchedulesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByUserIdAndStartDateBetweenAndRepeatType(
                        userId, startDate, endDate, ScheduleJpaEntity.RepeatType.NONE
                ).stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    // ==================== SaveSchedulePort 구현 ====================

    @Override
    public Schedule save(Schedule schedule) {
        if (schedule.getId() != null) {
            ScheduleJpaEntity existingEntity = scheduleRepository
                    .findById(schedule.getId())
                    .orElseThrow(EntityNotFoundException::new);

            existingEntity.update(
                    schedule.getTitle(),
                    schedule.getMemo(),
                    schedule.getStartDate(),
                    schedule.getEndDate(),
                    schedule.getStartTime() != null ? schedule.getStartTime() : LocalTime.of(0, 0),
                    schedule.getEndTime() != null ? schedule.getEndTime() : LocalTime.of(23, 59),
                    schedule.getCategoryId()
            );

            return scheduleMapper.toDomain(scheduleRepository.save(existingEntity));
        } else {
            ScheduleJpaEntity entity = scheduleMapper.toEntity(schedule);
            return scheduleMapper.toDomain(scheduleRepository.save(entity));
        }
    }

    @Override
    public void toggleComplete(Long scheduleId) {
        ScheduleJpaEntity entity = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        entity.toggleComplete();
        scheduleRepository.save(entity);
    }

    @Override
    public Map<String, ScheduleException> findExceptions(List<Long> scheduleIds,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        List<ScheduleExceptionEntity> exceptions =
                scheduleExceptionRepository.findByScheduleIdsAndDateRange(scheduleIds, startDate, endDate);

        Map<String, ScheduleException> map = new HashMap<>();
        for (ScheduleExceptionEntity entity : exceptions) {
            String key = entity.getScheduleId() + "_" + entity.getExceptionDate();
            map.put(key, scheduleMapper.toDomain(entity));
        }
        return map;
    }

    @Override
    public Map<String, Boolean> findCompletions(List<Long> scheduleIds,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        List<ScheduleCompletionEntity> completions =
                scheduleCompletionRepository.findByScheduleIdsAndDateRange(scheduleIds, startDate, endDate);

        Map<String, Boolean> map = new HashMap<>();
        for (ScheduleCompletionEntity entity : completions) {
            String key = entity.getScheduleId() + "_" + entity.getCompletionDate();
            map.put(key, entity.isCompleted());
        }
        return map;
    }

    @Transactional
    public void toggleCompletion(Long scheduleId, LocalDate date) {
        try {
            ScheduleCompletionEntity entity = ScheduleCompletionEntity.builder()
                    .scheduleId(scheduleId)
                    .completionDate(date)
                    .completed(true)
                    .build();
            scheduleCompletionRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            ScheduleCompletionEntity existing = scheduleCompletionRepository
                    .findByScheduleIdAndCompletionDate(scheduleId, date)
                    .orElseThrow();
            existing.toggleComplete();
            scheduleCompletionRepository.save(existing);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(Long scheduleId) {
        try {
            scheduleRepository.deleteById(scheduleId);
            scheduleRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("Schedule {} was already deleted by another transaction", scheduleId);
        }
    }
}

package com.plannie.adapter.out.persistence;

import com.plannie.adapter.out.persistence.entity.ScheduleJpaEntity;
import com.plannie.adapter.out.persistence.repository.ScheduleJpaRepository;
import com.plannie.application.port.out.LoadSchedulePort;
import com.plannie.application.port.out.SaveSchedulePort;
import com.plannie.domain.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
@Component
@RequiredArgsConstructor  // final 필드 생성자 자동 생성 (Lombok)
public class SchedulePersistenceAdapter implements LoadSchedulePort, SaveSchedulePort {

    private final ScheduleJpaRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    // ==================== LoadSchedulePort 구현 ====================

    @Override
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id)
                .map(scheduleMapper::toDomain);  // Entity → Domain 변환
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

    /**
     * 충돌하는 일정 조회
     * - 같은 날짜, 시간이 겹치는 일정들을 찾음
     * - 일정 생성/수정 시 중복 체크에 사용
     */
    @Override
    public List<Schedule> findConflictingSchedules(Long userId, LocalDate date,
                                                   LocalTime startTime, LocalTime endTime) {
        // excludeId를 -1로 설정 (새로운 일정 생성 시)
        return scheduleRepository.findConflictingSchedules(userId, date, startTime, endTime, -1L)
                .stream()
                .map(scheduleMapper::toDomain)
                .toList();
    }

    /**
     * 비관적 락을 사용한 조회
     * - 동시에 같은 일정을 수정하려 할 때 데이터 정합성 보장
     * - SELECT ... FOR UPDATE 쿼리 실행
     */
    @Override
    public Optional<Schedule> findByIdWithLock(Long id) {
        return scheduleRepository.findByIdWithLock(id)
                .map(scheduleMapper::toDomain);
    }

    // ==================== SaveSchedulePort 구현 ====================

    @Override
    public Schedule save(Schedule schedule) {
        // 1. 도메인 → Entity 변환
        ScheduleJpaEntity entity = scheduleMapper.toEntity(schedule);

        // 2. DB 저장 (JPA가 INSERT 또는 UPDATE 자동 판단)
        ScheduleJpaEntity savedEntity = scheduleRepository.save(entity);

        // 3. 저장된 Entity → 도메인 변환 (ID가 생성됨)
        return scheduleMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }
}
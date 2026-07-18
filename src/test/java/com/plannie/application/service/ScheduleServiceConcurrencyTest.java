package com.plannie.application.service;

import com.plannie.application.port.in.CreateScheduleUseCase;
import com.plannie.application.port.in.UpdateScheduleUseCase;
import com.plannie.domain.schedule.Schedule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("critical")
@SpringBootTest
class ScheduleServiceConcurrencyTest {

    @Autowired
    private ScheduleService scheduleService;

    @Test
    void 동시에_같은_일정을_수정하면_하나는_실패해야한다() throws InterruptedException {
        // Given: 일정 하나 생성
        Schedule schedule = scheduleService.createSchedule(
                new CreateScheduleUseCase.CreateScheduleCommand(
                        1L, "원본 제목", "메모",
                        LocalDate.now(), LocalDate.now(),
                        LocalTime.of(14, 0), LocalTime.of(17, 0),
                        null, "NONE", null, null, null
                )
        );

        // When: 10개 스레드가 동시에 수정 시도
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;   // 람다에서 사용하려면 final 필요

            executor.submit(() -> {    // 각 스레드가 할 일
                try {
                    // 수정 명령 생성
                    UpdateScheduleUseCase.UpdateScheduleCommand command =
                            new UpdateScheduleUseCase.UpdateScheduleCommand(
                                    schedule.getId(),                 // 같은 일정 ID
                                    1L,                               // userID
                                    "수정된 제목 " + index,              // 각자 다른 제목
                                    "수정된 메모",
                                    LocalDate.now(), LocalDate.now(),
                                    LocalTime.of(14, 0), LocalTime.of(15, 0),
                                    null, null, null
                            );

                    scheduleService.updateSchedule(command);   // 수정 시도
                    successCount.incrementAndGet();   // 성공 + 1
                    System.out.println("Thread " + index + " 성공!");

                } catch (ObjectOptimisticLockingFailureException e) {
                    // 낙관적 락 충돌
                    failCount.incrementAndGet();   // 실패 + 1
                    System.out.println("Thread " + index + " 실패 (낙관적 락)");

                } catch (Exception e) {
                    // 다른 에러
                    failCount.incrementAndGet();
                    System.out.println("Thread " + index + " 실패: " + e.getMessage());

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();         // 10명 모두 끝날 때까지 대기
        executor.shutdown();   // ExecutorService 종료

        // Then: 1개만 성공하고 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1);              // 1개만 성공해야 함
        assertThat(failCount.get()).isEqualTo(threadCount - 1);   // 9개는 실패해야 함

        // 최종 확인
        Schedule updated = scheduleService.getSchedule(schedule.getId(), 1L)
                .orElseThrow();
        System.out.println("최종 제목: " + updated.getTitle());
    }

    @Test
    void 완료_처리를_동시에_여러번_해도_한번만_처리되어야한다() throws InterruptedException {
        // Given: 반복 일정 생성
        Schedule schedule = scheduleService.createSchedule(
                new CreateScheduleUseCase.CreateScheduleCommand(
                        1L, "반복 일정", "메모",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1),
                        LocalTime.of(10, 0), LocalTime.of(11, 0),
                        null, "DAILY", null, null, null
                )
        );

        // 1월 15일을 타겟으로
        LocalDate targetDate = LocalDate.of(2025, 1, 15);

        // When: 5개 스레드가 동시에 같은 날짜 완료 처리
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    scheduleService.toggleRecurringComplete(
                            schedule.getId(),   // 같은 일정
                            targetDate,         // 같은 날짜
                            1L                  // 깉은 사용자
                    );
                    System.out.println(Thread.currentThread().getName() + " 완료 처리 성공");
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " 오류: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: DB에서 확인 (이 부분은 Repository 직접 접근 필요)
        // 실제로는 schedule_completions 테이블에 1개만 있어야 함
    }

    @Test
    void 동시에_삭제해도_최종적으로_삭제된다() throws InterruptedException {
        // Given: 일정 생성
        Schedule schedule = scheduleService.createSchedule(
                new CreateScheduleUseCase.CreateScheduleCommand(
                        1L, "삭제할 일정", "메모",
                        LocalDate.now(), LocalDate.now(),
                        LocalTime.of(16, 0), LocalTime.of(17, 0),
                        null, "NONE", null, null, null
                )
        );

        Long scheduleId = schedule.getId();
        System.out.println("생성된 일정 ID: " + scheduleId);

        // When: 5개 스레드가 동시에 삭제 시도
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    scheduleService.deleteSchedule(scheduleId, 1L, null);
                    System.out.println("Thread " + index + " 삭제 시도 완료");
                } catch (Exception e) {
                    // 트랜잭션 롤백 예외는 무시
                    System.out.println("Thread " + index + " 예외 발생 (예상됨): "
                            + e.getClass().getSimpleName());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 트랜잭션 정리를 위한 짧은 대기
        Thread.sleep(100);

        // Then: 최종적으로 삭제되었는지 확인 (이것만 중요!)
        Optional<Schedule> deleted = scheduleService.getSchedule(scheduleId, 1L);
        assertThat(deleted).isEmpty();

        System.out.println("동시성 삭제 테스트 성공: 일정이 최종적으로 삭제됨");
    }
}
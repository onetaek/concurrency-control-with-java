package io.hhplus.tdd.point.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

@SpringBootTest
public class PointServiceIntegrationTest {
	@Autowired
	private PointService pointService;
	@Autowired
	private PointHistoryTable pointHistoryTable;
	@Autowired
	private UserPointTable userPointTable;

	private static Long incrementId = 0L;

	@Test
	@DisplayName("동시성 테스트 - 한명의 사용자가 충전과 사용 섞임")
	void concurrentMixedChargeAndUseTest() throws InterruptedException {
		// given
		Long id1 = ++incrementId;
		Random random = new Random();

		int countOfThreads = 50; // 요청할 스레드 개수
		ExecutorService executorService = Executors.newFixedThreadPool(countOfThreads);
		CountDownLatch countDownLatch = new CountDownLatch(countOfThreads);

		AtomicInteger successCount = new AtomicInteger(); // 성공 횟수
		AtomicInteger failCount = new AtomicInteger(); // 실패 횟수

		// when
		for (int i = 0; i < countOfThreads; i++) {
			executorService.execute(() -> {
				try {
					long amount = random.nextInt(1000) + 1; // 1 ~ 1000 사이의 랜덤 포인트
					if (random.nextBoolean()) {
						pointService.charge(id1, amount); // 무작위로 충전
					} else {
						pointService.use(id1, amount);    // 무작위로 사용
					}
					successCount.incrementAndGet();
				} catch (IllegalArgumentException e) {
					failCount.incrementAndGet(); // 사용 실패 시 예외 발생
				} finally {
					countDownLatch.countDown();
				}
			});
		}
		countDownLatch.await(); // 모든 스레드가 완료될 때까지 대기
		executorService.shutdown();

		// then
		UserPoint userPoint = userPointTable.selectById(id1);
		List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(id1);

		// 충전 포인트 합계
		long totalCharged = pointHistoryList.stream()
			.filter(history -> history.type() == TransactionType.CHARGE)
			.mapToLong(PointHistory::amount)
			.sum();

		// 사용 포인트 합계
		long totalUsed = pointHistoryList.stream()
			.filter(history -> history.type() == TransactionType.USE)
			.mapToLong(PointHistory::amount)
			.sum();

		// (최종 포인트 == 충전 포인트 합계 - 사용 포인트 합계) 검증
		assertThat(userPoint.point()).isEqualTo(totalCharged - totalUsed);
		assertThat(pointHistoryList.size()).isEqualTo(successCount.get());
	}

	@Test
	@DisplayName("동시성 테스트 - 여러 사용자가 랜덤으로 포인트 충전 또는 사용 시도")
	void concurrentChargeAndUsePointsByMultipleUsersRandomized() throws InterruptedException {
		// given
		int numberOfUsers = 20;
		int numberOfThreadsPerUser = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers * numberOfThreadsPerUser);
		CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers * numberOfThreadsPerUser);
		long defaultPoint = 300L;

		// 사용자별 초기 포인트 설정
		List<Long> userIds = new ArrayList<>();
		for (int i = 0; i < numberOfUsers; i++) {
			Long userId = ++incrementId;
			userPointTable.insertOrUpdate(userId, defaultPoint); // 각 사용자에게 초기 포인트 할당
			userIds.add(userId);
		}

		Random random = new Random(); // 사용자 선택과 포인트, 작업을 위한 랜덤 생성기
		AtomicInteger successCount = new AtomicInteger(); // 성공 횟수

		// when
		for (int i = 0; i < numberOfThreadsPerUser * numberOfUsers; i++) {
			executorService.execute(() -> {
				try {
					// 랜덤한 사용자를 선택
					Long userId = userIds.get(random.nextInt(userIds.size()));

					// 랜덤하게 충전 또는 사용 선택 (true: 충전, false: 사용)
					boolean isCharge = random.nextBoolean();

					// 랜덤한 포인트 값 (1 ~ 100 사이의 포인트)
					long amount = random.nextInt(100) + 1;

					if (isCharge) {
						// 랜덤 충전
						pointService.charge(userId, amount);
					} else {
						// 랜덤 사용
						pointService.use(userId, amount);
					}

					successCount.incrementAndGet();
				} catch (IllegalArgumentException e) {
					// 실패 시 아무 것도 하지 않음
				} finally {
					countDownLatch.countDown();
				}
			});
		}

		countDownLatch.await(); // 모든 스레드가 완료될 때까지 대기
		executorService.shutdown();

		// then
		for (Long userId : userIds) {
			UserPoint userPoint = userPointTable.selectById(userId);
			List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);

			// 충전 포인트 합계 계산
			long totalCharged = pointHistoryList.stream()
				.filter(history -> history.type() == TransactionType.CHARGE)
				.mapToLong(PointHistory::amount)
				.sum();

			// 사용 포인트 합계 계산
			long totalUsed = pointHistoryList.stream()
				.filter(history -> history.type() == TransactionType.USE)
				.mapToLong(PointHistory::amount)
				.sum();

			// 각 사용자의 최종 포인트 검증 (기존 포인트 + 충전 포인트 합계 - 사용 포인트 합계)
			assertThat(userPoint.point()).isEqualTo(defaultPoint + totalCharged - totalUsed);
		}
	}
}

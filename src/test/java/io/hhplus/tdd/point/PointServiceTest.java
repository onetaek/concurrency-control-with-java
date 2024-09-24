package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;

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

@SpringBootTest
class PointServiceTest {

	@Autowired
	private PointService pointService;
	@Autowired
	private PointHistoryTable pointHistoryTable;
	@Autowired
	private UserPointTable userPointTable;

	private static Long incrementId = 0L;

	@Test
	@DisplayName("특정 사용자의 포인트를 조회한다.")
	void findPointById() {
		//given
		Long id1 = ++incrementId;
		Long id2 = ++incrementId;
		Long id3 = ++incrementId;
		userPointTable.insertOrUpdate(id1, 100);
		userPointTable.insertOrUpdate(id2, 400);
		userPointTable.insertOrUpdate(id3, 300);

		//when
		UserPoint userPoint = pointService.findPointById(id2);

		//then
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id2, 400L);
	}

	@Test
	@DisplayName("특정 사용자의 포인트이력를 조회한다.")
	void findPointHistoryAllById() {
		//given
		Long id1 = ++incrementId;
		Long id2 = ++incrementId;
		UserPoint userPoint1 = userPointTable.insertOrUpdate(id1, 100);
		UserPoint userPoint2 = userPointTable.insertOrUpdate(id2, 400);
		pointHistoryTable.insert(userPoint1.id(), 300, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(userPoint1.id(), 200, TransactionType.USE, System.currentTimeMillis());
		pointHistoryTable.insert(userPoint1.id(), 500, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(userPoint2.id(), 700, TransactionType.CHARGE, System.currentTimeMillis());

		//when
		List<PointHistory> pointHistoryList = pointService.findPointHistoryAllById(userPoint1.id());

		//then
		assertThat(pointHistoryList)
			.extracting("userId", "amount", "type")
			.containsExactlyInAnyOrder(
				tuple(id1, 300L, TransactionType.CHARGE),
				tuple(id1, 200L, TransactionType.USE),
				tuple(id1, 500L, TransactionType.CHARGE)
			);
	}

	@Test
	@DisplayName("500, 300을 충전하면 800이 남는다.")
	void chargePointTransactionTest() {
		//when
		Long id1 = ++incrementId;
		pointService.charge(id1, 500);
		pointService.charge(id1, 300);

		//then
		UserPoint userPoint = userPointTable.selectById(id1);
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id1, 800L);

		List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(id1);
		assertThat(pointHistoryList)
			.extracting("userId", "amount", "type")
			.containsExactlyInAnyOrder(
				tuple(id1, 500L, TransactionType.CHARGE),
				tuple(id1, 300L, TransactionType.CHARGE)
			);
	}

	@Test
	@DisplayName("500을 충전하고 200을 사용하면 300이 남는다.")
	void usePointTransactionTest() {
		//when
		Long id1 = ++incrementId;
		pointService.charge(id1, 500); // 500 포인트 충전
		pointService.use(id1, 200);    // 200 포인트 사용

		//then
		UserPoint userPoint = userPointTable.selectById(id1);
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id1, 300L);

		List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(id1);
		assertThat(pointHistoryList)
			.extracting("userId", "amount", "type")
			.containsExactlyInAnyOrder(
				tuple(id1, 500L, TransactionType.CHARGE),
				tuple(id1, 200L, TransactionType.USE)
			);
	}

	@Test
	@DisplayName("동시성 테스트")
	void concurrentChargeAndUseTest() throws InterruptedException {
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
					long amount = random.nextInt(500) + 1; // 1 ~ 500 사이의 랜덤 포인트
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
		System.out.println("totalCharged = " + totalCharged);

		// 사용 포인트 합계
		long totalUsed = pointHistoryList.stream()
			.filter(history -> history.type() == TransactionType.USE)
			.mapToLong(PointHistory::amount)
			.sum();
		System.out.println("totalUsed = " + totalUsed);

		// (최종 포인트 == 충전 포인트 합계 - 사용 포인트 합계) 검증
		assertThat(userPoint.point()).isEqualTo(totalCharged - totalUsed);
		System.out.println("Final userPoint = " + userPoint);

		// (포인트 이력 리스트의 크기 == 성공 횟수) 검증
		assertThat(pointHistoryList.size()).isEqualTo(successCount.get());
	}

}
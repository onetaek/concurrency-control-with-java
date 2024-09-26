package io.hhplus.tdd.point.unit;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointHistoryRepository;
import io.hhplus.tdd.point.PointHistoryRepositoryImpl;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.PointServiceImpl;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.UserPointRepository;
import io.hhplus.tdd.point.UserPointRepositoryImpl;

class PointServiceUnitTest {

	private UserPointTable userPointTable;
	private PointHistoryTable pointHistoryTable;
	private UserPointRepository userPointRepository;
	private PointHistoryRepository pointHistoryRepository;
	private PointService pointService;

	@BeforeEach
	void setUp() {
		// 실제 구현체를 사용하여 의존성 주입
		this.userPointTable = new UserPointTable();  // 실제 UserPointTable 사용
		this.pointHistoryTable = new PointHistoryTable();  // 실제 PointHistoryTable 사용
		this.userPointRepository = new UserPointRepositoryImpl(userPointTable);  // 실제 구현체
		this.pointHistoryRepository = new PointHistoryRepositoryImpl(pointHistoryTable);  // 실제 구현체
		this.pointService = new PointServiceImpl(userPointRepository, pointHistoryRepository);  // 서비스에 구현체 주입
	}

	private static Long incrementId = 0L;

	@Test
	@DisplayName("특정 사용자의 포인트를 조회한다.")
	void findPointById() {
		// given
		Long id2 = ++incrementId;
		userPointTable.insertOrUpdate(id2, 400);  // 실제 데이터 삽입

		// when
		UserPoint userPoint = pointService.findPointById(id2);

		// then
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id2, 400L);  // 포인트 값 검증
	}

	@Test
	@DisplayName("특정 사용자의 포인트 이력을 조회한다.")
	void findPointHistoryAllById() {
		// given
		Long id1 = ++incrementId;

		// 실제 데이터 삽입
		pointHistoryTable.insert(id1, 300, TransactionType.CHARGE, System.currentTimeMillis());
		pointHistoryTable.insert(id1, 200, TransactionType.USE, System.currentTimeMillis());
		pointHistoryTable.insert(id1, 500, TransactionType.CHARGE, System.currentTimeMillis());

		// when
		List<PointHistory> pointHistoryList = pointService.findPointHistoryAllById(id1);

		// then
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
		// given
		Long id1 = ++incrementId;

		// 초기 포인트 설정
		userPointTable.insertOrUpdate(id1, 0);

		// when
		pointService.charge(id1, 500);  // 500 충전
		pointService.charge(id1, 300);  // 300 충전

		// then
		UserPoint userPoint = pointService.findPointById(id1);
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id1, 800L);  // 포인트가 800으로 되어있는지 확인
	}

	@Test
	@DisplayName("500을 충전하고 200을 사용하면 300이 남는다.")
	void usePointTransactionTest() {
		// given
		Long id1 = ++incrementId;

		// 초기 포인트 설정
		userPointTable.insertOrUpdate(id1, 500);

		// when
		pointService.use(id1, 200);  // 200 사용

		// then
		UserPoint userPoint = pointService.findPointById(id1);
		assertThat(userPoint)
			.extracting("id", "point")
			.contains(id1, 300L);  // 500 - 200 = 300 확인
	}

	@Test
	@DisplayName("포인트를 초과하여 사용할 때 예외 발생")
	void usePointExceedingLimit() {
		// given
		Long id1 = ++incrementId;

		// 초기 포인트 설정
		userPointTable.insertOrUpdate(id1, 100);

		// when // then
		assertThatThrownBy(() -> pointService.use(id1, 200))  // 200 포인트 사용 시도
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("사용 가능한 포인트가 부족합니다.");
	}

	@Test
	@DisplayName("충전 후 포인트가 최대값을 초과할 때 예외 발생")
	void chargePointExceedingMaximum() {
		// given
		Long id1 = ++incrementId;

		// 초기 포인트 설정
		userPointTable.insertOrUpdate(id1, 900);  // 900 포인트 보유

		// when // then
		assertThatThrownBy(() -> pointService.charge(id1, 200))  // 200 충전 시도
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("충전 후 포인트가 최대 허용 값을 초과했습니다.");
	}
}
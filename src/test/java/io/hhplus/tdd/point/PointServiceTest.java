package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

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
	void handlePointTransactionWhenCharge() {
		//when
		Long id1 = ++incrementId;
		pointService.handlePoint(id1, 500, TransactionType.CHARGE);
		pointService.handlePoint(id1, 300, TransactionType.CHARGE);

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
	void handlePointTransactionWhenUse() {
		//when
		Long id1 = ++incrementId;
		pointService.handlePoint(id1, 500, TransactionType.CHARGE);
		pointService.handlePoint(id1, 200, TransactionType.USE);

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
}
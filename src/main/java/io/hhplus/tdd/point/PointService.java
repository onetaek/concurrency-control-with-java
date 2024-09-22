package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private static final long MAXIMUM_POINT = 1000L; // 최대 포인트

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	public UserPoint findPointById(long id) {
		return userPointTable.selectById(id);
	}

	public List<PointHistory> findPointHistoryAllById(long id) {
		return pointHistoryTable.selectAllByUserId(id);
	}

	// 충전, 사용 공통 로직 메서드
	public synchronized UserPoint handlePoint(
		long id,
		long amount,
		TransactionType transactionType
	) {
		// CHARGE(충전) 유형일 경우 -> 이전 값 + amount
		// 사용(USE) 유형일 경우 -> 이전 값 - amount
		UserPoint prevUserPoint = userPointTable.selectById(id);
		long prevPoint = prevUserPoint.point();
		long newPoint = transactionType == TransactionType.CHARGE
			? prevPoint + amount
			: prevPoint - amount;

		// 유효성 검사
		UserPoint.validate(amount, prevPoint, newPoint, transactionType, MAXIMUM_POINT);

		// 저장소에 insert
		UserPoint userPoint = userPointTable.insertOrUpdate(id, newPoint);
		pointHistoryTable.insert(id, amount, transactionType, userPoint.updateMillis());
		return userPoint;
	}
}

package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final UserPointTable userPointTable;
	private final PointHistoryTable pointHistoryTable;

	public UserPoint findPointById(long id) {
		return userPointTable.selectById(id);
	}

	public List<PointHistory> findPointHistoryAllById(long id) {
		return pointHistoryTable.selectAllByUserId(id);
	}

	public UserPoint charge(long id, long amount) {
		UserPoint prevUserPoint = userPointTable.selectById(id);
		long prevPoint = prevUserPoint.point();
		long newPoint = prevPoint + amount;
		UserPoint userPoint = userPointTable.insertOrUpdate(id, newPoint);
		pointHistoryTable.insert(id, amount, TransactionType.CHARGE, userPoint.updateMillis());
		return userPoint;
	}

	public UserPoint use(long id, long amount) {
		UserPoint prevUserPoint = userPointTable.selectById(id);
		long prevPoint = prevUserPoint.point();
		long newPoint = prevPoint - amount;
		UserPoint userPoint = userPointTable.insertOrUpdate(id, newPoint);
		pointHistoryTable.insert(id, amount, TransactionType.USE, userPoint.updateMillis());
		return userPoint;
	}
}

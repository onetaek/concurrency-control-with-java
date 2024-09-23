package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.PointHistoryTable;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

	private final PointHistoryTable pointHistoryTable;

	@Override
	public PointHistory save(PointHistory pointHistory) {
		return pointHistoryTable.insert(
			pointHistory.userId(),
			pointHistory.amount(),
			pointHistory.type(),
			pointHistory.updateMillis()
		);
	}

	@Override
	public List<PointHistory> selectAllByUserId(long userId) {
		return pointHistoryTable.selectAllByUserId(userId);
	}
}

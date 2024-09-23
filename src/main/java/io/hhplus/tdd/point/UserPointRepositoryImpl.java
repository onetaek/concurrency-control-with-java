package io.hhplus.tdd.point;

import org.springframework.stereotype.Repository;

import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

	private final UserPointTable userPointTable;

	@Override
	public UserPoint save(UserPoint userPoint) {
		return userPointTable.insertOrUpdate(
			userPoint.id(),
			userPoint.point()
		);
	}

	@Override
	public UserPoint selectById(long id) {
		return userPointTable.selectById(id);
	}
}

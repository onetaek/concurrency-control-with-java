package io.hhplus.tdd.point.unit;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.UserPointRepository;
import io.hhplus.tdd.point.UserPointRepositoryImpl;

class UserPointRepositoryUnitTest {

	private UserPointTable userPointTable;
	private UserPointRepository userPointRepository;

	@BeforeEach
	void setUp() {
		this.userPointTable = new UserPointTable();
		this.userPointRepository = new UserPointRepositoryImpl(userPointTable);
	}

	@Test
	@DisplayName("사용자 포인트 조회 테스트 - 없는 사용자일 경우")
	void selectById_userNotFound() {
		// given
		long userId = 1L;

		// when
		UserPoint result = userPointRepository.selectById(userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.point()).isEqualTo(0);
		assertThat(result.id()).isEqualTo(userId);
	}

	@Test
	@DisplayName("사용자 포인트 조회 테스트 - 있는 사용자일 경우")
	void selectById_userExists() {
		// given
		long userId = 1L;
		userPointTable.insertOrUpdate(userId, 500L);

		// when
		UserPoint result = userPointRepository.selectById(userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.point()).isEqualTo(500L);
		assertThat(result.id()).isEqualTo(userId);
	}

	@Test
	@DisplayName("사용자 포인트 저장 테스트")
	void saveUserPoint() {
		// given
		long userId = 1L;
		UserPoint userPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

		// when
		UserPoint result = userPointRepository.save(userPoint);

		// then
		assertThat(result).isNotNull();
		assertThat(result.point()).isEqualTo(1000L);
		assertThat(result.id()).isEqualTo(userId);

		// 저장한 포인트를 다시 조회하여 확인
		UserPoint savedPoint = userPointRepository.selectById(userId);
		assertThat(savedPoint.point()).isEqualTo(1000L);
	}
}

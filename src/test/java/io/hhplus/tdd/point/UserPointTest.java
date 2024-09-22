package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserPointTest {

	@Test
	@DisplayName("amount 값이 0이면 예외발생")
	void validateWhenAmountIs0() {
		// given
		var amount = 0L;
		var prevPoint = 0L;
		var newPoint = 0L;
		var transactionType = TransactionType.CHARGE;
		var maximumPoint = 0L;

		// when // then
		assertThatThrownBy(
			() -> UserPoint.validate(amount, prevPoint, newPoint, transactionType, maximumPoint)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("값이 양수이어야 합니다.");
	}

	@Test
	@DisplayName("amount 값이 음수이면 예외발생")
	void validateWhenAmountIsMinus() {
		// given
		var amount = -2L;
		var prevPoint = 0L;
		var newPoint = 0L;
		var transactionType = TransactionType.CHARGE;
		var maximumPoint = 0L;

		// when // then
		assertThatThrownBy(
			() -> UserPoint.validate(amount, prevPoint, newPoint, transactionType, maximumPoint)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("값이 양수이어야 합니다.");
	}

	@Test
	@DisplayName("충전시 새로운 값이 최대 값을 넘어가면 예외발생")
	void validateWhenExceed() {
		// given
		var amount = 10L;
		var prevPoint = 5L;
		var newPoint = 15L;
		var transactionType = TransactionType.CHARGE;
		var maximumPoint = 13L;

		// when // then
		assertThatThrownBy(
			() -> UserPoint.validate(amount, prevPoint, newPoint, transactionType, maximumPoint)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("충전 후 포인트가 최대 허용 값을 초과했습니다.");
	}

	@Test
	@DisplayName("사용시 포인트가 부족하지 않으면 통과")
	void validatePassWhenLimit() {
		// given
		var amount = 10L;// 사용하려는 값
		var prevPoint = 10L;// 보유한 포인트
		var newPoint = 0L;
		var transactionType = TransactionType.USE;
		var maximumPoint = 13L;

		// when // then
		assertDoesNotThrow(
			() -> UserPoint.validate(amount, prevPoint, newPoint, transactionType, maximumPoint)
		);
	}

	@Test
	@DisplayName("사용시 포인트가 부족하면 예외발생")
	void validateWhenLimit() {
		// given
		var amount = 12L;// 사용하려는 값
		var prevPoint = 10L;// 보유한 포인트
		var newPoint = -2L;
		var transactionType = TransactionType.USE;
		var maximumPoint = 13L;

		// when // then
		assertThatThrownBy(
			() -> UserPoint.validate(amount, prevPoint, newPoint, transactionType, maximumPoint)
		)
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("사용 가능한 포인트가 부족합니다.");
	}
}
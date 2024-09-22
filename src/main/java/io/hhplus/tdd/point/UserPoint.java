package io.hhplus.tdd.point;

public record UserPoint(
	long id,
	long point,
	long updateMillis
) {

	public static UserPoint empty(long id) {
		return new UserPoint(id, 0, System.currentTimeMillis());
	}

	public static void validate(
		long amount,
		long prevPoint,
		long newPoint,
		TransactionType transactionType,
		long maximumPoint
	) {

		if (amount <= 0) {
			throw new IllegalArgumentException("값이 양수이어야 합니다.");
		}

		if (transactionType == TransactionType.CHARGE && newPoint > maximumPoint) {
			throw new IllegalArgumentException("충전 후 포인트가 최대 허용 값을 초과했습니다.");
		}

		if (transactionType == TransactionType.USE && prevPoint < amount) {
			throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다.");
		}
	}
}

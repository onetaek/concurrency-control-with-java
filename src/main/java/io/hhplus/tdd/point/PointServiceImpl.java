package io.hhplus.tdd.point;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
	private static final long MAXIMUM_POINT = 1000L; // 최대 포인트
	private static final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

	private final UserPointRepository userPointRepository;
	private final PointHistoryRepository pointHistoryRepository;

	public UserPoint findPointById(long id) {
		return userPointRepository.selectById(id);
	}

	public List<PointHistory> findPointHistoryAllById(long id) {
		return pointHistoryRepository.selectAllByUserId(id);
	}

	public UserPoint charge(long id, long amount) {
		Lock lock = getLockForUser(id);
		lock.lock();
		try {
			UserPoint userPoint = userPointRepository.selectById(id);
			long prevPoint = userPoint.point();
			userPoint = userPoint.charge(amount);

			UserPoint.validate(amount, prevPoint, userPoint.point(), TransactionType.CHARGE, MAXIMUM_POINT);

			userPointRepository.save(userPoint);
			pointHistoryRepository.save(
				new PointHistory(0, id, amount, TransactionType.CHARGE, System.currentTimeMillis())
			);

			return userPoint;
		} finally {
			lock.unlock();
		}
	}

	public UserPoint use(long id, long amount) {
		Lock lock = getLockForUser(id);
		lock.lock();
		try {
			UserPoint userPoint = userPointRepository.selectById(id);
			long prevPoint = userPoint.point();
			userPoint = userPoint.use(amount);

			UserPoint.validate(amount, prevPoint, userPoint.point(), TransactionType.USE, MAXIMUM_POINT);

			userPointRepository.save(userPoint);
			pointHistoryRepository.save(
				new PointHistory(0, id, amount, TransactionType.USE, System.currentTimeMillis())
			);

			return userPoint;
		} finally {
			lock.unlock();
		}
	}

	private Lock getLockForUser(long id) {
		return userLocks.computeIfAbsent(id, key -> new ReentrantLock());
	}
}

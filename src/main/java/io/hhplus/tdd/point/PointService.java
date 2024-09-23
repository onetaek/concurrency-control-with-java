package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {

	UserPoint findPointById(long id);

	List<PointHistory> findPointHistoryAllById(long id);

	UserPoint charge(long id, long amount);

	UserPoint use(long id, long amount);
}

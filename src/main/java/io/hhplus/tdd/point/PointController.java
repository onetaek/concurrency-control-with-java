package io.hhplus.tdd.point;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/point")
public class PointController {

	private static final Logger log = LoggerFactory.getLogger(PointController.class);

	private final PointService pointService;

	@GetMapping("{id}")
	public UserPoint point(
		@PathVariable(name = "id") long id
	) {
		return pointService.findPointById(id);
	}

	@GetMapping("{id}/histories")
	public List<PointHistory> history(
		@PathVariable(name = "id") long id
	) {
		return pointService.findPointHistoryAllById(id);
	}

	@PatchMapping("{id}/charge")
	public UserPoint charge(
		@PathVariable(name = "id") long id,
		@RequestBody long amount
	) {
		return pointService.handlePoint(id, amount, TransactionType.CHARGE);
	}

	@PatchMapping("{id}/use")
	public UserPoint use(
		@PathVariable(name = "id") long id,
		@RequestBody long amount
	) {
		return pointService.handlePoint(id, amount, TransactionType.USE);
	}
}

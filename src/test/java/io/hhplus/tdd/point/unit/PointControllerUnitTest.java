package io.hhplus.tdd.point.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.hhplus.tdd.point.PointController;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

class PointControllerUnitTest {

	private MockMvc mockMvc;

	@Mock
	private PointService pointService;

	@InjectMocks
	private PointController pointController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);  // Mockito 초기화
		mockMvc = MockMvcBuilders.standaloneSetup(pointController).build();  // MockMvc 설정
	}

	@Test
	@DisplayName("특정 사용자의 포인트를 조회한다.")
	void findPointById() throws Exception {
		// given
		long userId = 1L;
		when(pointService.findPointById(anyLong())).thenReturn(new UserPoint(userId, 500L, System.currentTimeMillis()));

		// when // then
		mockMvc.perform(get("/point/{id}", userId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(500L));
	}

	@Test
	@DisplayName("특정 사용자의 포인트 이력을 조회한다.")
	void findPointHistoryAllById() throws Exception {
		// given
		long userId = 1L;
		when(pointService.findPointHistoryAllById(anyLong())).thenReturn(List.of(
			new PointHistory(1L, userId, 300L, TransactionType.CHARGE, System.currentTimeMillis()),
			new PointHistory(2L, userId, 200L, TransactionType.USE, System.currentTimeMillis())
		));

		// when // then
		mockMvc.perform(get("/point/{id}/histories", userId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].userId").value(userId))
			.andExpect(jsonPath("$[0].amount").value(300L))
			.andExpect(jsonPath("$[0].type").value("CHARGE"))
			.andExpect(jsonPath("$[1].userId").value(userId))
			.andExpect(jsonPath("$[1].amount").value(200L))
			.andExpect(jsonPath("$[1].type").value("USE"));
	}

	@Test
	@DisplayName("특정 사용자의 포인트를 충전한다.")
	void chargePoint() throws Exception {
		// given
		long userId = 1L;
		long amount = 500L;
		when(pointService.charge(anyLong(), anyLong())).thenReturn(
			new UserPoint(userId, 500L, System.currentTimeMillis()));

		// when // then
		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(500L));
	}

	@Test
	@DisplayName("특정 사용자의 포인트를 사용한다.")
	void usePoint() throws Exception {
		// given
		long userId = 1L;
		long amount = 200L;
		when(pointService.use(anyLong(), anyLong())).thenReturn(
			new UserPoint(userId, 300L, System.currentTimeMillis()));

		// when // then
		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(300L));
	}
}

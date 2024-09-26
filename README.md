# 동시성 제어 방식 분석 및 구현

## 프로젝트 개요

이 프로젝트는 여러 사용자가 동시에 포인트를 **충전**하거나 **사용**하는 상황에서 발생할 수 있는 **동시성 문제**를 해결하기 위한 방법을 구현하고 검증하는 과제입니다. 각 사용자가 포인트 충전과 사용 요청을
동시에 수행할 때 **데이터 일관성**을 보장하고, **경합 조건(Race Condition)**을 방지하기 위해 적절한 동시성 제어가 필요합니다.

## 목차

1. [동시성 문제 정의](#동시성-문제-정의)
2. [해결 방법](#해결-방법)
    - ReentrantLock을 사용한 사용자별 동시성 제어
    - ConcurrentHashMap을 이용한 Lock 관리
3. [구현 세부 사항](#구현-세부-사항)
    - PointService 구현
    - 테스트 코드 및 검증 방법
4. [성능 최적화](#성능-최적화)
5. [결론](#결론)

---

## 동시성 문제 정의

### 1. 문제 상황

여러 사용자가 동시에 포인트를 충전하거나 사용할 때, 각 사용자의 포인트가 정확하게 계산되어야 합니다. 하지만 동시에 여러 요청이 처리되는 과정에서 **경합 조건(Race Condition)**이 발생할 수
있습니다. 특히, 다음과 같은 문제가 발생할 수 있습니다:

- 동일한 사용자가 동시에 포인트를 충전하거나 사용할 때, **마지막으로 완료된 작업만 적용**되어 이전 작업의 결과가 덮어씌워질 수 있음.
- 각 사용자의 포인트가 **정확하게 일관성**을 유지해야 하지만, 비동기 처리로 인해 **데이터 일관성**이 깨질 수 있음.

### 2. 요구사항

- **여러 스레드**가 동시에 충전과 사용 요청을 처리할 때, 데이터 일관성을 유지해야 함.
- 한 사용자의 포인트가 **다른 사용자의 작업과 충돌하지 않도록** 처리해야 함.
- 충전과 사용 요청이 동시에 들어올 경우, 각 요청이 **순차적으로 처리**되어야 함.

## 해결 방법

### 1. 동시성 제어 방법: `ReentrantLock`

- 각 사용자의 요청을 **서로 독립적으로 처리**하기 위해, **ReentrantLock**을 사용하여 동시성 제어를 구현했습니다.
- `ReentrantLock`은 한 스레드가 Lock을 획득하면, 다른 스레드는 해당 Lock이 해제될 때까지 대기하게 하여 **동시에 여러 스레드가 동일한 자원**을 처리하는 문제를 해결합니다.

### 2. Lock 관리: `ConcurrentHashMap`을 이용한 사용자별 Lock 관리

- **ConcurrentHashMap**을 사용하여 사용자별로 **Lock 객체**를 관리했습니다. 각 사용자는 고유한 ID를 가지고 있으며, 사용자별로 Lock을 부여하여 **동일한 사용자의 충전/사용 요청이
  순차적으로 처리**되도록 보장합니다.
- `computeIfAbsent` 메서드를 사용하여, Lock 객체가 없는 사용자는 새로운 Lock을 생성하고, 이미 Lock이 있는 사용자는 기존 Lock을 재사용합니다.

```java
private static final ConcurrentHashMap<Long, Lock> userLocks=new ConcurrentHashMap<>();

private Lock getLockForUser(long id){
	return userLocks.computeIfAbsent(id,key->new ReentrantLock());
	}
```

### 3. 동시성 제어 적용: `charge` 및 `use` 메서드

- 각 사용자별로 동시성 제어를 적용한 `charge`와 `use` 메서드를 통해 **포인트 충전**과 **사용**이 정확하게 처리되도록 하였습니다.

```java
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
```

```java
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
```

## 구현 세부 사항

### 1. `PointService` 구현

- **포인트 충전**과 **포인트 사용**에 대해 각 사용자별로 동시성을 제어하여 구현하였습니다.
- `ReentrantLock`을 사용하여 **각 사용자의 요청**이 병렬로 처리되는 과정에서 **일관성**을 유지하였습니다.

### 2. 테스트 코드 및 검증 방법

- **JUnit**을 사용하여 동시성 제어가 잘 이루어지는지 테스트했습니다.
- **다중 사용자**가 동시에 요청을 보내는 상황을 시뮬레이션하여, 각 사용자의 포인트가 정확히 처리되었는지 검증했습니다.
- `CountDownLatch`와 `ExecutorService`를 사용하여 **여러 스레드**가 동시에 요청을 보내는 상황을 구현했습니다.

```java
@Test
@DisplayName("동시성 테스트 - 여러 사용자가 랜덤으로 포인트 충전 또는 사용 시도")
void concurrentChargeAndUsePointsByMultipleUsersRandomized() throws InterruptedException {
    // given
    int numberOfUsers = 20;
    int numberOfThreadsPerUser = 20;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers * numberOfThreadsPerUser);
    CountDownLatch countDownLatch = new CountDownLatch(numberOfUsers * numberOfThreadsPerUser);
    long defaultPoint = 300L;

    // 사용자별 초기 포인트 설정
    List<Long> userIds = new ArrayList<>();
    for (int i = 0; i < numberOfUsers; i++) {
        Long userId = ++incrementId;
        userPointTable.insertOrUpdate(userId, defaultPoint); // 각 사용자에게 초기 포인트 할당
        userIds.add(userId);
    }

    Random random = new Random(); // 사용자 선택과 포인트, 작업을 위한 랜덤 생성기
    AtomicInteger successCount = new AtomicInteger(); // 성공 횟수

    // when
    for (int i = 0; i < numberOfThreadsPerUser * numberOfUsers; i++) {
        executorService.execute(() -> {
            try {
                // 랜덤한 사용자를 선택
                Long userId = userIds.get(random.nextInt(userIds.size()));

                // 랜덤하게 충전 또는 사용 선택 (true: 충전, false: 사용)
                boolean isCharge = random.nextBoolean();

                // 랜덤한 포인트 값 (1 ~ 100 사이의 포인트)
                long amount = random.nextInt(100) + 1;

                if (isCharge) {
                    // 랜덤 충전
                    pointService.charge(userId, amount);
                } else {
                    // 랜덤 사용
                    pointService.use(userId, amount);
                }

                successCount.incrementAndGet();
            } catch (IllegalArgumentException e) {
                // 실패 시 아무 것도 하지 않음
            } finally {
                countDownLatch.countDown();
            }
        });
    }

    countDownLatch.await(); // 모든 스레드가 완료될 때까지 대기
    executorService.shutdown();

    // then
    for (Long userId : userIds) {
        UserPoint userPoint = userPointTable.selectById(userId);
        List<PointHistory> pointHistoryList = pointHistoryTable.selectAllByUserId(userId);

        // 충전 포인트 합계 계산
        long totalCharged = pointHistoryList.stream()
            .filter(history -> history.type() == TransactionType.CHARGE)
            .mapToLong(PointHistory::amount)
            .sum();

        // 사용 포인트 합계 계산
        long totalUsed = pointHistoryList.stream()
            .filter(history -> history.type() == TransactionType.USE)
            .mapToLong(PointHistory::amount)
            .sum();

        // 각 사용자의 최종 포인트 검증 (기존 포인트 + 충전 포인트 합계 - 사용 포인트 합계)
        assertThat(userPoint.point()).isEqualTo(defaultPoint + totalCharged - totalUsed);
    }
}
```

## 성능 최적화

- 처음 동시성 제어를 할 때 메서드에 synchronized 를 사용해서 제어를 하였는데 이는 서로 다른 사용자끼리도 Lock이 걸리기 때문에 이를 개선하여 ConcurrentHashMap 과 ReentrantLock 을 사용하는 방식으로 수정하여 Lock이 걸리는 시간을 최소화하여 성능 최적화 하였습니다.

## 결론

이 프로젝트를 통해 다중 사용자 환경에서 발생할 수 있는 동시성 문제를 성공적으로 해결하였습니다. `ReentrantLock`과 `ConcurrentHashMap`을 사용한 동시성 제어 방식은 각 사용자별 요청
처리를 안전하고 효과적으로 관리할 수 있었습니다.
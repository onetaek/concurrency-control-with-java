# [ 1주차 과제 ] TDD 로 개발하기

## 과제 필수 사항
- 프로젝트에 첨부된 설정 파일은 수정하지 않습니다.
- 테스트 케이스 작성 및 작성 이유를 주석으로 작성합니다.
- 프로젝트 내의 주석을 참고하여 필요한 기능을 작성합니다.
- 분산 환경은 고려하지 않습니다.

## 🔥 TODO
- Default
    - [X] `/point` 패키지 (디렉토리) 내에 `PointService` 기본 기능 작성
    - [X] `/database` 패키지의 구현체는 수정하지 않고, 이를 활용해 기능을 구현
    - [X] PATCH  `/point/{id}/charge` : 포인트를 충전한다.
    - [X] PATCH `/point/{id}/use` : 포인트를 사용한다.
    - [X] GET `/point/{id}` : 포인트를 조회한다.
    - [X] GET `/point/{id}/histories` : 포인트 내역을 조회한다.
- Step1
    - [X] 포인트 충전, 사용에 대한 정책 추가 (잔고 부족, 최대 잔고 등)
    - [X] 동시에 여러 요청이 들어오더라도 순서대로 (혹은 한번에 하나의 요청씩만) 제어될 수 있도록 리팩토링
    - [X] 동시성 제어에 대한 통합 테스트 작성
- Step2
    - [X] 동시성 제어 방식에 대한 분석 및 보고서 작성

## 동시성 제어 방식에 대한 분석 및 보고서
<div aglin="center">  
  <a href="https://velog.io/@wontaekoh/Jenkins%EC%99%80-Docker%EB%A1%9C-AWS%EC%84%9C%EB%B2%84%EC%97%90-%EC%9E%90%EB%8F%99-%EB%B0%B0%ED%8F%AC%ED%95%98%EA%B8%B0">
    <img src="https://velog-readme-stats.vercel.app/api?name=wontaekoh&slug=동시성-문제-및-Java에서의-해결방법" alt="Velog's GitHub stats: 동시성-문제-및-Java에서의-해결방법" />
  </a>
  <img src="https://github.com/user-attachments/assets/d0c7ed89-9d56-42e2-8be4-d9c4081c446f" />
</div>
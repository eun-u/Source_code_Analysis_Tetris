# G0 공통 계약

2026-09-28. 이 문서는 현재 구현의 기준이다. 이전 phase 문서와 네트워크 설계는 이력이며, 아래 실행 범위와 구분한다.

## 책임과 흐름

```text
TetrisApplication → ModeManager → MatchSession
                                 ├─ LocalMatchSession → MonsterSession → BattleManager
                                 └─ OnlineMatchSession → TcpNetworkClient → LocalGameServer

BattleManager → 참가자 ID → PlayerController → GameEngine
AIContext → WeightPolicy → 공통 휴리스틱 → AIPlan → 로컬 세션의 입력 적용
SessionUpdate → SessionUiBinding → Swing EDT → UI
```

`MatchSession`은 두 대전 모드의 입력·상태·구독·종료 계약이다. package-private `PlaySession`은 기존 로컬 실행 구현용이며 온라인에 적용하지 않는다. 튜토리얼은 별도 학습 흐름이다.

## 입력과 결과

- `PlayerIntent`: 사용자 허용 Type 또는 USE_ITEM + ItemUse. 실행자 ID·엔진 순번·피해량 제외
- `GameAction`: 신뢰된 실행 경로에서 생성하는 엔진 명령. 사용자 입력에는 GRAVITY_TICK/RECEIVE_GARBAGE 등 서버 전용 명령 금지
- `MatchSession.submit/requestPause/leave`: 로컬 요청 ID 반환. 성공은 SessionUpdate의 CommandOutcome에서 확인
- `CommandOutcome`: requestId, accepted, 실패 reasonCode, matchId, 선택적 battleVersion. 성공 reasonCode는 null
- `BattleManager.submitItem`: payload 대상 검증 후 ITEM_NOT_IMPLEMENTED 반환. G0의 실제 효과·소비 없음
- `BattleManager.forfeit`: 명시적 탈락 처리, PAUSED에서도 가능, 마지막 생존자 확정

로컬 요청 결과는 요청 반환 뒤 EDT 큐에서 전달한다. 원시 온라인 세션 구독은 즉시 거절 콜백이 요청 반환 전에 올 수 있으므로 UI는 `SessionUiBinding`을 사용한다. 이 바인딩은 요청 결과를 항상 EDT 큐로 전달하고, 모든 전달 시 Snapshot을 최신 상태로 대체하여 화면 역행을 막는다. 경기 전환이나 CLOSED/FAILED 이후 이전 이벤트는 폐기하며 요청 결과는 원래 matchId를 유지한다. 구독 해제 뒤 예약된 콜백은 폐기한다. 같은 연결의 원시 수신 콜백은 NetworkClient가 순서를 보장해야 한다.

## 스레드와 수명주기

- TetrisApplication, ModeManager, LocalMatchSession, Swing UI: EDT 전용
- 로컬 중력 400ms·AI 확인 50ms: LocalMatchSession 소유. AI 실제 행동 간격은 프로필의 delayMillis
- AI 계산: AIController 단일 daemon worker. 요청 시 AIContext의 보드·관측 사본 고정
- 온라인: 서버가 tick과 전투 상태 소유. OnlineMatchSession은 상태 변환만 수행
- close: 반복 허용, 타이머·AI 작업·구독·네트워크 종료. 종료 후 신규 MatchSession 입력은 CLOSED 오류
- 로컬 pause: 남은 AI 대기 시간 보존, 정지 시간 제외
- 온라인 pause: PAUSE_NOT_ALLOWED

## 불변 상태 생성

`CoreSnapshots.board/game/copyOf`와 `BattleSnapshots.participant/battle/copyOf`는 Wire 변환 및 독립 UI 샘플용이다. 10×22 보드·셀·피스 위치·ghost·ID 연결·HP·승자·종료 사유를 검증하고 배열/목록/맵을 복사한다. 생성된 사본은 표시 데이터이며 실제 엔진 상태를 주입하는 setter가 아니다.

SessionSnapshot의 RUNNING/PAUSED/FINISHED에는 현재 참가자가 포함된 BattleState가 필요하다. CONNECTING/WAITING/FAILED/CLOSED에는 BattleState가 없다. UI는 nullable 상태를 사용하기 전에 phase를 검사한다.

## Story

- `StageCatalog`: stable Stage/Monster ID와 NORMAL→ELITE→BOSS 순서
- `MonsterSpec`: id, name, tier, hp, aiProfileId. 행동 간격과 가중치는 별도 AI 프로필 소유
- `CampaignProgress`: 앱 실행 중 전투별 완료 기록 및 Stage 해금
- `StoryProgressService`: 결과 반영의 단일 소유자. startStage에서 잠금 검사
- `EncounterRun`: 새 전투 실행 ID와 참가자 연결. 로컬 matchId는 runId와 동일
- recordBattleResult는 실제 해당 세션의 BattleResult만 사용. stale 결과·중복·상충 결과 구분
- 앱 어댑터가 runId와 실제 로컬 세션을 결합. UI render는 승패·해금 기록을 수정하지 않는 구조
- 홈 이동 후 해금 유지, 재진입은 첫 미완료 전투, 완료 Stage 재플레이는 일반부터 시작
- 성은 원본의 난이도 직접 선택은 startEncounter에서 이전 난이도 승리와 Stage 해금을 검사하며 해금된 난이도는 직접 재도전 가능
- 다음 전투·재도전은 새 보드와 최대 HP. 재도전은 AI 관측 초기화, 다음 전투는 현재 실행의 관측 유지
- Save 미구현, 앱 재시작 시 진행 초기화

## AI

일반·엘리트·보스 모두 `PolicyDrivenStrategy`의 공통 탐색·평가를 사용한다. G0의 등록 정책은 FIXED다. 정책은 `PolicyDecision`을 반환하며 이전 가중치·정책 상태는 전체 AI 입력이 수락된 뒤에만 확정한다. 취소·stale·빈 계획·부분 거절은 정책 상태를 전진시키지 않는다.

프로필은 `src/main/resources/ai/profiles.properties`에서 정의한다. Story는 profileId로만 참조한다. 쉬움·어려움 선택/배율과 Mirror 모델은 제품 코드에서 제거했다. 적응·보스 국면 정책은 AI 담당 후속 작업이다. 난이도 상승은 아직 밸런스 검증 결과가 아니다.

## 네트워크의 현재 경계

`TcpNetworkClient`와 `LocalGameServer`가 실제 TCP 대전을 제공한다. 현재 서버는 `127.0.0.1`에 바인딩하며, 방은 2명 고정이다. 연결별 서버 발급 참가자 ID로 입력 실행자를 확정하고, 참가자·준비 상태·전투 상태는 ID 기반으로 관리한다. 서버가 400ms 중력 tick과 전투 판정을 소유한다. 방 생성·참가·준비 뒤 경기 시작, 퇴장·연결 종료 시 기권 판정, 종료 후 양측 재준비로 새 matchId의 재대전을 지원한다.

Wire 버전 1은 4바이트 big-endian 길이와 UTF-8 JSON 본문을 사용하며 프레임 상한은 1MiB다. 알 수 없는 필드, 잘못된 타입·값·버전은 거절한다. 요청 ID는 연결 내에서 엄격히 증가하고 엔진 명령 순번과 별개다. 전투 상태는 전체 Snapshot으로 전달하며 `EVENTS` Wire 메시지는 지원하지 않는다. 첫 Snapshot의 version=0 허용, 유효 MATCH_STARTED와 roomVersion에 따른 새 경기 채택, 구경기 Snapshot 폐기, 종료 후 RUNNING 역행 차단, 요청 접수와 확정 결과 구분이 세션 계약이다. `FakeNetworkClient`는 테스트 전용이며 제품 JAR에 포함하지 않는다. LAN 접속, 재접속·상태 복원, 인증 서비스는 후속 범위다.

## 작성·변경 규칙

Java 8, UTF-8, 공백 4칸. 타입 PascalCase, 메서드/변수 lowerCamelCase, 상수 UPPER_SNAKE_CASE. 시간 단위는 Millis/Nanos, 참가자·화면은 ID 기반 Map 사용. 한국어 주석은 명사형 종결. 공유 API 변경은 입력·결과·실패·호출부·테스트·인수 문서를 함께 갱신한다.

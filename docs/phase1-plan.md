# Phase 1: Core 확장과 오프라인 일반 몬스터 대전

> 구현 후 계약 보완: Garbage 상단 초과는 Core 이벤트의 `GARBAGE_TOP_OUT` 사유로 구분하며, 대기 Garbage는 220행을 넘으면 변경 전에 거절한다. AIController는 한 게임 세션에만 사용하고 종료 시 닫는다. 완료 결과의 실패를 먼저 확인한 뒤 성공한 stale 계획만 폐기한다. 실제 검증은 [검증 기록](phase1-verification.md)에 정리했다.

2026-09-28. 사용자의 후속 단계 진행 지시에 따라 Phase 0 검증 후 다음 실행 가능한 단계를 구현한다. 온라인/로그인/외부 연동과 Git commit/push는 계속 보류한다. UI는 기본 Swing을 유지한다. 현재 28개 production 소스, 7개 test 소스이며 기존 6개 headless 묶음이 재검증을 통과했다. 수정 전 소스/docs/런처를 `%TEMP%/tetris-phase1-before-20260928-142924`에 보관했다.

## 범위와 책임

- Core: HOLD(한 고정당 1회), NEXT 3개, Ghost 위치, Combo, 명시적인 간이 T-Spin 판정, Garbage 큐/주입. 기존 좌표/키/낙하/지연 스폰은 유지. SRS/Wall Kick은 이번에 추가하지 않는다.
- Battle: ID별 참가자 맵, HP/Damage/Garbage, Top Out과 HP 고갈의 별도 패배, 일시정지/결과. 인간과 AI가 같은 GameEngine과 규칙을 사용한다.
- AI: 가능한 입력 경로를 탐색하는 HeuristicStrategy, BoardEvaluator, 비동기 AIController, 계산량/시간 측정. HOLD 후보도 평가한다. 아이템 판단은 AI에 넣지 않는다.
- UI/app: 홈에서 혼자 플레이/몬스터 대전 선택, 양쪽 보드/HP/HOLD/NEXT, pause/home/resume/result. 새 계약의 snapshot만 표시한다.
- 후속: Character/Item 효과/Fever, Adaptive/Mirror AI, Stage 진행, 온라인/사용자/저장/상점, 최종 이미지/소리. 현재 단계에서 구현했다고 표시하지 않는다.

## 공통 계약 확장

기존 생성자/getter는 유지한다. `GameState`에 `getHoldPiece(): PieceType`(없으면 EMPTY), `canHold(): boolean`, `getNextPieces(): List<PieceType>`(불변, 시작 후 3개), `getGhostY(): int`, `getCombo(): int`(-1이면 무연속), `getPendingGarbageLines(): int`를 추가한다. NEXT를 채울 때 실패한 generator로 인해 부분 상태가 커밋되지 않도록 내부 준비 버퍼를 사용한다.

Core `GameEvent.Type`에 PIECE_HELD, COMBO, T_SPIN, GARBAGE_QUEUED, GARBAGE_RECEIVED를 추가한다. LINE_CLEAR의 `getLineCount`, `getCombo`, `isTSpin`을 Battle이 공격의 단일 근거로 사용한다. 콤보는 첫 연속 클리어 0, 다음 1, 무클리어 고정 시 -1이다. T-Spin은 T의 마지막 유효 조작이 회전이고 고정 직전 네 대각 코너 중 3개 이상이 벽/고정 셀일 때다. 실제 이동한 좌우/낙하와 HOLD는 회전 표식을 해제하고, 0칸 하드 드롭은 유지한다. 공식 SRS/T-Spin Mini 판정을 구현한 것으로 주장하지 않는다.

`GameAction.Type.RECEIVE_GARBAGE`, `GameAction.Garbage(int lines, int holeColumn)`, `GameAction.garbage(actorId, sequence, payload)` 및 `Controller.submit(Garbage)`를 추가한다. 이 명령은 trusted Battle 경로가 제출하고 BattleManager의 일반 사용자 submit에서는 거절한다. 큐에 넣을 때 GARBAGE_QUEUED, 다음 블록 고정 경계에서 적용할 때 GARBAGE_RECEIVED. 보드 아래에 구멍 1칸을 제외한 GARBAGE 셀 행을 삽입하며 상단 고정 셀이 밀려나면 별도 이유의 TOP_OUT/GAME_OVER. 활성 블록에 즉시 행을 밀어 넣어 스폰 위치 때문에 즉사시키지 않는다. `PieceType.GARBAGE`는 생성 가능한 테트로미노가 아니다.

`PlacementSimulator.spawnX(BoardState)`와 `spawnY(BoardState, Piece)`로 AI의 HOLD 후보 스폰과 Core 스폰 좌표를 공유한다. 기존 `canPlace`/`hardDrop`는 순수 계산이다.

## Battle API

`ParticipantSpec(id, name, maxHp)`, `BattleManager(List<ParticipantSpec>, long seed)`. 각 참가자는 같은 seed의 별도 generator/engine/controller를 소유한다. 내부는 ID별 맵이며 이번 UI는 2인이다. constructor, `start()`, `submit(actorId, GameAction.Type)`, `tick()`, `pause()`, `resume()`는 `BattleResult`, `getState()`는 불변 `BattleState`를 반환한다.

`BattleState`: Status READY/RUNNING/PAUSED/FINISHED, `getStatus/getVersion/getParticipants/getParticipant(id)/getWinnerId/getReason`. ParticipantState: `getId/getName/getHp/getMaxHp/getGameState/isEliminated`. BattleResult: `isAccepted/getReason/getState/getEvents`. BattleEvent: Type MATCH_STARTED/CORE_EVENT/DAMAGE/HP_CHANGED/GARBAGE_SENT/GARBAGE_RECEIVED/MATCH_FINISHED/PAUSED/RESUMED/ACTION_REJECTED, `getType/getActorId/getTargetId/getAmount/getReason/getCoreEvent/getEventId`.

DamageManager는 LINE_CLEAR 하나당 한 번 계산한다. 시험 규칙: 1/2/3/4줄 기본 피해 4/8/12/20, T-Spin이면 기본 2배, Combo 보너스 `2*min(max(combo,0),5)`. Garbage 기본 0/1/2/4행, T-Spin +2, Combo>=2이면 +1. 기본 HP=100. 이 수치는 초기 플레이 가능한 기준이며 밸런스 검증 결과가 아니다. HPManager는 0..maxHp로 제한한다. HP가 0이면 패배하며 Core Top Out과 별도 취급한다. 공격 대상은 다음 살아 있는 참가자이며 마지막 한 명이 남으면 경기 종료, 경기 후 입력/시계/추가 공격은 거절한다. pause/resume는 참가자 전부에 적용한다.

## AI API/실행 정책

`AIStrategy.plan(GameState): AIPlan`, `HeuristicStrategy()`와 `HeuristicStrategy(HeuristicWeights)`. `HeuristicWeights`는 안전/빠른 클리어/Tetris 지향 프로필을 제공한다. 후보 평가는 line/height/maxHeight/hole/bumpiness/well 등으로 한다.

`AIPlan`: `getSourceVersion/getActions/getCandidateCount/getElapsedNanos/getScore/isTimedOut`. actions는 불변 GameAction.Type 리스트. 단순 목적 좌표로 순간이동하지 않고 유효한 좌/우/회전/soft drop 입력 경로 후 HARD_DROP을 반환한다. UI/엔진 수정 없이 immutable state에서 계산한다. 후보 탐색은 상태 수 상한/시간 제한/interrupt 검사로 제한한다.

`controller.AIController(AIStrategy)`는 단일 daemon executor를 소유한다. `request(GameState): boolean`, `poll(GameState current): AIPlan`(미완료/오래된 결과는 null), `isThinking`, `cancelPending`, `close`. 예외는 보고하며 무시하지 않는다. 계산은 EDT 밖, 계획 실행은 app의 단일 EDT 큐에서 BattleManager.submit을 거친다. 세션 교체/홈/일시정지/종료에서 취소하고 version이 다른 결과는 폐기한다. 계산 속도와 별도로 몬스터 행동 간 지연을 둔다.

## 작업 순서와 소유권

1. Core/controller 기존 파일 + core tests는 Core 담당만 변경.
2. battle 패키지/tests는 Battle 담당만 변경. Core/AI 파일은 수정하지 않음.
3. ai 패키지, 새 controller/AIController.java와 AI tests는 AI 담당만 변경. 기존 Controller/PlayerController는 Core 담당 소유.
4. app/ui/resource, 통합/GUI 테스트, 빌드/문서는 통합 담당 소유.
5. 통합 컴파일 → Core/Battle/AI/화면 회귀 → 독립 리뷰 → 수정 → 최종 검증. 다음 단계 기능은 이 범위에 섞지 않는다.

검증: HOLD 중복/리셋/교체 Top Out, NEXT 결정성/불변성/실패 원자성, Ghost 실제 낙하와 일치, Combo/T-Spin, Garbage 큐/overflow/일시정지, 피해/HP/중복 공격/전투 종료, AI 입력 경로 실행의 유효성/오래된 계획/취소/계산 시간, 실제 Swing 대전/홈복귀/결과/창닫기. 측정값은 기기와 샘플 수를 기록하며 성능 보장으로 과장하지 않는다.

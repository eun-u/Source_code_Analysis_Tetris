# Phase 0 실행 설계 및 계약 (2026-09-28)

이번 사용자 지시는 사전 분석을 검증한 뒤 **리팩토링을 실제 수행하고 홈에서 게임을 실행**하는 것이다. 이전 `phase0-prework.md`는 당시 검토 기록으로 보존한다. 그 문서의 승인 대기, 두 모드만 허용, Maven/JUnit 도입 제안은 아래의 현재 결정으로 대체한다. Git commit/push, 온라인, 로그인, 외부 서비스는 이번 범위가 아니다.

## 1. 현재 코드와 기준선

저장소 전체는 Java 소스 5개, IntelliJ 설정, 기존 설계 문서/스크린샷이다. `Main`은 예제이며 실제 흐름은 `Tetris.main → JFrame → Board.start → Timer/키 → Board 변경 → paint`다. `Board`가 입력, 시계, 규칙, 셀 저장, 표시를 모두 맡고 `Tetris`의 JLabel을 역참조한다. `Shape`는 회전과 랜덤 생성을 함께 담당한다. `Tetrominoes`는 종류만 정의한다.

수정 전 현 소스로 Java 8 컴파일 및 Swing 기준선 11개 확인: 창/시작, 좌/우 이동, 좌/우 회전, D 낙하, P 일시정지/복귀, Space 고정, 줄 제거, Top Out. 기존 소스와 docs를 저장소 밖 `tetris-phase0-before-20260928-135318` 임시 폴더에 복사했다. 기존 한국어 설명의 의미를 새 책임 위치에 옮긴다. 기준선의 입력 검사는 리스너 호출이며 OS 키 포커스 검증은 새 UI에서 별도로 수행한다.

보존할 동작: 10×22, 아래가 y=0, 블록 상대 좌표 적용은 `(x + dx, y - dy)`, 스폰 x=6, y=21+minY, 400ms 중력, 기존 회전 좌표/키, 줄 제거 후 다음 중력 틱에 스폰. 의도적 수정: 줄 압축 후 상단을 비워 셀 복제를 막음, `Random.nextInt(7)`로 음수 인덱스 제거, EDT 시작, 창 단위 키 바인딩, 홈에서 중력 정지.

## 2. 변경 순서 및 범위

1. 현 코드/설계/기준선 검증, 이 문서와 계약 검토.
2. `src/main/java`, `src/main/resources`, `src/test/java` 도입. Java 8 표준 라이브러리만 사용하는 오프라인 빌드/테스트/JAR 스크립트 제공. Maven/JUnit 다운로드는 요구하지 않음.
3. 순수 core 추출 및 결정적 회귀 테스트. 도메인에서 Swing/AWT import 금지.
4. 입력/중력 → Controller → Action → Engine → 불변 State/Event 흐름 연결.
5. 홈, 게임, 결과 화면과 Router, classpath AssetManager 연결. UI는 기본 Swing만 사용.
6. 컴파일, headless 회귀/계약 테스트, 실제 창/키 입력 및 패키징 검증, 독립 코드 리뷰와 수정.

이번 단계는 기존 게임을 실행 가능한 구조로 바꾸는 단계다. HOLD/NEXT/Ghost/Combo/T-Spin/SRS, HP/전투/아이템, AI 전략, 스테이지, 소켓/로그인/저장/상점은 후속 기능이다. 미구현 Action은 명확하게 거절하고, 홈에는 실제 실행 가능한 게임과 안내만 제공한다.

## 3. 이번 단계의 실제 구조

```text
src/main/java/
  Main.java                            호환 실행 진입점
  kr/ac/jbnu/se/tetris/
    Tetris.java                        호환 실행 진입점
    app/TetrisApplication.java          의존성 조립 및 세션/화면 수명주기
    core/
      Board, BoardState                변경 가능한 격자(엔진 내부) / 읽기 전용 격자
      PieceType, Piece                 종류 / 불변 상대 좌표와 회전
      PieceGenerator, SeededPieceGenerator
      GameAction, GameActionSink, ActionResult
      GameEvent, GameState, GameEngine
      PlacementSimulator, PlacementResult  AI용 순수 배치 평가 계약
    controller/Controller, PlayerController
    ui/MainWindow, Screen, ScreenRouter
       HomePanel, GamePanel, BoardView, ResultPanel, GameKeyBindings
    resource/AssetManager
src/main/resources/
  assets.properties
  images/{background,character,monster/{normal,elite,boss},block,item,icon,effect,ui}
  sound/{bgm,sfx}, fonts
src/test/java/                        별도 실행 가능한 Java 테스트
build.ps1, run.cmd                    Java 8+ JDK 빌드/검증/실행
```

이후의 최종 패키지는 사용자 제안처럼 `battle`(BattleManager, DamageManager, HPManager, FeverManager, ItemEffect), `ai`(AIStrategy, HeuristicStrategy, AdaptiveStrategy, MirrorStrategy, BoardEvaluator, PlayerProfile), `story`(Stage, StageManager, Monster 및 등급), `character`(CharacterType, CharacterStats, modifier), `item`(Item, Inventory, ItemPattern), `mode`(GameMode, LocalMode, StoryMode, OnlineMode), `network`(GameServer, GameClient, GameRoom, RoomManager, PlayerSession, NetworkMessage), `user`(UserService, SaveData, ProgressData), `shop`(Shop, CurrencyManager)로 확장한다. `resource`에 SoundManager/ThemeManager, `ui`에 Stage/Lobby/Room/Shop/Settings/Tutorial 화면을 등록한다. 아직 작동하지 않는 빈 클래스를 미리 대량 생성하지 않는다.

## 4. 고정 API

### 공통 명령 및 결과

`GameAction`은 불변의 `Type type, String actorId, long sequence`. sequence는 한 엔진/참가자 입력 스트림에서 0 이상 단조 증가한다. 타이머도 같은 Controller를 사용한다. 시각은 외부 wall clock 대신 엔진의 논리 tick으로 기록한다. Type: START, MOVE_LEFT, MOVE_RIGHT, ROTATE_LEFT, ROTATE_RIGHT, SOFT_DROP, HARD_DROP, GRAVITY_TICK, PAUSE, RESUME, HOLD, USE_ITEM. START는 READY에서만 허용하며 재시작은 새로운 세션/엔진이다.

아이템 명령은 `GameAction.ItemUse(itemId, targetActorId, optional TargetCell(x,y))`라는 불변 payload를 받는다. `GameAction(Type, actorId, sequence, ItemUse)` 생성자와 `getItemUse()`를 제공한다. 기존 3인자 생성자는 유지한다. itemId/targetActorId는 비어 있을 수 없고 셀 좌표는 0 이상이며, 실제 보드 범위/소유권/인벤토리/효과 검증은 후속 Battle 구현에서 한다. non-USE_ITEM 명령에 아이템 payload를 붙일 수 없다. payload가 있어도 Phase 0의 USE_ITEM은 명확하게 미지원 거절이다.

`GameActionSink.dispatch(GameAction): ActionResult`, `GameActionSink.getState(): GameState`.

`ActionResult`: `isAccepted()`, `getReason()`, `getState()`, `getEvents()`. 한 dispatch는 검증, 원자적 전이, 불변 스냅샷/순서 있는 이벤트 반환이다. 충돌, 잘못된 actor, 중복/역전 sequence, 상태 위반, 미구현 액션은 실패 결과로 반환한다. 유효 actor의 새 sequence는 액션이 거절되어도 소비한다. 상태 version은 수락한 전이에서 증가하고, 거절은 보드/version/tick을 바꾸지 않는다. GRAVITY_TICK이 수락될 때만 논리 tick 증가. 이벤트 구독자에서 엔진으로 재진입하는 동기 호출은 만들지 않는다. 조립 계층이 결과를 UI/로그/향후 Sound/Battle로 순서대로 배포한다.

### State 및 Piece

충돌 거절은 좌우 이동/회전에 적용한다. SOFT_DROP/GRAVITY_TICK이 아래로 움직일 수 없으면 **수락된 고정 전이**이며 PIECE_PLACED를 발행한다. HARD_DROP도 바닥에서 고정한다. 줄 제거 대기 중에도 PAUSE/RESUME를 허용하는 것은 기존 입력 사각지대를 고치는 의도적 변경이다.

`PieceGenerator.nextPiece(): PieceType`이며 `SeededPieceGenerator(long seed)`는 기존의 7종 균등 독립 추출을 유지한다(7-bag 추가 아님). 생성기는 세션마다 새 인스턴스로 소유한다. BoardState는 `getWidth/getHeight/getCell`을 제공한다.

주입된 생성기가 null/EMPTY를 반환하거나 예외를 던져도 dispatch는 부분 변경을 남기지 않는다. 스폰이 필요한 전이에서 다음 Piece를 먼저 검증하고 실패하면 이유가 있는 거절 결과를 반환한다. 격자/활성 블록/누적 줄/상태/version/tick은 그대로 유지하며, 새 sequence만 소비한다. 생성기 자체의 외부 부수효과까지 되돌리는 계약은 아니다.

`GameEngine(PieceGenerator)` 및 `GameEngine(String actorId, PieceGenerator)`, 기본 actor `local`. `GameState`는 **한 참가자의 보드 세션**이며 `getActorId`, `getVersion`, `getTick`, `getStatus`, `getBoard`, `getActivePiece`, `getPieceX`, `getPieceY`, `getLinesCleared`, `isAwaitingSpawn` 제공. Status: READY/RUNNING/PAUSED/GAME_OVER. BoardState는 width/height 및 `getCell(x,y)`만으로 읽는다. Piece는 `getType`, `getRotation`, `x(i)`, `y(i)`, `minY`, `rotateLeft`, `rotateRight` 제공. PieceType: EMPTY,Z,S,I,T,O,L,J. State/배열/목록은 방어적 복사, Piece는 불변이다.

향후 경기 전체는 `BattleState`의 참가자 ID → GameState 맵과 HP/Fever/Item 별도 상태로 구성한다. 두 명을 고정 필드로 만들지 않는다. HP 0과 TOP_OUT은 별도 패배 사유다. UI 및 전투가 core.Board를 직접 조작하지 않는다. Garbage/Item 명령을 추가할 때 엔진 소유자가 typed payload/권한/회귀 테스트를 함께 추가한다.

### AI용 배치 평가 계약

`PlacementSimulator.canPlace(BoardState, Piece, x, y)` 및 `hardDrop(BoardState, Piece, x, startY): PlacementResult`를 공개한다. 원본 snapshot에서 분리한 Board에 기존 충돌/낙하/고정/줄 제거 규칙을 적용하며 살아 있는 엔진을 변경하지 않는다. 결과는 `isValid/getReason/getBoard/getPiece/getX/getY/getLinesCleared`를 제공하는 불변 값이다. 시작 위치가 막힌 후보는 거절한다. 이 API는 배치의 기하학적 유효성만 평가하며 현재 블록에서 그 후보까지 이동/회전할 수 있는 **입력 경로의 도달 가능성**까지 증명하지 않는다. AI의 계획은 최종적으로 공통 GameAction 경로에서 검증된다. AI가 충돌/줄 제거를 복사해 구현할 필요는 없다.

### Event

`GameEvent`: Type, eventId, stateVersion, tick, actorId와 종류별 검증된 데이터. 실제 Phase 0 발생 종류: GAME_STARTED, PIECE_SPAWNED, PIECE_MOVED, PIECE_ROTATED, PIECE_PLACED, LINE_CLEAR, PAUSED, RESUMED, TOP_OUT, GAME_OVER, ACTION_REJECTED. 배치에는 고정된 Piece/x/y, 줄 제거에는 lineCount, 거절/종료에는 reason이 포함된다. eventId는 엔진 세션 안에서 단조 증가하며 전역 유일 ID가 아니다. 재시작 후 로그에는 별도 sessionId가 필요하다. 후속 COMBO/T_SPIN/DAMAGE/GARBAGE/HP/FEVER/ITEM/STAGE 이벤트는 문서상 예약이며 아직 발행하지 않는다.

고정 순서: START → GAME_STARTED, PIECE_SPAWNED. 고정 → PIECE_PLACED, (LINE_CLEAR) 또는 PIECE_SPAWNED. 줄 제거 시 활성 블록은 없고 다음 중력 틱에서 스폰. 스폰 충돌 → TOP_OUT, GAME_OVER 각 1회. 거절 → ACTION_REJECTED; 거절 사유도 명시.

### Controller/스레드

`Controller.submit(GameAction.Type): ActionResult`, `Controller.getState(): GameState`. `PlayerController(GameActionSink, String actorId)`가 단조 sequence를 만든다. Controller는 Swing 키를 모른다. 키→Action은 ui.GameKeyBindings의 책임이다. 향후 AIController는 계산한 Action을 이 경로에 제출하고, NetworkController는 인증/프로토콜 검증 후 동일 sink에 제출한다. 각 엔진의 입력 sequence 소유자는 하나이며 여러 생산자는 그 소유자의 큐로 합류한다. 세 종류 모두 같은 core 규칙을 사용한다.

아이템 선택 생산자도 `Controller.submit(GameAction.ItemUse)` 오버로드로 같은 sequence 소유자에게 제출한다. item rule/pattern과 Tetris placement strategy는 계속 별도 책임이다.

현재 세션 명령과 UI 반영은 EDT에서 직렬 처리하고 엔진 dispatch/getState도 동기화한다. 향후 AI는 불변 snapshot을 worker에서 평가하고 `(sessionId, stateVersion, actions)`를 제출한다. 오래된 계획은 폐기하며 EDT/서버 단일 큐에서 실행한다. 네트워크 수신 스레드가 Swing을 직접 호출하지 않는다. AI 아이템 패턴/보스 스크립트는 별도 담당이다.

## 5. 화면/Asset 계약

Screen: `getId(): String`, `getPanel(): JPanel`, `onEnter()`, `onExit()` (기본 no-op). Router는 ID 등록/전환과 수명주기만 관리한다. Home → Game → Result → 새 게임/Home. Home 이동 시 자동 일시정지, 계속하기는 같은 세션을 복귀, 새 게임은 별도 seed/엔진. Result에서 종료된 엔진에 중력이 계속 전달되지 않는다. 창 닫기/화면 이탈도 timer를 정리한다. Mock은 immutable GameState/뷰 입력만으로 그릴 수 있다.

AssetManager는 classpath `/assets.properties`의 안정적인 ID를 실제 리소스 경로로 해석하고 원본 및 표시 크기 캐시, 누락 시 생성 placeholder와 1회 경고를 제공한다. 파일 시스템 `src/...` 경로에 의존하지 않는다. 키 예: `character.default`, `monster.normal`, `monster.elite`, `monster.boss`, `item.heal`, `background.menu`. 크기 규격은 normal 256, elite 384, boss/character 512, item/block 64, icon 48, background 1920×1080. 실제 파일이 없는 상태도 앱 실행이 가능해야 한다. UI에 가짜 HP/Stage 수치를 실제 상태처럼 보여주지 않는다.

## 6. 검증 및 병렬 소유권

Core: 7개 도형/회전, 벽/바닥/기존 블록 충돌, soft/hard drop, 한 줄/다중 줄/상단 압축, 지연 스폰, pause/resume, top out, 종료 후 액션, seed 동일성, snapshot 불변, actor/sequence 거절, 이벤트 순서/버전. HOLD/USE_ITEM 미지원 거절도 검사한다.

UI: 홈/게임/결과/복귀/새 세션, 기존 키 매핑, 버튼 포커스 후 조작, 화면 이탈/종료 시 timer 정지, 크기 변화/asset 누락. headless Swing 테스트와 실제 창 키 입력을 구분해 기록한다. JAR을 프로젝트 밖 CWD에서도 실행해 classpath 리소스 검증. 테스트 결과를 `phase0-verification.md`에 작성한다.

소유권: core/controller 및 해당 테스트는 한 구현자, ui/resource/app 및 빌드/문서는 통합 담당. Core 계약을 먼저 고정하고 두 담당은 서로의 파일을 수정하지 않는다. 독립 리뷰 후 통합 담당이 최종 빌드/회귀 확인. Phase 1 기능 병렬 개발은 이 게이트 완료 이후 별도 작업이다.

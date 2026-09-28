# Phase 0 사전 분석 및 설계 검토안

> 과거 검토 기록(2026-09-24). 2026-09-28 사용자 실행 지시에 따른 현재 범위·계약·빌드 결정은 [phase0-implementation.md](phase0-implementation.md)를 따른다. 아래 승인 대기 및 도구/모드 제안은 당시 기록이다.

상태: **설계 검토용**. 이 문서는 리팩토링 결과가 아니다. 승인 전에는 게임 코드와 게임 모드를 변경하지 않는다.

## 1. 저장소와 현재 실행 흐름

- 현재 추적 파일은 Java 소스 5개, IntelliJ 설정 및 `.gitignore`뿐이다. README, 빌드 스크립트, 테스트, 리소스, 서버 코드는 없다. IntelliJ 프로젝트의 언어 수준은 Java 8이다.
- 실제 게임 진입점은 `kr.ac.jbnu.se.tetris.Tetris.main()`이다. 루트의 `Main.main()`은 IntelliJ 생성 예제의 문구만 출력한다.
- `Tetris`가 `JFrame`과 상태 표시 `JLabel`을 만들고 `Board`를 붙여 `start()`를 호출한다. 기본 창 크기는 200×400이다.
- `Board`는 10×22 셀 배열을 비우고 임의의 블록을 생성한다. Swing `Timer`가 400ms마다 한 줄 낙하를 호출한다. 키 입력과 타이머가 같은 `Board` 메서드를 직접 실행하고, 상태 변경마다 화면을 다시 그린다.
- 블록 고정 → 줄 제거 → 다음 블록 생성 순으로 진행한다. 새 블록을 스폰할 수 없으면 타이머를 멈추고 상태 표시를 `game over`로 바꾼다. 줄 제거 시에는 다음 타이머 틱까지 스폰을 지연한다.
- 기존 키 매핑은 ←/→ 이동, ↑ 왼쪽 회전, ↓ 오른쪽 회전, `D` 한 칸 낙하, Space 끝까지 낙하, `P` 일시정지다. HOLD, NEXT, Ghost, Combo, HP는 아직 없다.

### 실행 기준선 (2026-09-24)

처음에는 현재 환경의 PATH에 Java가 없어, 저장소 밖 임시 폴더에 공식 Eclipse Temurin JDK 8.0.504+1 ZIP을 받았고 SHA-256을 배포 메타데이터와 대조했다. Java 8 옵션으로 소스 5개를 컴파일했으며 6개 클래스 파일이 생성됐다. 저장소 밖 임시 검증 프로그램이 Swing EDT에서 `Tetris` 창을 열고 다음을 확인했다: 창/게임 시작, 좌우 이동, ↑/↓ 회전, D 낙하, P 일시정지/해제, Space 하드 드롭/고정, 1줄 제거 및 상태 표시, 스폰 충돌 시 Game Over. [기준선 화면](phase0-baseline.png)을 저장했다. 이후 설치된 Oracle Java 8u503 JRE의 `java.exe`로 같은 검증 프로그램을 다시 실행해 모두 통과했다. 다만 현재 터미널은 갱신된 PATH를 인식하지 못하고, 설치 항목에는 `javac`이 없어 컴파일에는 임시 JDK를 사용했다. 키 조작은 등록된 `KeyListener`를 직접 호출했으므로 실제 OS 키 포커스 전달은 아직 수동 확인 대상이다. 테스트 프로그램과 JDK는 저장소에 넣지 않았다.

| 클래스 | 현재 책임 | 주요 의존성 |
| --- | --- | --- |
| `Main` | 실행과 무관한 Hello World 예제 | 없음 |
| `Tetris` | 창 생성, 상태 표시, `Board` 시작 | Swing, `Board` |
| `Board` | 셀 상태, 이동/회전/충돌/낙하/줄 제거/게임 종료, 입력/타이머/렌더링 | Swing, `Tetris`, `Shape`, `Tetrominoes` |
| `Shape` | 블록 좌표와 회전, 랜덤 블록 선택 | `Tetrominoes`, `Random` |
| `Tetrominoes` | 빈 셀과 7종 블록의 열거형 | 없음 |

의존 방향은 `Tetris → Board → Shape → Tetrominoes`이며 `Board → Tetris` 역참조가 추가되어 있다. `Board`의 게임 규칙은 Swing 없이 사용할 수 없으므로 AI·서버·테스트가 모두 UI에 묶인다.

## 2. 확인된 설계 위험과 결정할 사항

1. `Board`는 규칙·상태·입력·타이머·그림·상태 표시를 한 클래스에서 처리한다. 전투 기능을 여기에 추가하면 공유 규칙과 UI의 경계가 사라진다.
2. `Shape.setRandomShape()`는 호출마다 새 `Random`을 만들며 시드를 주입할 수 없다. `Math.abs(nextInt()) % 7`은 `Integer.MIN_VALUE`에서 음수가 될 수 있다.
3. `Board.removeFullLines()`는 행을 당긴 뒤 맨 위 행을 비우지 않아 상단 셀이 복제될 수 있다. 리팩토링 중 관찰 테스트로 현상을 기록하고, 기대 규칙은 별도 테스트로 고친다.
4. `Board`는 `paint()`를 재정의하고 입력용 포커스를 요청하지 않는다. 실제 키 동작은 창 포커스에 따라 확인해야 한다.
5. `Tetris.main()`은 Swing EDT 밖에서 UI를 만든다. 새 진입점은 EDT에서 생성한다.
6. 현재 코드의 `D`는 Soft Drop, Space는 Hard Drop에 대응한다. ↑/↓ 회전 방향도 테스트로 고정한다. 새 기본 키 배치는 별도 UX 결정으로 다룬다.
7. 서버 권한, 동기화 방식, 전투 수치, 세이브 형식은 아직 구현 근거가 없다. Phase 0에서는 인터페이스 경계와 데이터 계약만 고정하고 정책 수치는 확정하지 않는다.

## 3. 의존 방향과 패키지 제안

Java 8을 유지하고 표준 빌드 구조인 `src/main/java`, `src/main/resources`, `src/test/java`로 옮긴다. Phase 0에서 빌드 도구와 테스트 실행 명령을 함께 고정한다. 이관 중에는 `Tetris.main()`을 호환 실행 진입점으로 유지한다.

```text
kr.ac.jbnu.se.tetris
├─ app/             Application, composition root, launcher
├─ core/            GameEngine, GameState, BattleState 데이터, BoardState, Piece, PieceGenerator,
│                  GameAction, GameEvent, BattleSystem 계약, 규칙과 순수 계산
├─ battle/          BattleSystem 구현, 이후 HP/Damage/Fever/Modifier
├─ controller/      Controller, PlayerController, AIController, NetworkController 경계
├─ mode/            GameMode, StoryPvEMode, OnlinePvPMode 경계
├─ story/           Stage와 Monster 진행 데이터(후속 단계)
├─ ai/              블록 배치 전략(후속 단계)
├─ item/            아이템 정의·효과·사용 패턴(후속 단계)
├─ character/       능력치와 전투 수정자(후속 단계)
├─ network/         서버·클라이언트·세션·메시지(후속 단계)
├─ user/            인증·저장 DTO·진행 데이터(후속 단계)
├─ shop/            구매와 재화(후속 단계)
├─ tutorial/        학습 진행 흐름. GameMode 구현 금지
├─ ui/              MainWindow, Screen, ScreenRegistry, ScreenRouter, ViewModel
└─ resource/        AssetManager, 이후 SoundManager·ThemeManager
```

의존 규칙: `core`는 Swing/AI/Network/Asset을 import하지 않는다. Controller는 명령만 만들고 규칙을 실행하지 않는다. Mode는 진행 상태를 관리하고 공통 전투 엔진을 사용한다. UI/Sound/Logging은 읽기 전용 상태와 이벤트를 구독한다. `story`와 `network`가 별도 보드·피해 계산을 구현하지 않는다.

## 4. Phase 0에서 고정할 핵심 계약

아래는 Java 8 기준의 API 형태 제안이다. 상태 변경은 한 `GameEngine` 처리 경로에서만 발생한다.

```java
interface PieceGenerator { Piece next(); }
interface GameActionSink { ActionResult dispatch(GameAction action); }
interface Controller { void start(GameActionSink sink); void stop(); }
interface GameEventListener { void onEvent(GameEvent event, GameState snapshot); }
interface BattleSystem { BattleResolution resolve(CoreOutcome outcome, BattleState state); }
interface GameMode { String id(); void start(); void onEvent(GameEvent event); }
```

- `GameEngine`은 `GameActionSink`를 구현한다. `dispatch(GameAction)`은 검증 → 단일 상태 전이 → 순서가 정해진 이벤트 생성 → 읽기 전용 스냅샷 발행을 수행한다. 결과에는 수락 여부, 상태 버전, 이벤트 목록이 들어간다. 타이머는 `GRAVITY_TICK` 명령을 제출한다.
- `BoardState`는 격자와 충돌·줄 제거만 소유한다. 화면 좌표, 타이머, 키 코드, HP는 소유하지 않는다.
- `Piece`는 블록 종류·회전·좌표의 값 객체다. `PieceGenerator`는 주입되며 테스트용 고정 시퀀스와 향후 시드 기반 구현을 지원한다.
- 이벤트 구독자는 게임 상태를 직접 변경하지 않는다. `BattleState`와 `BattleSystem` 인터페이스는 `core`의 계약이고, 전투 규칙 구현은 `battle` 패키지에 둔다. 조립 지점에서 구현을 엔진에 주입하므로 `core`는 `battle`을 import하지 않는다. 이벤트는 UI·소리·로그·동기화로 전달한다.
- Online 입력은 서버가 actor/sequence/권한을 검증한 뒤 공통 `GameAction`으로 변환한다. 최종 상태의 기준은 서버이며, 클라이언트는 이벤트와 버전이 있는 스냅샷으로 차이를 보정할 수 있게 한다.

### GameAction

`GameAction`은 불변의 `{type, actorId, sequence, tick, itemId?, targetId?}`로 정의한다. `sequence`와 `tick`은 로컬에서도 일관되게 기록해 향후 재생·온라인 검증에 사용한다. `USE_ITEM`의 선택 정보는 명시적 타입/필드로 전달하고 임의 `Map`에 숨기지 않는다.

| 종류 | 의미 |
| --- | --- |
| `MOVE_LEFT`, `MOVE_RIGHT` | 활성 블록 한 칸 이동 |
| `ROTATE_LEFT`, `ROTATE_RIGHT` | 기존 회전 방향을 유지한 회전 요청 |
| `SOFT_DROP`, `HARD_DROP` | 한 칸 낙하, 바닥까지 낙하 후 고정 |
| `HOLD` | 후속 단계의 보관 블록 교체 |
| `USE_ITEM` | 후속 단계의 아이템 사용 요청 |
| `GRAVITY_TICK` | UI 타이머 또는 서버 시계의 낙하 명령 |
| `PAUSE`, `RESUME` | 로컬 실행 수명주기 요청. Online 허용 여부는 모드 정책이 결정 |

미구현 행동은 성공한 척하지 않고 명시적으로 거절한다. 튜토리얼도 같은 명령을 사용하며 독립 게임 모드가 아니다.

### GameEvent

`GameEvent`는 불변의 `{eventId, stateVersion, tick, actorId, targetId?, type, details}`다. `details`는 이벤트 종류별 타입으로 검증한다. 한 명령에서 나온 이벤트 순서를 보장한다.

| 범주 | 이벤트 |
| --- | --- |
| Core | `GAME_STARTED`, `PIECE_SPAWNED`, `PIECE_MOVED`, `PIECE_ROTATED`, `PIECE_PLACED`, `LINE_CLEAR`, `COMBO`, `T_SPIN`, `TOP_OUT`, `GAME_OVER` |
| Battle | `DAMAGE`, `HP_CHANGED`, `GARBAGE_SENT`, `GARBAGE_RECEIVED`, `FEVER_STARTED`, `FEVER_ENDED`, `ITEM_USED` |
| Mode | `STAGE_STARTED`, `STAGE_CLEAR`, `MATCH_RESULT` |
| 검증 | `ACTION_REJECTED` |

Phase 0에서는 현재 규칙에서 실제로 발생하는 이벤트만 발행한다. `LINE_CLEAR`에는 제거 줄 수, `GAME_OVER`에는 `TOP_OUT` 원인을 포함한다. 후속 이벤트 이름은 계약 예약이며 기능 완료를 뜻하지 않는다.

### GameState

`GameState`는 한 경기의 스냅샷이다: `{version, tick, lifecycle, participantsById, battleState}`. 참가자별 상태는 `{board, activePiece, x, y, hold, nextQueue, linesCleared, combo, toppedOut}`이다. `BattleState`는 참가자별 HP/fever/item/garbage를 위한 별도 구조이며 Phase 0에서는 계약 수준으로 둔다. 스토리 Stage 진행과 Online lobby/room 상태는 각각 Mode 상태로 분리한다. UI에는 방어적 복사 또는 불변 뷰만 준다.

### Controller 및 Mode

- `PlayerController`: Swing 키를 매핑해 명령 제출. 키 바인딩은 UI에만 있다.
- `AIController`: `AIStrategy`의 배치 결과를 공통 명령 시퀀스로 변환한다. 아이템 선택은 별도 `MonsterItemPattern`의 책임이다.
- `NetworkController`: 프로토콜 명령의 신원·순서를 확인하고 공통 명령으로 변환한다. 규칙 계산은 서버의 공통 엔진이 한다.
- `StoryPvEMode`: Stage/몬스터 순서, 보상, 진행 상태를 소유한다. 일반/엘리트/Boss는 Stage 내부 분류다.
- `OnlinePvPMode`: 연결/로비/방/준비/경기/결과 상태를 소유한다. 서버 세션 목록을 사용하며 참가자 두 명을 필드로 고정하지 않는다.
- 공유 `BattleSystem`이 두 Mode의 공격/HP 판정을 처리한다. GameMode 구현은 정확히 위 둘만 존재한다.

## 5. 동적 화면 및 Asset 계약

```java
interface Screen {
    String getId();
    JPanel getPanel();
    default void onEnter() {}
    default void onExit() {}
}
```

- `ScreenRegistry`는 ID → Screen을 등록·조회·해제한다. 중복 ID와 미등록 ID는 명확한 오류로 다룬다.
- `ScreenRouter`는 Registry를 조회해 `CardLayout`에 화면을 붙이거나 제거한다. 전환 때 `onExit/onEnter`를 호출하고 필요한 경우 Back Stack을 관리한다. Swing 작업은 EDT에서 처리한다.
- `MainWindow`는 프레임과 Router의 컨테이너일 뿐 화면 목록을 알지 않는다. 화면 등록은 `Application` 조립 지점에서 한다. Home 메뉴 항목도 등록 가능한 navigation entry로 만들어 메뉴 개수 변경이 Router 수정으로 이어지지 않게 한다.
- Phase 0 UI는 Home, 기본 게임 표시, Story/Online 진입 상태의 최소 골격을 사용한다. Battle UI는 읽기 전용 ViewModel 또는 mock GameState로 미리 그릴 수 있다. 미구현 기능을 플레이 가능한 것으로 표시하지 않는다.
- `src/main/resources/images/{background,character,monster/normal,monster/elite,monster/boss,block,item,icon,effect,ui}`, `sound/{bgm,sfx}`, `fonts`를 표준 리소스 트리로 둔다. 런타임은 ClassLoader로 로드한다.
- `AssetManager`는 안정적인 asset ID → 경로/규격 매핑, 캐시, 크기 조정, 누락 시 placeholder 반환을 담당한다. 누락은 로그에 한 번 기록한다. `ThemeManager`는 후속 단계에서 ID/스킨 변경만 담당한다.
- Phase 0에는 monster/character/item placeholder를 보이게 하는 샘플 리소스 또는 생성형 fallback을 둔다. 최종 PNG를 같은 ID에 대체할 수 있도록 한다. 권장 원본 규격은 monster 256/384/512, character 512, item/block 64, icon 48, background 1920×1080이다.

## 6. 리팩토링 실행 순서와 게이트

1. **기준선 확보:** Java 8 컴파일·실행 환경 확인. 기존 키, 낙하, 줄 제거, Top Out, 일시정지를 화면과 자동화 가능한 테스트로 기록. 실제 검증 여부를 구분한다.
2. **빌드·테스트 기반:** 재현 가능한 빌드 도구와 JUnit 5를 추가하고 소스/리소스를 표준 위치로 이동. 현재 `Tetris.main()` 동작 유지. 루트의 예제 `Main`은 진입점 혼동을 없앤다.
3. **순수 Core 추출:** `Shape/Tetrominoes`를 `Piece` 계층으로 옮기고 `PieceGenerator`, `BoardState`, `GameState`, `GameEngine`을 도입. 충돌·이동·회전·낙하·줄 제거·Top Out을 Swing 없이 실행한다.
4. **입력/이벤트 분리:** 키와 타이머를 `GameAction`으로 변환. Engine 결과의 이벤트/스냅샷으로 렌더링. 기존 입력·낙하 타이밍·일시정지를 회귀 검증한다.
5. **확장 경계:** Controller, BattleSystem, 두 GameMode의 최소 인터페이스를 만든다. AI/네트워크/HP/Stage의 실제 로직은 구현하지 않는다.
6. **UI/Asset 골격:** MainWindow, 동적 Registry/Router, 최소 Home/게임 표시, AssetManager/placeholder를 연결한다. 기존 Tetris 실행 경로도 확인한다.
7. **통합 게이트:** 모든 테스트와 수동 화면 검증을 통과시키고 API 문서, 패키지 소유권, 후속 작업의 입력/출력 계약을 확정한다. 그 뒤에만 병렬 기능 개발을 시작한다.

## 7. 기존 기능 보호 테스트 계획

| 단계 | 검증 | 판정 기준 |
| --- | --- | --- |
| 사전 화면 | `Tetris.main()` 실행, 창 표시, 키 포커스 | 창·보드·상태 표시가 보이고 키가 동작함 |
| Core | 좌우 이동/벽 충돌, 좌·우 회전, 낙하/고정 | 결정적인 보드와 블록 시퀀스에서 예상 좌표·셀 |
| Core | Single/연속 줄 제거, 맨 위 행 정리 | 제거 줄 수와 모든 셀이 예상 상태. 기존 상단 복제 결함은 별도 재현 테스트 |
| Core | 스폰 충돌/Top Out | `GAME_OVER` 1회, 이후 입력 거절·틱 중지 |
| 입력 | ↑/↓/D/Space/P 기존 매핑 | 기존 조작 결과 동일, 일시정지 중 보드 불변 |
| 이벤트 | 한 액션의 상태 버전/이벤트 순서 | Spawn/Place/Line Clear/Game Over가 정확히 1회씩 발행 |
| UI | 화면 등록/해제/전환, asset 누락 | Router 전환 및 fallback 표시, 규칙 상태 불변 |

테스트용 `FixedPieceGenerator`와 수동 시계로 난수·시간을 통제한다. GUI 테스트는 실제 Swing EDT에서 수행하고, Core 테스트는 headless로 실행한다. Phase 0에서는 HOLD/NEXT/Ghost/HP/AI/Online 동작을 완료 조건으로 오인하지 않는다.

## 8. Phase 0 후 병렬 작업 경계

| 작업 흐름 | 소유 패키지 | 의존하는 고정 계약 |
| --- | --- | --- |
| Core/Battle | `core`, `battle`, `item`, `character` | Action/Event/State, BattleSystem |
| Story/AI | `mode`의 Story 구현, `story`, `ai` | GameState 읽기 뷰, Controller, 배치 명령, Mode 이벤트 |
| Online/Data | `mode`의 Online 구현, `network`, `user`, `shop` | Action 시퀀스, State 버전/스냅샷, 서버 권한 |
| Frontend/Asset | `ui`, `resource`, `tutorial` | 읽기 전용 State, GameEvent, Screen/Asset ID |

공유 파일의 변경은 Core 계약 소유자가 통합한다. 팀이 3명이라면 네 작업 흐름 중 Frontend/Asset을 별도 소유권으로 지정하되 일정과 인력을 배분한다. 인터페이스 변경은 예제 DTO와 계약 테스트를 함께 갱신한다.

## 9. 검토가 필요한 설계 결정

1. 빌드 도구: Maven + Wrapper를 제안한다. Java 8과 JUnit 5를 명시하고 IDE 밖에서도 같은 명령으로 빌드할 수 있다.
2. Phase 0 화면: 기존 `Tetris.main()` 호환 실행을 유지하면서 새 `Application` 진입점에 Home 골격을 둔다. 기존 게임을 별도 Solo 모드로 등록하지 않는다.
3. 회전 규칙: Phase 0은 현재 좌표 회전과 키 매핑을 보존한다. SRS/Wall Kick은 Core 후속 작업에서 사양과 테스트를 함께 추가한다.
4. 온라인 권한: 서버가 최종 경기 상태를 판정하는 방향을 제안한다. 전송 형식과 지연 보정 정책은 Online 작업에서 결정한다.

# Phase 0 검증 기록 (2026-09-28)

> 이 문서는 Phase 0 완료 시점의 기록입니다. 현재 구현과 검증 결과는 [Phase 1 검증 기록](phase1-verification.md)을 참고하세요.

## 수행 범위

저장소 분석 → [계약/실행 설계](phase0-implementation.md) → 독립 설계 검토 → core/controller 추출과 UI/실행 통합 → 회귀 검증 순서로 수행했다. 기존 `Board`/`Shape`를 유지한 별도 게임 엔진을 남기지 않았다. `src/main/java`의 순수 core와 Swing ui가 실제 실행 경로이며 `Main`과 `Tetris` 모두 홈으로 연결된다.

현재 제공 기능은 홈, 새 게임/계속하기, 기존 Tetris, 일시정지, 종료 결과/재시작, classpath placeholder이다. Phase 1 이상의 전투/AI/온라인 등은 구현하지 않았다. Git commit/push, 외부 로그인/네트워크 연결은 수행하지 않았다.

## 실행 환경 및 기준선

- 컴파일: 기존 임시 Temurin JDK `1.8.0_504`, `javac -encoding UTF-8 -source 8 -target 8`.
- 패키지 실행: 설치된 Oracle JRE `1.8.0_503`. 작업 디렉터리를 프로젝트 밖 TEMP로 지정해 JAR/리소스 경로 독립성을 확인했다.
- 수정 전 소스의 기준선 11개 검증을 이번 작업에서 다시 통과했다. 원본 소스/docs는 `%TEMP%\tetris-phase0-before-20260928-135318`에 보존했다. 이 임시 백업은 영구 저장을 보장하지 않는다.
- 새 런처는 임시 JDK가 사라지면 `JAVA_HOME` 또는 `-JdkHome`으로 지정한 JDK를 사용한다. 빌드된 JAR은 JRE만으로 실행된다.

## 자동 검증

`./build.ps1 -Task GuiTest`로 main Java 28개, test Java 7개를 컴파일하고 다음 6개 headless 테스트 묶음을 통과했다. 최종 실행 종료 코드는 0이다.

| 테스트 | 확인 내용 |
| --- | --- |
| BoardTest | 7개 블록의 기존 좌표, 회전, 벽/바닥/기존 셀 충돌, 불변 보드 snapshot, 단일/다중 줄 압축, 상단 행 복제 결함 수정 |
| GameEngineTest | 시작/정지/복귀, actor·sequence 거절, HOLD/USE_ITEM 미지원 거절, 이동/낙하/고정, 줄 제거 후 지연 스폰, Top Out/종료 이벤트, seed 재현, 이벤트 순서·버전. 생성기의 null/EMPTY/예외 실패에서 시작·hard/soft/gravity 고정·지연 스폰의 상태 불변 |
| PlayerControllerTest | 키와 중력의 공통 명령 경로, actor 보존, 단일 증가 sequence, 아이템/대상/선택 칸의 typed payload 전달 |
| PlacementContractTest | core 밖에서 사용하는 배치 시뮬레이션, 실제 엔진 고정 결과와 동등성, 잘못된 후보 거절, 원본 snapshot/이벤트 목록 불변 |
| AssetTest | JAR classpath manifest/PNG, 크기 조절/캐시, 누락 fallback·1회 경고, 잘못된 크기 거절 (6개 확인) |
| UiTest | Router 수명주기/오류, 기존 키 매핑, 홈/게임/결과/복귀/새 세션, 타이머 정리, 작은 화면 렌더링, EDT 규칙 (33개 확인) |

테스트는 Java main 진입점과 실패 시 AssertionError를 사용하는 오프라인 실행 방식이다. 외부 JUnit/Maven 패키지를 설치하지 않았다. AssetTest의 `Unknown asset ID: not.registered` 경고는 의도적인 누락 테스트 결과다.

## 실제 데스크톱 검증

DesktopSmoke는 실제 JFrame과 OS 키/마우스 입력으로 15개 확인을 통과했다: 홈 표시, 새 게임 버튼, P 일시정지, 정지 중 tick 불변, 계속 버튼, 버튼 클릭 뒤 ←/→, D, Space, Esc 홈 이동, 홈에서 tick 불변, 계속하기, Top Out 결과 화면, 다시 하기, 창 닫기 및 timer 종료.

첫 시도에서는 다른 창에 가려져 클릭 전달이 실패했다. 테스트 전용 창을 always-on-top으로 설정하고 포커스 확인 없이 OS 입력을 보내지 않도록 테스트 하네스를 보완한 뒤 통과했다. 실제 앱에는 always-on-top을 적용하지 않았다. `computer-use`는 패키징된 앱의 Tetris 창 존재를 확인했지만 다른 창에 가린 캡처는 시각 검증 증거로 쓰지 않았다.

통과 실행의 실제 화면 캡처는 `out/phase0/home.png`, `game.png`, `result.png`에 있다. 홈의 조작 안내와 버튼, 최소 창 크기 640×680의 게임 보드/조작부, 결과/재시작 버튼을 확인했다. UI 미관 개선은 요청대로 범위에서 제외했다.

## 패키징 및 구조

- `out/tetris.jar`: Main-Class는 `kr.ac.jbnu.se.tetris.Tetris`.
- 최종 검증한 JAR SHA-256: `18488B9FAA94AF558EF2B60BB9282B6015E1F8F7BD872F3BB326021D22377945`. 재빌드하면 JAR 타임스탬프 등으로 hash가 바뀔 수 있다.
- 프로젝트 밖 TEMP에서 Oracle JRE로 JAR의 AssetTest 6개 확인 통과. 실제 JAR 실행으로 Tetris 창도 생성했다.
- core/controller에 Swing/AWT/ui/app/resource/ai/network 의존 import 없음. Board에 Random/Timer/HP/게임 화면 코드 없음.
- IntelliJ 소스/리소스/테스트 루트 갱신, `out/`은 기존 ignore 정책 유지.
- `git diff --check` 통과. GitHub 업로드 없음.

## 변경 파일 정리

| 기존 파일/영역 | 현재 위치와 역할 |
| --- | --- |
| `src/Main.java`, 기존 `Tetris.java` | `src/main/java/Main.java`, 같은 패키지의 `Tetris.java`: 호환 실행 진입점 |
| 기존 Swing `Board.java` | `core/Board`, `GameEngine`, State/Action/Event와 `ui/BoardView`, `GameKeyBindings`, `app/TetrisApplication`으로 분리 |
| 기존 `Shape.java`, `Tetrominoes.java` | `core/Piece`, `PieceType`, `PieceGenerator`, `SeededPieceGenerator`로 분리 |
| 새 controller | Controller 계약과 PlayerController의 명령/sequence 연결 |
| 새 UI/resource | Home/Game/Result, ScreenRouter/MainWindow, AssetManager와 classpath manifest/placeholder |
| 테스트/빌드 | `src/test/java`, `build.ps1`, `run.cmd`, IntelliJ 소스 루트 수정 |
| 문서 | README, 현재 실행 설계/검증 기록. 이전 설계와 기준 화면 보존 |

## 독립 리뷰 및 수정

사전 설계 검토에서 바닥 충돌은 거절이 아니라 고정 전이라는 점을 명확히 했다. 구현 후 독립 리뷰의 세 가지 지적을 모두 반영했다.

1. AI가 충돌/줄 제거 규칙을 복제해야 했던 API 공백 → 별도 Board 복사본을 사용하는 PlacementSimulator/PlacementResult 공개.
2. USE_ITEM에 아이템/대상 정보를 담을 수 없던 공백 → ItemUse/TargetCell 값 객체 및 공통 Controller 오버로드 추가. 실제 효과는 계속 미지원.
3. 사용자 정의 PieceGenerator 실패 시 부분 상태 변경 → 다음 블록 사전 검증과 분리된 고정 결과 계산 후 상태 반영. 실패는 이유가 있는 ACTION_REJECTED이며 게임 상태는 불변.

수정 코드를 재검토하고 관련 테스트를 추가한 뒤 통합 테스트와 실제 창 검증을 다시 통과했다.

최종 독립 리뷰에서 남은 조치 필요 사항은 발견되지 않았다. 통합 담당이 최종 JAR의 hash를 다시 확인했고, 해당 JAR로 Tetris 홈 세션을 실행한 상태로 인계했다.

## 완료 경계

사람 입력은 PlayerController, 향후 AI/Network 입력은 같은 `GameActionSink`와 불변 State/Event를 사용한다. 이번 단계는 계약 및 공유 엔진 기반을 제공하며 실제 AIController·소켓·Battle 기능을 구현한 것은 아니다. Garbage/아이템 payload, 경기 전체 BattleState, 영속 SaveData는 후속 계약 확장 때 영향범위와 테스트를 함께 검토한다.

HOLD/NEXT/Ghost/Combo/T-Spin/SRS, HP/Damage/Fever/Character/Item, Heuristic/Adaptive/Mirror AI, Story/Stage, 온라인/로그인/상점/세이브, 최종 이미지/BGM/SFX는 후속 단계다. 이번 검증으로 전체 최종 요구사항 또는 서로 다른 PC의 온라인 동작을 확인했다고 해석하지 않는다.

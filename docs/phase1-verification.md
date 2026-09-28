# Phase 1 검증 기록 (2026-09-28)

## 완료 범위

Phase 0 기준선 분석과 [Phase 1 계약/계획](phase1-plan.md)을 먼저 정리한 뒤 Core, Battle, AI를 소유 영역별로 구현하고 app/UI에서 통합했다. 이번 단계에서는 혼자 플레이와 오프라인 일반 몬스터 대전을 실행할 수 있다. 홈 → 게임/대전 → 결과 → 같은 모드 재시작, 홈 복귀 및 계속하기를 지원한다.

- Core: HOLD, NEXT 3개, Ghost, Combo, 간이 T-Spin, 블록 고정 시 적용하는 Garbage, seed 생성기. 기존 좌표·회전·400ms 중력·줄 제거 후 지연 스폰을 유지한다.
- Battle: 참가자 ID 기반 2~4명 도메인 모델, HP/Damage/Garbage와 이벤트, HP 고갈·Top Out 탈락. 현재 화면과 앱은 2인 대전이다.
- AI: SAFE/QUICK/TETRIS 평가 가중치, 실제 이동 가능한 경로와 HOLD 후보 검색, 단일 worker, snapshot 버전 검사, 작업 취소, 배치 간 지연.
- UI: 기존 Swing 홈/게임/결과에 대전 화면과 HP·양쪽 보드·HOLD/NEXT 추가. 게임/피해 규칙은 도메인에 있다. 이미지 미관 개선은 범위에서 제외했다.

Character/Item/Fever, Adaptive/Mirror, Stage/Story, Save/Shop, SRS/Wall Kick, 최종 이미지/BGM/SFX는 아직 구현하지 않았다. 온라인·로그인·외부 연동과 Git commit/push는 수행하지 않았다.

## 기준선과 환경

- Phase 0의 6개 headless 묶음을 변경 전에 재검증했다. 직전 소스/docs/실행 파일 백업은 `%TEMP%/tetris-phase1-before-20260928-142924`에 보관했다. 임시 폴더는 영구 보관을 보장하지 않는다.
- 컴파일: 로컬 Temurin JDK 1.8.0_504, UTF-8/source 8/target 8. 외부 패키지나 JDK를 다운로드하지 않았다.
- 최종 패키지 실행: 설치된 Oracle JRE 1.8.0_503, 작업 폴더는 프로젝트 밖 TEMP.
- `build.ps1`의 컴파일/테스트 출력은 `out/phase1`로 옮겼다. 기존 `out/phase0` 증거는 유지했다. 실행 JAR은 `out/tetris.jar`이다.

## 최종 자동 검증

`build.ps1 -Task GuiTest` 실행: production Java 48개, test Java 12개 컴파일, **headless 11개 묶음과 실제 데스크톱 27개 항목 통과**, 종료 코드 0.

| 테스트 | 주요 확인 |
| --- | --- |
| BoardTest / GameEngineTest | 기존 좌표·회전·충돌·줄 제거·Top Out·명령 순서·불변 snapshot·생성기 실패 원자성 회귀 |
| GameEngineFeatureTest | HOLD 제한/교체/Top Out, NEXT 불변·선행 생성 실패, Ghost, Combo, 간이 T-Spin, Garbage 대기/적용/상단 초과/큐 상한 |
| PlayerControllerTest / PlacementContractTest | 공통 Action 전달, typed payload, 시뮬레이션과 실제 엔진 동등성 |
| BattleManagerTest | 피해 계산, HP 범위, 공격 중복 방지, Garbage, 여러 참가자 대상 선택, HP/Top Out/동시 탈락, 실패 종료, pause/resume |
| HeuristicStrategyTest | 실제 엔진에서 후보 경로 실행, HOLD, 세 가중치, 탐색 상한, 안전한 제한 시간 fallback, 입력/계획 불변 |
| AIControllerTest | 비동기 계산, 중복 요청, stale 결과 폐기, 취소/종료, 일반 및 stale 작업 예외 전달 |
| MonsterSessionTest | 실제 worker와 BattleManager로 끝까지 자동 대전, pause/cancel/resume/close, HP 피해와 Garbage |
| AssetTest | classpath manifest/PNG, 누락 fallback·캐시·크기 검증 6개 |
| UiTest | 홈/혼자/대전/결과/재시작, 양쪽 세션·타이머 종료, HOLD 키, EDT 규칙 등 48개 |

통과 로그: `out/phase1/verification.stdout.log`, `verification.stderr.log`. AssetTest의 `Unknown asset ID: not.registered`는 의도적인 누락 테스트 경고다. 처음 로그를 PowerShell `*>`로 감쌀 때 이 native stderr 경고가 `ErrorActionPreference=Stop`에 의해 실행을 중단시켰다. 최종 실행은 별도 PowerShell 프로세스의 stdout/stderr를 각각 파일로 저장했고 실제 종료 코드를 확인했다.

## 자동 대전과 AI 시간 표본

최종 통합 실행에서 seed=1, 사람 측은 테스트용 TETRIS 가중치 전략, 몬스터는 앱의 실제 SAFE 전략/worker를 사용했다. 배치 간 지연만 가상 시계로 단축했고 게임 상태를 직접 조작하지 않았다.

- 사람 43개 / 몬스터 43개 배치, HP 피해 및 대기 Garbage 확인.
- 종료 HP: 사람 10 / 몬스터 0, `HP_DEPLETED`, 승자 `local`.
- 몬스터 후보 합계 2,724개, 계산 시간 합계 145ms.
- 별도 AI 경로 표본: 24회 판단 / 후보 1,332개 / 계산 시간 합계 90ms.

이는 현재 PC의 특정 입력 표본이다. 모든 보드의 응답 시간이나 난이도·밸런스를 보장하지 않는다. 기본 검색은 1,800개 상태와 100ms 검사 예산으로 제한하며 EDT 밖에서 수행한다. OS 스케줄링까지 포함한 엄격한 시간 상한은 아니다. 700ms는 배치 사이 지연이며 개별 이동 애니메이션은 아직 없다. Combo/T-Spin의 일부 단위 테스트는 의도적으로 만든 보드 fixture를 사용한다.

## 실제 창 검증

DesktopSmoke는 실제 OS 키/마우스 입력으로 시작, 이동, Soft/Hard Drop, HOLD, P, Esc, 계속하기, 재시작을 확인했다. 몬스터가 실제로 블록을 배치하는 것도 대기 후 검증했다. 홈과 pause에서는 두 보드와 AI timer가 멈추고, 종료에서는 worker 작업과 timer가 정리된다. 결과 화면 검증을 위한 Top Out 쌓기만 app 명령으로 가속한다.

초기 실행에서 Continue 클릭이 한 번 전달되지 않은 현상이 있었으나 이어진 직접 실행 및 최종 전체 실행에서는 모두 통과했다. 원인은 확정하지 않았으며 테스트 중 데스크톱 입력 간섭 가능성은 남아 있다. 테스트 전용 창은 always-on-top이고 입력 전에 포커스를 검사한다. 실제 게임 창에는 이 속성을 적용하지 않는다.

실제 캡처 `out/phase1/home.png`, `game.png`, `battle.png`, `result.png`, `battle-result.png`를 확인했다. 최소 760×680 창에서 양쪽 보드·HP·HOLD/NEXT·홈/계속 버튼을 확인했다.

## 독립 검토 및 수정

작성 영역을 바꾸어 읽기 전용 교차 검토를 진행하고 통합 담당이 코드·테스트·실행 결과를 확인했다.

1. Garbage 상단 초과 사유를 `GARBAGE_TOP_OUT`으로 분리하고 대기 행을 최대 220개로 제한했다. 초과 요청은 상태 변경 전에 거절한다.
2. GARBAGE enum 추가로 seed 생성기가 Garbage를 뽑지 않도록 명시적인 7종 목록을 사용한다.
3. 완료된 AI 작업이 stale일 때 예외까지 숨기던 경로를 수정했다. 완료 결과를 확인한 뒤 성공한 stale 계획만 폐기한다. 실패는 원인을 전달한다. 동기화 latch로 순서를 고정한 회귀 테스트를 추가하고 독립 재검토했다.
4. AIController는 세션당 하나를 생성하고 종료 시 닫는 계약을 명시했다. 새로운 엔진에 같은 컨트롤러를 재사용하지 않는다.

검토 범위에서 추가로 수정이 필요한 실행 결함은 발견하지 못했다. 취소된 작업은 명시적으로 폐기하며 해당 결과를 적용하지 않는다.

## 패키지와 구조 확인

- 최종 JAR SHA-256: `D530DBF91FDD24D37FE14EA07B0D3CDB7137F1801C737A10AFAA2BDFC5A3E6FE`. 재빌드 시 타임스탬프 때문에 달라질 수 있다.
- TEMP에서 최종 JAR과 Oracle JRE로 AssetTest 6개를 통과했다. 해당 JAR을 별도로 실행해 Tetris 홈 창을 열었다. 실행 직후 stderr는 비어 있었다.
- Core는 Swing/AWT 및 상위 계층 import가 없다. Battle/AI/Controller에도 Swing/AWT import가 없다. AI와 사람 입력 모두 같은 엔진을 사용한다.
- `git diff --check` 통과. 기존 Phase 0 작업과 함께 미커밋 상태이며 업로드하지 않았다.

## 변경 영역

| 영역 | 변경 |
| --- | --- |
| core / 기존 controller | Action/State/Event 확장, HOLD/NEXT/Ghost/Combo/T-Spin/Garbage, seed 생성기 및 시뮬레이션 스폰 계약 |
| battle | BattleManager, DamageManager, HPManager, ParticipantSpec/State, BattleState/Event/Result |
| ai / AIController | Strategy, 가중치/평가/Plan, 합법 입력 검색, worker 및 stale/cancel/error 처리 |
| app | PlaySession, LocalSession, MonsterSession, TetrisApplication 조립 |
| ui | BattlePanel, PiecePreview/QueuePanel, 홈 선택, HOLD 키·Ghost·HP·결과, 최소 창 크기 |
| test / build / docs | 기능·대전·worker·UI 테스트, 실제 입력 검증, phase1 출력, README/API/검증 기록 |

다음 Phase 2 기능 개발은 이 인터페이스와 회귀 테스트를 기준으로 이어갈 수 있다. 이번 기록은 전체 최종 요구사항 완료나 온라인 동작 검증을 의미하지 않는다.

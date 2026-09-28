# 튜토리얼·대학 스토리 AI 검증 기록

2026-09-28. 기존 Phase 1 구현을 확인하고 [이번 범위](phase2-plan.md)에 따라 로컬 작업을 이어갔다. 기존 미커밋 파일/구조 변경을 보존했다. Git commit/push/upload는 수행하지 않았다.

## 완료 범위

- 혼자하기는 `TutorialSession`의 이동/회전/Soft Drop/HOLD/Hard Drop 5개 목표다. 수락된 Core 이벤트로 달성 여부를 확인하며 완료/Top Out 결과와 재시도를 제공한다.
- `StageCatalog`는 UTF-8 설정에서 5개 단계, 단계마다 일반→엘리트→보스 3개 상대를 읽는다. 전공책/시험→대학원 입학→학위 컨셉이며 총 15개 전투다. 단계 자유 선택, 승리 다음 상대, 승패 뒤 재플레이, 최종 단계 선택 복귀를 지원한다.
- 일반은 Heuristic, 엘리트는 최근 플레이 경향의 지수 이동 통계(α=0.15)로 가중치를 바꾸는 Adaptive, 보스는 실제 배치 로그를 학습하는 Mirror다. 모두 같은 합법 후보 탐색과 GameAction→BattleManager→GameEngine 경로를 사용한다.
- `PlayerProfile`은 배치/라인/Tetris/Combo/HOLD/Hard Drop/T-Spin/공격/높이/구멍/위험 틱 통계를 관리한다. `PlacementLog`는 시작 보드·활성 피스·NEXT/HOLD와 선택 위치/회전·HOLD·Hard Drop·라인/Combo/T-Spin 메타데이터를 최대 128건 보관한다. 일반 디버그 로그와 별개다.
- 기록은 같은 스토리의 다음 상대와 재도전에서 유지한다. 결과에서 홈으로 나가도 결과 복귀가 가능하다. 새 게임 시작/앱 종료 시 초기화하며 디스크 저장은 아직 없다.
- 기본 Swing을 유지하고 안내, 단계/난이도 선택, 결과 이동 UX만 연결했다. 시각 디자인·최종 이미지·사운드는 이번 작업에 포함하지 않았다.
- [네트워크 설계](network-design.md)는 서버 권위형 BattleManager, 2인 시작/2~4인 세션 자료구조, Action/Event/STATE_DELTA 및 주기 Snapshot, 순번·epoch·입력 검증, 방/재대결/연결 종료, 별도 PC 테스트 경계를 정의한다. 서버/클라이언트 실행 코드는 아직 없다.

전체 Phase 2/3 완료는 아니다. Character/Item/Fever, 몬스터 Item Pattern, Save/Shop/Login, 실제 원격 대전은 후속 범위다. Mirror는 로지스틱 쌍별 배치 순위 모델이며 점수를 보정된 사람의 선택 확률이나 검증된 모방 정확도로 해석하지 않는다.

## 컴파일·회귀·패키지

로컬 Temurin JDK 1.8.0_504, UTF-8/source 8/target 8, 외부 라이브러리 없이 production 62개/test 19개 소스를 컴파일했다.

`build.ps1 -Task Test`는 **17개 headless 테스트 묶음 PASS**, 종료 코드 0이다. 새 테스트 파일 중 수동 도구 `MonsterBalanceBenchmark`와 기존 `DesktopSmoke`는 자동 테스트 묶음에 포함하지 않는다.

| 검증 | 확인 내용 |
|---|---|
| 기존 Core/Battle/Controller/Asset | 충돌/회전/HOLD/NEXT/라인/Combo/T-Spin/HP/Garbage/Top Out/명령 순서/불변 상태/누락 asset 회귀 |
| AI 3개 묶음 | 합법 경로, HOLD, stale/cancel/error, 일반 회귀, 실제 통계로 엘리트 가중치 변경, 로그 제한, 보스 데이터 부족 fallback, 합성 일관 행동 학습 후 실제 위치 재현, 중단 후 같은 로그 재학습 |
| MonsterSessionTest | 실제 worker와 공통 엔진 대전, 피해/Garbage/종료, pause/resume/close |
| TutorialSessionTest | 순서 밖 조작/정지 중 입력 제외, 5개 목표, 완료 후 상태 고정 |
| StageCatalog/StageProgress | 5장/15상대, 설정 오류/중복/불변성, 난이도, 승패/재도전/중복 진행 차단 |
| StoryFlowTest | 실제 엔진으로 15상대 진행, 패배 retry, 승리 replay, 결과→홈→결과, 최종 복귀. 이 테스트는 EDT에서 시계를 직접 진행해 상대 AI 타이머를 배제하므로 밸런스 증거로 사용하지 않음 |
| UiTest | 화면/키/타이머/종료/스토리 선택 등 56개 확인 |
| OffscreenUiTest | 6개 화면 × 760×680/960×820, 12개 이미지 버퍼 출력 |

로그: `out/phase2/verification.stdout.log`, `verification.stderr.log`. 누락 asset 테스트의 `Unknown asset ID: not.registered` 경고는 의도한 검증이다. 마지막 수정은 AI 테스트의 측정값 출력뿐이며 해당 파일을 별도로 재컴파일하고 같은 테스트를 통과했다(`out/phase2/ai-metrics.log`). production JAR은 그대로다.

`out/tetris.jar`를 프로젝트 밖 TEMP 작업 디렉터리에서 classpath로 사용해 AssetTest와 StageCatalogTest를 통과했다. PNG와 단계 설정이 JAR 안에서 로드된다. JAR SHA-256:

```text
59C9A084D25249B45CB5B0FBA7830E34320D56D29A9A4718F505E1BC7BE8FF2B
```

`git diff --check` 통과. 새 Java 소스까지 별도 확인해 81개 Java 파일 모두 한국어 주석이 있다. Core/Battle/AI/Controller에는 Swing/AWT import가 없다. 기존 핵심 소스의 주석 작업은 담당 영역 34개 파일에서 주석·공백을 제외한 토큰 비교로 실행 코드/API 불변을 확인했다.

## 숨김 실행과 UX 확인

Java 검증은 headless로 실행했다. 최종 전체 검증은 `Start-Process -WindowStyle Hidden`으로 실행하고 직접 종료 코드를 확인했다. JFrame/Robot을 이용한 실제 게임 창과 OS 입력 자동화는 실행하지 않았다. 기존 `GuiTest`는 `-AllowVisibleDesktop` 없이는 즉시 거절하도록 변경했다.

**후속 확인 및 정정:** 사용자가 작업 중 PowerShell 터미널 노출을 보고했다(15:08:44 캡처). 앞선 headless 검증은 Java 화면 차단을 확인한 것이며, 모든 검증 호출의 콘솔 비노출을 확인한 것은 아니다. 일반 PowerShell 호출과 일부 `-WindowStyle Hidden` 호출이 혼재했고, 별도 프로세스의 콘솔 생성 자체를 차단하는 공통 실행 경로는 없었다. 따라서 사용자 화면/포커스에 전혀 영향을 주지 않았다는 보장은 철회한다. 캡처의 종료 코드 0은 정상 종료를 나타내지만 해당 프로세스가 이미 종료되어 정확한 개별 명령과 창의 연결은 현재 목록으로 확정하지 못했다. 이후 검증 실행부는 Java headless와 별개로 콘솔 생성 차단 및 표준 출력/오류 리디렉션을 적용하고, 상위 실행 도구의 창 노출도 별도로 확인해야 한다. 사용자의 터미널 설정 변경이나 기존 창 종료는 수행하지 않았다.

`out/phase2/ui/`의 최소 크기 단계 선택, 전투, 튜토리얼, 결과 이미지를 확인했다. 조작 안내, 단계/난이도 선택, 양쪽 HP/보드/HOLD/NEXT와 결과 버튼이 표시된다. 네이티브 창·OS 입력·실제 DPI에서의 동작은 이번 headless 검증으로 확인했다고 주장하지 않는다.

## 난이도 조정과 자동 대전

수동 도구 `MonsterBalanceBenchmark`로 1장/5장 × 일반/엘리트/보스 × 쉬움/보통/어려움 × seed 1/7 = **36대전**을 실행했다. 상대는 실제 비동기 MonsterSession, 사람 측 대체 입력은 SAFE Heuristic 700상태/40ms, 1,400ms마다 배치 기회다. 양쪽 중력은 400ms, 가상 시간 최대 120초, 경기당 실시간 한도 10초다. worker 대기 중 가상 시간을 멈춘다. 이는 EDT 부하/네트워크 지연을 재현하는 시험이 아니다.

최초 쉬움 배율은 HP×0.8/지연×1.3이었다. 이때 대체 플레이어는 쉬움 일반 4/4, 엘리트 3/4, 보스 2/4 승리했고 후반 보스에는 0/2였다. 쉬움의 체력과 속도를 함께 완화해 **HP×0.65/지연×1.75**로 변경했다. 보통은 설정 원값, 어려움은 HP×1.2/지연×0.85를 유지한다. 예: 1장 일반 보통 65HP/1700ms, 쉬움 42HP/2975ms; 5장 보스 보통 155HP/780ms, 쉬움 101HP/1365ms.

조정 후 동일한 36대전을 다시 실행했으며 모두 종료했고 시간 한도에 걸린 경기는 없다.

| 난이도 | 일반 상대 승수 | 엘리트 상대 승수 | 보스 상대 승수 | 전체 대체 플레이어 승수 |
|---|---:|---:|---:|---:|
| 쉬움 | 4/4 | 4/4 | 4/4 | 12/12 |
| 보통 | 4/4 | 1/4 | 0/4 | 5/12 |
| 어려움 | 2/4 | 0/4 | 0/4 | 2/12 |

보스 로그는 각 벤치마크 경기에서 빈 상태로 시작하며 대체 플레이어의 합성 배치가 쌓인다. 실제 스토리에서는 앞 상대의 실제 사용자 기록이 이어지므로 결과가 달라질 수 있다. 표본은 두 seed와 양 끝 단계뿐이다. 사람이 플레이하기 쉬워졌다는 실증, 각 단계의 승률 보장, 보스 모방 품질을 의미하지 않는다. 다음 체감 조정에는 초보/숙련 사용자별 클리어율·Top Out 비율·평균 시간이 필요하다.

원본: `out/phase2/balance-initial.log`, `balance-final.log`. 재현 명령(JDK java 사용):

```powershell
java -Djava.awt.headless=true -ea -cp 'out/phase2/classes;out/phase2/test-classes' kr.ac.jbnu.se.tetris.app.MonsterBalanceBenchmark
```

최종 표본의 개별 AI 판단 최대는 약 **44.7ms**였다. 별도의 일관 배치 16건 fixture에서 프로필 조회 0.0117ms, 보스 학습 24.5975ms, 후보 평가/예측 1.3036ms, 전체 판단 25.9065ms, 후보 18개를 측정했다. 학습 동작과 시간 계측용 합성 fixture이며 사람 데이터의 일반화 성능이 아니다.

탐색 제한은 일반 700상태/40ms, 엘리트 1200/65ms, 보스 1800/90ms다. Mirror 학습은 최근 최대 24건/25회 반복/300ms 검사 예산이며 별도 worker에서 실행된다. 예산은 루프 검사 기준이고 OS 스케줄링을 포함한 엄격한 지연 상한이 아니다. 학습 중 취소 시 같은 로그를 다시 시도할 수 있고 유효 모델은 보존한다. 장기 CPU 부하/메모리/다른 PC 성능은 미검증이다.

## 독립 검토와 반영

1. 학습 시작 시 revision을 소모해 취소 후 같은 데이터로 재학습하지 못하던 문제를 수정했다. 중단 후 재시도 테스트를 추가했다.
2. 멈춘 전투의 거절된 틱이 플레이어 위험 시간에 누적되지 않도록 실제 RUNNING/수락 이벤트만 센다.
3. 결과에서 홈으로 이동하면 스토리 진행에 돌아갈 수 없던 문제를 수정했다. 홈에서 같은 결과로 복귀하며 학습/진행을 유지한다.
4. 보스 점수 차이를 sigmoid 압축 결과로 판단하던 부분을 로짓 차이로 변경하고 실제 학습한 X 위치가 엔진에서 재현되는지 확인했다. 점수의 확률 보정은 주장하지 않는다.

최신 코드 재검토에서 추가 P1/P2 지적은 없었다. 이 기록은 이번 범위의 로컬 검증 완료이며 전체 최종 기능 목록의 완료를 뜻하지 않는다.

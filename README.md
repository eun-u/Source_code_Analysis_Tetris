# Tetris — Story AI · 온라인 PvP

Java 8 / Swing 기반의 실행 가능한 공통 개발 기반입니다. 팀별 시작 위치와 수정 범위는 [인수인계 시작점](docs/handoff/README.md), 입력·상태·스레드 계약은 [공통 계약](docs/contracts/g0-contracts.md)을 기준으로 합니다.

Render 서버·Supabase 로그인·온라인 PvP 랭킹의 설정 순서는 [배포·운영 안내](docs/render-operations.md)를 참고합니다. 실제 서비스 이용에는 프로젝트·메일·환경 변수 설정이 필요하며, 외부 절전 방지용 주기 요청은 포함하지 않습니다.

## 실행 및 검증

```powershell
.\build.ps1 -Task Run
.\build.ps1 -Task Test
.\build.ps1 -Task NetworkFixture
# 별도 터미널의 로컬 PvP 서버 (기본 포트 28080)
.\build.ps1 -Task Server -Port 28080
# 실제 창을 여는 개발 UI 미리보기
.\build.ps1 -Task Preview -AllowVisibleDesktop
```

`run.cmd` 또는 빌드된 `out/tetris.jar`로 실행할 수 있습니다. 빌드에는 JDK 8 이상이 필요하며 필요 시 `-JdkHome 'C:\path\to\jdk'`를 지정합니다. Maven Wrapper가 고정된 의존성을 내려받습니다. 실행 진입점은 `kr.ac.jbnu.se.tetris.Tetris.main()`과 `Main.main()`입니다. 컴파일 산출물은 `target`, 화면 검증 이미지는 `out/g0`, 제품 JAR은 `out/tetris.jar`와 `out/tetris-server.jar`에 생성됩니다. 테스트용 Fake와 미리보기는 JAR에서 제외됩니다.

## 현재 실행되는 범위

- 튜토리얼: 이동·회전·낙하·HOLD·Hard Drop의 5개 조작 목표
- Story PvE: 5개 샘플 Stage × 일반·엘리트·보스, HP 0 또는 Top Out에 따른 승패
- 진행: 전투별 완료 기록, 보스 클리어 후 다음 Stage 해금, 직접 잠금 우회 거절
- AI: 공통 탐색·평가를 재사용하며 일반 FIXED, 엘리트 ADAPTIVE, 보스 HP 국면별 BOSS 정책 적용
- 세션: 로컬 게임 시계·AI 실행과 온라인 표시 계약 분리, 종료·재시작·늦은 콜백 처리
- UI: 참가자 ID 기반 표시, 서버 없이 실행하는 화면 fixture
- Network: 로컬 TCP 서버·클라이언트, 방 생성·입장·준비·서버 판정·재대결·연결 종료 기권
- 온라인 랭킹: 인증된 WebSocket 대전, Supabase 계정, 서버 확정 결과의 원자적 Elo 저장과 랭킹 조회. 각 방은 1:1이며 접속자와 방은 동적으로 관리
- Item/Character: 입력·검증 경계와 중립 캐릭터 설정. Item 실제 효과는 미지원 거절

현재 Story의 표시 이름과 Stage 수는 개발용 콘텐츠입니다. 최종 대학생활·졸업·취업 소재는 담당자가 구체화합니다. 진행 기록은 앱 실행 중 유지되며 재시작 시 초기화됩니다.

## 후속 작업

실제 Render/Supabase 계정·SMTP 설정과 서로 다른 PC의 인터넷 검증, 경기 중 재접속 복구, Item 효과, 캐릭터 특수 능력, Save/Shop/Fever, 최종 이미지·BGM·Skin은 후속 작업입니다. 쉬움 난이도는 후속 고려 대상이며 현재 선택 UI와 배율 코드는 제외했습니다. 보스의 Mirror 모델도 제거했습니다. 현재 가중치와 HP가 등급별 실제 난이도 상승을 검증했다는 뜻은 아닙니다.

## 로컬 Online PvP

서버를 위 `Server` 명령으로 실행하고 게임을 두 번 엽니다. 두 창에서 **Online PvP · 로컬 서버 → 접속**을 선택합니다. 한쪽에서 방을 만든 뒤 표시된 방 번호로 다른 쪽이 입장하고, 양쪽이 준비하면 대전이 시작됩니다. 서버 기본 주소는 `127.0.0.1:28080`이며 같은 PC에서만 접속할 수 있습니다. 나가기·창 종료는 대전 중 기권 처리됩니다. 결과 화면에서 양쪽이 재대결 준비를 누르면 새 경기가 시작됩니다.

자세한 실행 방법과 검증 범위는 [로컬 PvP 안내](docs/local-pvp.md)를 참고합니다. 수정 전에 열어둔 게임 창에는 새 코드가 적용되지 않으므로 새로 실행해야 합니다.

## 조작

| 입력 | 동작 |
|---|---|
| ← / → | 좌우 이동 |
| ↑ / ↓ | 왼쪽 / 오른쪽 회전 |
| D | 한 칸 낙하 |
| Space | Hard Drop |
| C | HOLD |
| P | 로컬 일시정지/계속 |
| Esc | 홈 이동, PvE 일시정지 / PvP 대전 나가기 |

현재 보드는 10×22이며 중력은 400ms입니다. NEXT 3개·Ghost·Combo·간이 T-Spin을 유지합니다. SRS/Wall Kick/Mini 판정은 미구현입니다. 게임 규칙 변경 시 Core와 AI 배치 시뮬레이터를 함께 검증합니다.

## 설정 및 담당

| 위치 | 책임 | 담당 |
|---|---|---|
| app / app.session | 화면 조립·세션 수명주기·공통 API | 통합 담당 |
| core / battle / item / character | 게임·전투 규칙 | 전예랑 |
| ai / controller.AIController | 휴리스틱·정책·worker | 심은진 |
| network / auth / ranking / supabase | 로컬·WebSocket 서버, 인증·랭킹 서비스·프로토콜 | 심은진 |
| story | 콘텐츠·진행·해금 | 전예랑, 콘텐츠 공동 기획 조성은 |
| ui / resource | 화면·리소스 | 조성은 |

[Stage 설정](src/main/resources/story/stages.properties)은 HP와 aiProfileId를 참조합니다. [AI 프로필](src/main/resources/ai/profiles.properties)은 가중치·행동 간격·탐색 한도를 소유합니다. 같은 설정을 두 파일에서 중복 관리하지 않습니다.

## 검증과 기록

`Test`는 헤드리스 검증이며 임시 루프백 포트에서 실제 TCP·WebSocket 서버/클라이언트와 Swing 앱 통합 시험도 실행합니다. Supabase HTTP와 일부 저장소는 테스트 대역을 사용하며, 실제 DB 트랜잭션은 `supabase/tests/run_postgres_tests.py`로 별도 검증합니다. `Preview`와 `GuiTest`는 명시적인 `-AllowVisibleDesktop`이 필요합니다. `NetworkFixture`는 기존 Fake 재생용입니다. 실제 클라우드·장치의 네트워크, 화면 포커스·키 입력, 인간 대상 밸런스는 별도 검증입니다.

- [G0 검증 기록](docs/g0-verification.md)
- [로컬 PvP 구현·검증 기록](docs/local-pvp.md)
- [AI 인수인계](docs/handoff/ai.md)
- [Network 인수인계](docs/handoff/network.md)
- [Render 운영 안내](docs/render-operations.md)
- [Render 구현·로컬 검증 기록](docs/render-verification.md)
- [인증·랭킹 안내](docs/auth-ranking.md)
- [AI 정책 비교 기록](docs/ai-policy-verification.md)
- [Battle 인수인계](docs/handoff/battle.md)
- [Story 인수인계](docs/handoff/story.md)
- [UI 인수인계](docs/handoff/ui.md)

기존 `docs/phase0-*`, `phase1-*`, `phase2-*` 및 [이전 네트워크 설계](docs/network-design.md)는 당시 기준의 이력입니다. 현재 동작과 범위는 위 G0 문서와 로컬 PvP 안내를 우선합니다. `dev`는 팀 통합 기준이며 개인 개발은 해당 기준에서 분기합니다.

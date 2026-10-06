# Tetris Monster — 퍼즐 RPG

Java 8 / Swing 기반으로 상단 캐릭터·몬스터 전투와 하단 테트리스 플레이를 연결한 게임입니다. 이번 버전의 규칙은 [퍼즐 RPG 규칙](docs/puzzle-rpg-rules.md)을 기준으로 합니다. 팀별 시작 위치와 수정 범위는 [인수인계 시작점](docs/handoff/README.md), 입력·상태·스레드 계약은 [공통 계약](docs/contracts/g0-contracts.md)을 참고합니다.

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

`run.cmd` 또는 빌드된 `out/tetris.jar`로 실행할 수 있습니다. 빌드에는 JDK 8 이상이 필요하며 필요 시 `-JdkHome 'C:\path\to\jdk'`를 지정합니다. 실행 진입점은 `kr.ac.jbnu.se.tetris.Tetris.main()`과 `Main.main()`입니다. 빌드/테스트 산출물은 `out/g0`, 제품 JAR은 `out/tetris.jar`에 생성됩니다. 테스트용 Fake와 미리보기는 JAR에서 제외됩니다.

## 현재 실행되는 범위

- 튜토리얼: 이동·회전·낙하·HOLD·Hard Drop의 5개 조작 목표
- Story PvE: 5개 샘플 Stage × 일반·엘리트·보스, HP 0 또는 Top Out에 따른 승패
- 진행: 전투별 완료 기록, 보스 클리어 후 다음 Stage 해금, 최초 승리 보상과 로컬 진행 저장
- AI: 공통 휴리스틱 탐색·평가, 난이도 프로파일과 일반·엘리트·보스 행동 정책
- 세션: 로컬 게임 시계·AI 실행과 온라인 표시 계약 분리, 종료·재시작·늦은 콜백 처리
- UI: 픽셀 전투 무대, 격자·Ghost·HOLD·NEXT, HP·Fever·아이템·가비지 상태 표시
- Network: 로컬 TCP 서버·클라이언트, 방 생성·입장·준비·서버 판정·재대결·연결 종료 기권
- Item/Character: 8종 아이템의 실제 효과, PvE 4종 캐릭터·상점·장착, PvP 기본형 강제
- 전투 시간: 참가자별 중력, 가비지 2초 대기·4줄 분출·상쇄, Fever 10초·시간 왜곡 5초

현재 Story의 표시 이름과 Stage 수는 개발용 콘텐츠입니다. 진행·재화·캐릭터는 사용자 홈의 `.tetris-monster/save.properties`에 저장됩니다. 테스트는 임시 저장소 또는 메모리 상태를 사용합니다.

## 후속 작업

서로 다른 PC의 LAN·재접속·서비스 인증·랭킹·공개 배포, 최종 콘텐츠·BGM은 후속 작업입니다. 현재 가중치와 HP가 등급별 실제 난이도 상승을 검증했다는 뜻은 아닙니다. HP 승리와 Top-Out 승리의 비율은 실제 플레이 후 조정합니다.

## 로컬 Online PvP

게임 창의 **온라인 → 로컬 서버 시작**으로 서버를 켤 수도 있습니다. 이 경우 서버를 시작한 창을 닫으면 서버도 종료됩니다.

서버를 위 `Server` 명령으로 실행하고 게임을 두 번 엽니다. 두 창에서 **Online Battle**을 선택하면 기본 로컬 서버 `127.0.0.1:28080`에 연결합니다. 다른 주소는 **온라인 → 서버 연결...**에서 지정합니다. 한쪽에서 방을 만든 뒤 **온라인 → 방 번호 보기**로 실제 방 번호를 확인하고, 다른 쪽이 **온라인 → 방 번호로 입장...**으로 입장합니다. 양쪽이 READY를 누르면 대전이 시작됩니다. 입력한 방 이름은 해당 창의 표시용이며 실제 통신에는 서버의 방 번호를 사용합니다. 결과에서 대기방으로 복귀한 뒤 양쪽이 READY를 누르면 재대결합니다. 대전 포기·창 종료는 서버가 기권 처리합니다.

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
| 1 / 2 / 3 / 4 | 아이템 슬롯 사용 (자동 아이템은 공격/피격 때 발동) |
| Esc / 화면의 돌아가기 | 로컬 모드·전투 종료 후 선택 화면으로 복귀 (온라인은 로비, 진행 중 대전은 기권) |
| 기능 메뉴 → 대전 포기 | 현재 대전 기권 |

보드는 10×22입니다. 로컬 무한/Sprint는 400ms, PvP는 500ms부터 시작하며 첫 가속은 60초입니다. PvE는 난이도별 낙하 간격을 사용합니다. 전투 화면에서 격자·Ghost·HOLD·NEXT와 아이템 슬롯을 확인합니다. SRS/Wall Kick/Mini 판정은 미구현입니다. 게임 규칙 변경 시 Core와 AI 배치 시뮬레이터를 함께 검증합니다.

## 설정 및 담당

임시 UI는 사용자가 지정한 `University_Simulation`의 픽셀 패널·버튼 색상과 아바타 PNG를 차용합니다. [에셋 출처](src/main/resources/ui/university/README.md)에 원본 위치를 기록했습니다. 전투 상단의 돌아가기 버튼과 오른쪽 키 안내는 작은 창에서도 표시됩니다.

| 위치 | 책임 | 담당 |
|---|---|---|
| app / app.session | 화면 조립·세션 수명주기·공통 API | 통합 담당 |
| core / battle / item / character | 게임·전투 규칙 | 전예랑 |
| ai / controller.AIController | 휴리스틱·정책·worker | 심은진 |
| network | 로컬 서버·클라이언트·프로토콜·후속 LAN | 심은진 |
| story | 콘텐츠·진행·해금 | 전예랑, 콘텐츠 공동 기획 조성은 |
| ui / resource | 화면·리소스 | 조성은 |

[Stage 설정](src/main/resources/story/stages.properties)은 몬스터 이름·HP·aiProfileId를 소유합니다. [난이도 프리셋](src/main/java/kr/ac/jbnu/se/tetris/ai/DifficultyProfileCatalog.java)은 LEVEL_1~9의 낙하 간격·AI 행동 간격·탐색 한도·공격 강도를 지정하며 스토리에서 Stage HP와 등급을 결합합니다. [AI 프로필](src/main/resources/ai/profiles.properties)은 기본 평가 가중치와 최대 조정 비율을 제공합니다.

## 검증과 기록

이번 퍼즐 RPG 버전의 결과는 [MVP 검증 기록](docs/puzzle-rpg-verification.md)에 정리했습니다.

`Test`는 창·포커스 변경 없는 헤드리스 검증이며 임시 루프백 포트에서 실제 TCP 서버·클라이언트 통합 시험도 실행합니다. `Preview`와 `GuiTest`는 명시적인 `-AllowVisibleDesktop`이 필요합니다. `NetworkFixture`는 기존 Fake 재생용입니다. 실제 장치의 LAN, 화면 포커스·키 입력, 인간 대상 밸런스는 별도 검증입니다.

- [G0 검증 기록](docs/g0-verification.md)
- [로컬 PvP 구현·검증 기록](docs/local-pvp.md)
- [AI 인수인계](docs/handoff/ai.md)
- [Network 인수인계](docs/handoff/network.md)
- [Battle 인수인계](docs/handoff/battle.md)
- [Story 인수인계](docs/handoff/story.md)
- [UI 인수인계](docs/handoff/ui.md)

현재 실행 화면은 성은 브랜치 `0eee2c4`에서 가져온 패널을 기반으로 하는 `SeongeunApplication`입니다. 사용자가 승인한 퍼즐 RPG 전투 화면과 UI 안정화 작업을 반영했습니다. 로그인 화면의 로컬 시작은 로비로 이동하며 계정 인증·가입은 미연결입니다. Local Mode는 Infinite와 40줄 Sprint를 사용합니다. 화면 또는 기능 메뉴에서 튜토리얼·캐릭터 상점·로컬 서버를 이용합니다.

기존 `docs/phase0-*`, `phase1-*`, `phase2-*` 및 [이전 네트워크 설계](docs/network-design.md)는 당시 기준의 이력입니다. 현재 동작과 범위는 위 G0 문서와 로컬 PvP 안내를 우선합니다. `dev`는 팀 통합 기준이며 개인 개발은 해당 기준에서 분기합니다.

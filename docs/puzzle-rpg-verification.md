# 퍼즐 RPG MVP 검증 기록

검증일: 2026-10-06. 기준 브랜치: `codex/feature-puzzle-rpg-mvp`, 시작 커밋: `c1aca5d`.

## 입력·복귀와 임시 UI 추가 검증

초기 MVP 이후 키 입력과 전투 복귀를 보완하고, 사용자가 지정한 `University_Simulation`의 색상·픽셀 패널·아바타 PNG를 차용했다. 제품 소스 150개와 검증 소스 50개를 Java 8로 컴파일했고 전체 41개 테스트 모음을 통과했다. 로그는 `out/keyboard-ui-integration.log`다. 이 버전 JAR SHA-256은 `81B0316CD737F7202337A829BEE61B25DC1AC58805DA5AA863EA15C5E9FE1211`이다.

- 기존 보드의 키가 버튼과 JScrollPane 바인딩에 선점되는 문제를 별도 JFrame에서 재현했다. 수정 후 같은 창의 표시 중인 게임 보드만 조작 키를 먼저 받고, 숨긴 카드와 다른 창의 입력은 가져오지 않는 경로를 확인했다.
- `BoardKeyboardDesktopSmoke`는 실제 Swing 버튼 포커스와 KeyboardFocusManager 이벤트 전달을 검사한다. OS 키 입력을 주입하는 시험과는 구분한다.
- Esc·돌아가기의 로컬/스토리 세션 종료와 타이머 해제, 온라인 상대 기권 전달을 앱·실제 TCP 테스트에서 확인했다.
- 실제 JScrollPane에 담은 1160×780 및 760×660 렌더에서 상단 돌아가기와 오른쪽 키 안내가 표시되는 것을 확인했다. 최종 PNG는 `out/university-skin/battle-final-1160.png`와 `battle-final-760.png`다.
- PNG 3개의 원본과 복사본 해시를 대조했고 원본 프로젝트는 수정하지 않았다. 출처는 `src/main/resources/ui/university/README.md`에 기록했다.
- 독립 정적 리뷰에서 남은 확정 P1/P2는 없었다. 새 제품 창을 실행했으나 사용자 입력 감지로 Windows 자동 키 시험은 중단했다. 실제 OS 키 조작 전체를 검증했다고 주장하지 않는다.

## 초기 MVP 통합 결과 (`7687350`)

JDK 8u504로 제품 소스 149개와 검증 소스 49개를 컴파일하고 전체 41개 헤드리스 테스트 모음을 통과했다. 실제 루프백 TCP와 파일 저장을 허용한 실행 환경에서 검증했다.

```powershell
.\build.ps1 -Task Test -JdkHome 'C:\Users\User\AppData\Local\Temp\codex-tetris-baseline-jdk8\jdk8u504-b01'
```

최종 로그는 `out/rpg-final-verified.log`, 실행 JAR은 `out/tetris.jar`다. JAR SHA-256은 `F3C284B859455BB74EE5034E55A59D3117EA664D2234EE605C633355360FA0EB`다.

- Core: 아이템 미노 5개 주기, 회전·HOLD 원본 유지, 부분/동시 줄 제거 시 중복 획득 방지, 전투 착지 경계와 가비지 적용.
- Battle: 실제 8종 아이템 획득·자동/수동 사용·실패 시 보존, PC 예외, 가비지 2초 FIFO·상쇄·최대 4줄, Fever와 시간 왜곡 만료, 캐릭터 패시브, PvP 기본형 HP100·60초 첫 가속.
- Monster: 난이도 LEVEL_1~9, 스토리 HP 연결, 보스 67%/33% 단계 경계와 허용 가중치 조정폭, AI 세션 종료/일시정지.
- App/save: 로컬·스토리 진입, 기권·결과·복귀, 구매·장착·최초 보상 저장과 중복 방지, 저장 실패 후 다음 저장의 진행 복구, 잘못된 저장 거부, 전투 중 상점 메뉴 진입 차단, 모달 안내 중 로컬 시간 정지·입력 차단과 복귀.
- Network: 실제 두 클라이언트 방 생성·입장·준비·대전·결과·재대결·연결 종료, 서버의 실제 아이템 보유 검사, 확장 전투 상태와 미노 메타데이터 직렬화.

## 화면과 독립 리뷰

최종 제품 클래스로 `BattlePanelRenderSmoke`를 실행해 `out/rpg-ui/battle-panel-final.png`를 생성하고 확인했다. 1160×780 및 최소 960×660 렌더에서 상단 전투 무대, 격자, HOLD/NEXT, 아이템 슬롯, 하단 조작 안내가 표시된다. Sprint 완료는 보드 오버레이로 표시한다.

독립 정적 리뷰에서 전투 중 상점 이동과 모달 안내 뒤에서 전투가 계속되는 문제를 발견했다. 상점은 로비/상점에서만 허용하고, 로컬 모달 동안 기존 RUNNING 세션을 일시정지한 뒤 복귀하며, 온라인 전투는 비모달 안내를 사용하도록 수정했다. 두 수정의 회귀 검증을 포함한 전체 테스트를 다시 통과했다. 최종 정적 리뷰에서 남은 확정 P1/P2는 없었다.

## 검증 범위

헤드리스 렌더와 세션/API·실제 루프백 TCP 검증이다. 실제 데스크톱 키보드 포커스, 다른 PC의 LAN, 공개 서버/계정 서비스, 인간 대상 난이도·승리 비율 밸런스는 이번 결과로 입증하지 않는다. 게임 규칙은 `docs/puzzle-rpg-rules.md`를 따른다.

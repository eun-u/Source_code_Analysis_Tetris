# Frontend/UI 파트 인수인계

현재 프론트엔드는 Java Swing이다. 새 웹 프레임워크는 도입하지 않는다. UI는 확정 상태를 표시하고 입력 의도를 전달한다. HP·피해·승패·아이템 보유량의 직접 계산은 전투 계층 소유다.

## 현재 실행 화면: 성은 원본과 내부 로직 연결

현재 제품 진입점 `Tetris.main`은 `SeongeunApplication`을 실행한다. `origin/seongeun`의 `0eee2c4`에 있는 패널·컴포넌트·모델을 `ui/seongeun`에 복사하며 패키지 이름을 분리했다. 원본 생성자의 배치·폰트·간격·크기·기존 버튼을 유지하고 상태 표시와 입력 전달 API만 추가한다. `ui/seongeun/Board`는 원본 보드의 배경·블록 그리기 방식을 사용하며 게임 규칙과 시간 진행은 기존 Core/세션에 맡긴다.

원본에 조작 화면이 없는 튜토리얼, Stage 4·5, 서버 접속·방 번호 입장, 일시정지·포기 등은 별도 기능 메뉴와 대화상자로 접근한다. 기존 화면을 축소하거나 스크롤 구조로 바꾸지 않는다. 화면 가독성·배치 수정은 사용자가 확인 후 결정한다. 이번 연결 확인은 빌드와 실제 입력·세션·통신 기능으로 한정하며 미학 평가로 UI를 고치지 않는다.

원본 로그인 버튼은 로컬 로비 이동을 유지한다. 현재 계정 인증·가입 서비스는 연결되지 않았다. 원본의 가짜 방·가짜 READY·임의 승리 데이터는 실제 세션 상태로 교체하며, `Result Test`는 승패를 조작하지 않는다. 실제 서비스가 없는 Fever·아이템 효과·보상은 구현된 것으로 표시하지 않는다.

## 이전 UI 어댑터 이력 (현재 제품 화면에 사용하지 않음)

`origin/seongeun`의 `0eee2c4` 원본이 UI/UX 기준이다. `d8824b3`에서 변경했던 세로 메뉴·별도 Stage 선택/시작·로컬 플레이어 왼쪽 배치를 원본의 가로 메뉴·난이도 직접 선택·상대 왼쪽으로 복원했다. 화면은 현재 `src/main/java`의 `TetrisApplication`, `BoardView`와 세션 상태에 연결하며 공통 표시 컴포넌트는 `ui/components`에 위치한다.

로그인→로비, 회원가입→로그인, Story→난이도 선택→대전→결과→Story, Online→방 목록→대기방→대전→결과→대기방, Local Mode→Infinite/Sprint→결과→Local Mode 흐름을 사용한다. 결과 화면은 원본의 통계 다섯 줄과 복귀·로비 두 버튼이다. Back/대전 홈/결과 로비 버튼은 중단 세션의 타이머와 AI를 정리한다. 프로그램용 `showHome/continueGame` API는 기존 일시정지·재개 계약을 유지한다.

현재 콘텐츠는 5개 Stage다. 원본의 약 162px Stage 행 높이와 난이도별 버튼 배치를 유지하며 4·5 Stage는 스크롤로 접근한다. `CampaignProgress.isEncounterUnlocked`와 `StoryProgressService.startEncounter`가 선택한 난이도의 순차 해금 및 재도전을 검사한다. 로컬 Infinite는 TopOut까지, Sprint는 40줄까지 플레이하며 전투와 같은 `SevenBagGenerator`를 사용한다. 튜토리얼은 Local Mode의 보조 버튼으로 접근한다.

원본의 로그인·회원가입 화면은 유지하지만 인증은 아직 연결되지 않았다. 로그인 버튼은 비활성화하고 명시적인 `로컬로 시작`을 제공한다. 가입 버튼은 서비스 연결 준비 메시지를 표시하며 계정을 생성하지 않는다. 화면 이동/종료 시 비밀번호 입력을 지운다. 실제 인증은 은진 브랜치와 별도 통합한다. 서버는 방 목록 조회를 제공하지 않아 목록을 비워 두고 원본 목록 아래에 실제 TCP 접속·방 번호 입장 조작을 둔다. 대기방 카드와 준비 상태는 서버 확정 상태를 사용하며, 확정된 경기 종료 후 대기방에서 다시 READY를 눌러 재대결한다.

Fever와 아이템은 원본 위치에 미지원/EMPTY로 표시한다. 확인할 수 없는 레벨·캐릭터 이름·최대 콤보·누적 피해·보상은 빈 값이나 `—`로 표시한다. 실제 참가자 UUID는 카드 툴팁에 두고 원본의 Player/Enemy 역할 이름을 사용한다. 원본 전투 열은 작은 창에서 전체 보드를 동시에 볼 수 있도록 비율을 유지하며 축소하고, 3·4인 fixture만 필요시 가로 스크롤한다. 기본 창의 약 784×562 내부 영역에서는 이름·HP·아이템 글자도 약 6~8px로 작아지며, 960×820 렌더에서는 더 크게 보인다. 원본 배치와 전체 보드 가시성을 유지하기 위한 절충이며 실제 기본 창에서의 가독성은 수동 확인 대상이다. 구형 Board 타이머, 독립 UIPreview 실행기와 더미 판정은 제품에 포함하지 않는다.

`SeongeunUiIntegrationTest`는 가로 메뉴와 원본 버튼 흐름, 난이도 잠금, 실제 입력 전달, 서버 확정 준비 상태, HP 표시와 화면 퇴장 시 세션 종료를 검증한다. `StoryFlowTest`는 난이도 버튼으로 실제 15전투를 진행한다. `OnlineUiFlowTest`는 원본 로비·대기방·결과 복귀 버튼과 READY를 통해 실제 TCP 대전·재대결을 검증한다. `LocalGameSessionTest`는 실제 엔진의 40줄 완주, TopOut과 시드 재현을 검증한다. `OffscreenUiTest`는 실행 화면과 fixture를 800×600, 760×680, 960×820, 784×562로 렌더링한다. 마지막 크기는 창 테두리 안쪽의 더 작은 영역에 대한 확인이며 실제 창의 포커스·물리 키 입력은 별도 검증 대상이다.

## 서버 없이 시작

```powershell
.\build.ps1 -Task Preview -AllowVisibleDesktop
```

`Preview`는 현재 제품과 같은 `SeongeunApplication`을 실행한다. 온라인에는 별도 로컬 서버가 필요하다. 이전 `src/test/.../ui/UiPreviewMain`은 기존 어댑터의 Stage 잠금, 2/3/4명 참가자, 방 준비, 승리·패배·연결 실패 샘플을 남긴 개발 fixture이며 현재 제품 화면이 아니다.

`support.OnlinePreviewScenario.running()`은 FakeNetworkClient와 OnlineMatchSession을 생성한다. 네트워크 상태를 바꾸려면 Fake에 NetworkUpdate를 emit하고 drain한다. 실제 통신 구현이 없어도 같은 UI 계약으로 작업할 수 있다.

## 표시·입력 경계

- BattlePanel.setState(BattleState, localParticipantId, canPause, thinking): ID별 참가자 View 구성
- RoomPanel.setState(RoomState): 방 참가자·ready 상태 표시, Consumer<RoomCommand>으로 요청 전달
- StageSelectPanel: Stage ID와 잠금 조회 함수, 시작 콜백 주입
- SessionUiBinding.bind(MatchSession, Consumer<SessionUpdate>): EDT 전달·구독 해제·늦은 요청 결과 처리
- ResultPanel: 세션이 확정한 결과의 표시와 다음 행동
- AssetManager: classpath 리소스 조회와 누락 대체

반환된 요청 ID로 pending 상태를 등록하려면 원시 온라인 subscribe 대신 SessionUiBinding을 사용한다. 요청을 보냈다는 사실만으로 버튼 성공·HP 감소를 표시하지 않는다. close/구독 해제 뒤 예약 콜백은 화면에 반영하지 않는다.

제품 홈의 Online 버튼은 실제 로컬 TCP 서버를 사용하는 OnlineLobbyPanel로 연결한다. Fake는 제품 화면에 연결하지 않는다. 접속·방 생성·번호 입장·양측 준비·대전·결과·재대결을 지원한다. 방 화면의 나가기는 leave 요청이며 대전 중 홈/창 종료는 연결 종료에 따른 서버 기권 처리다. PvP는 일시정지하지 않는다. 자세한 실행 순서는 [로컬 PvP 안내](../local-pvp.md)를 따른다.

## 담당 작업과 완료 기준

담당 작업은 화면 배치·캐릭터/몬스터 리소스·정보 표시·전환·입력 UX다. 공통 컴포넌트가 필요할 때 ui/component로 분리한다. 현재 파일을 이름만 바꾸기 위해 일괄 이동하지 않는다.

서버 없이 샘플로 개발한 화면을 Local/Online 세션에 연결한다. G0UiContractTest·OffscreenUiTest·UiTest가 인원/ID·화면·입력을 검증하고 OnlineUiFlowTest가 실제 TCP와 두 앱의 로비·대전·결과·재대결·종료를 검증한다. 실제 창의 크기·포커스·키 입력·연출 검증은 담당자가 별도로 수행한다. 헤드리스 검증만으로 최종 사용성 검증 완료를 선언하지 않는다.

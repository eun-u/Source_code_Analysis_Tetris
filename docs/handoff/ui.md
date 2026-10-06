# Frontend/UI 파트 인수인계

현재 프론트엔드는 Java Swing이다. 새 웹 프레임워크는 도입하지 않는다. UI는 확정 상태를 표시하고 입력 의도를 전달한다. HP·피해·승패·아이템 보유량의 직접 계산은 전투 계층 소유다.

## 성은 UI 통합

`origin/seongeun`의 `0eee2c4` 화면 구성을 현재 `src/main/java`의 실행 화면에 이식했다. 로비 메뉴, Stage 행, 참가자 카드와 HP 바, 대전·결과 화면은 현재 `TetrisApplication`과 세션 상태를 사용한다. 공통 표시 컴포넌트는 `ui/components`에 위치한다.

구형 `src/kr/.../Board`와 별도 `UIPreview` 실행기, 더미 방·레벨·결과·3단계 진행 모델은 제품에 포함하지 않는다. 현재 엔진의 `BoardView`, 5개 `StageCatalog`, 진행 서비스와 서버 판정을 유지한다. 계정 인증·회원가입은 현재 `dev`의 기능이 아니며 은진 브랜치와 별도 통합한다. 방 목록 조회, Infinite/Sprint, 아이템·Fever 조작도 현재 세션이 지원할 때 연결한다.

`SeongeunUiIntegrationTest`는 화면의 실제 입력 전달, Stage 잠금, 서버 확정 준비 상태, 참가자 카드 교체와 HP 표시를 검증한다. `OnlineUiFlowTest`는 홈·접속·방 생성·입장 버튼으로 실제 TCP 대전을 시작한다. `OffscreenUiTest`는 로컬 접속 화면을 포함한 실행 화면과 fixture를 760×680, 960×820으로 렌더링한다. 실제 창 포커스와 물리 키 입력은 별도 검증 대상이다.

## 서버 없이 시작

```powershell
.\build.ps1 -Task Preview -AllowVisibleDesktop
```

`src/test/.../ui/UiPreviewMain`은 Stage 잠금, 2/3/4명 참가자, 방 준비, 승리·패배·연결 실패 샘플을 제공한다. 조작은 의도 로그만 남기므로 실제 대전·실제 연결로 오해하지 않는다. 3/4인 표시는 자료구조 검증용이며 첫 온라인 출시 인원은 별도 정책이다.

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

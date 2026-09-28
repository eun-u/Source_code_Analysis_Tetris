# Network 파트 인수인계

## 현재 실행 범위

로컬 TCP 서버와 실제 클라이언트, Swing 온라인 로비·방·대전 화면이 연결되어 있다. 같은 PC에서 서버 한 개와 게임 창 두 개를 실행해 방 생성→입장→준비→1:1 대전→결과·재대결을 진행할 수 있다. 서버는 **127.0.0.1에만 바인딩**하므로 서로 다른 PC의 LAN 대전은 아직 지원하지 않는다. 개발용 `FakeNetworkClient`와 샘플 데이터는 `src/test`에만 있다.

| 영역 | 기준 파일 | 책임 |
|---|---|---|
| 사용자 입력 | `core/PlayerIntent.java` | 사용자의 행동 종류와 Item payload, 실행자 ID·순번 제외 |
| 공통 대전 | `app/session/MatchSession.java` | 로컬/온라인의 입력·표시·구독·종료 계약 |
| 온라인 변환 | `app/session/OnlineMatchSession.java` | NetworkUpdate를 SessionUpdate로 변환, 요청 ID 대응, 경기 교체 검증 |
| 실제 클라이언트 | `network/TcpNetworkClient.java` | TCP 송수신, 구독자별 순서 보장, 요청 큐와 종료 처리 |
| 네트워크 경계 | `network/NetworkClient.java` | 접속·방 명령·입력·전체 Snapshot 요청·수신 구독 |
| 로컬 서버 | `network/server/LocalGameServer.java` | 방·참가자 ID·400ms 중력·전투·승패의 최종 소유자 |
| 통신 형식 | `network/protocol/WireRequest.java`, `WireCodec.java` | 버전 1 JSON 요청·갱신과 길이 지정 TCP 프레임 |
| 방/메시지 | `network/RoomCommand.java`, `RoomState.java`, `NetworkUpdate.java`, `RequestOutcome.java` | UI와 실제 클라이언트가 공유할 값 객체 |
| 테스트 대역 | `src/test/.../support/FakeNetworkClient.java`, `OnlinePreviewScenario.java` | 송신 기록과 수신 순서의 결정적 재생 |

서버가 `BattleManager`로 전투 규칙과 승패를 판정하고 클라이언트는 전달받은 `BattleState`를 표시한다. 서버의 현재 방 정책은 **정확히 2명**이다. `RoomCommand.createRoom`은 2~4명을 표현할 수 있지만 서버는 2 이외의 요청을 `ROOM_SIZE_NOT_SUPPORTED`로 거절한다. 방 내부의 참가자·준비 상태는 ID 기반 Map이며 PC별 고정 변수는 없다.

## 명령과 결과

```text
UI PlayerIntent
  → MatchSession.submit() → localRequestId
  → TcpNetworkClient.send() → networkRequestId
  → LocalGameServer → BattleManager → RequestOutcome
  → SessionUpdate.CommandOutcome(localRequestId)

서버 BattleState Snapshot
  → WireCodec → NetworkUpdate.snapshot(roomId, matchId, BattleState)
  → OnlineMatchSession 검증
  → SessionUpdate.snapshot
```

`submit`, `requestPause`, `leave`의 반환값은 요청 ID이며 게임 내 성공을 뜻하지 않는다. 확정 성공·거절은 `CommandOutcome`으로 전달한다. `OnlineMatchSession`은 실행 중이 아닌 입력을 `SESSION_NOT_RUNNING`, Item 사용을 `ITEM_NOT_IMPLEMENTED`, 온라인 일시정지를 `PAUSE_NOT_ALLOWED`로 거절한다. `MatchSession.subscribe`를 직접 사용하면 로컬·온라인의 결과 콜백이 요청 ID 반환보다 먼저 실행될 수 있다. 직접 구독은 허용하지만 UI에서 요청 ID를 먼저 등록해야 한다면 `SessionUiBinding`의 EDT 전달을 사용한다.

`RoomCommand`는 생성, 입장, 준비, 나가기, 재대결, 방 상태 요청의 여섯 종류다. 클라이언트는 경기 입력에 matchId와 증가하는 요청 ID를 붙인다. 서버는 **연결별 요청 ID가 이전보다 커야 함**을 확인하고, 해당 소켓에 결합된 참가자 ID로만 `BattleManager`에 입력한다. 클라이언트가 actorId·피해량·HP·보드를 제출하는 통신 필드는 없다. `PlayerIntent`는 서버 전용 `START`, `GRAVITY_TICK`, `RECEIVE_GARBAGE`, `PAUSE`, `RESUME`를 허용하지 않는다. Item 효과는 아직 구현되지 않았으며 온라인 세션의 Item 사용 요청은 `ITEM_NOT_IMPLEMENTED`로 거절한다.

통신 프레임은 **4바이트 big-endian 길이 + UTF-8 JSON 본문**이다. 본문은 버전 1 envelope를 사용하고 크기는 1~1,048,576바이트로 제한한다. `WireCodec`은 분할·연속 프레임을 읽으며 잘못된 길이·UTF-8·JSON·버전·중복 또는 미정의 필드를 거절한다. `actorId`나 `damage` 같은 임의 필드도 거절한다. 최초 버전은 전체 Snapshot을 전달하고 Delta는 구현하지 않았다. `NetworkUpdate.EVENTS`는 wire에서 명시적으로 미지원이며 서버는 Snapshot과 RequestOutcome을 전송한다.

## 방·경기 전환 순서

1. `CONNECTED`와 `RoomState` 수신
2. 현재 방 ID·로컬 참가자 ID가 일치하고 방 버전이 뒤로 가지 않는 `MATCH_STARTED` 수신
3. 해당 경기 ID의 첫 전체 `SNAPSHOT` 수신, `BattleState.version=0` 허용
4. 같은 경기의 더 높은 버전 Snapshot으로 화면 갱신
5. 새 `MATCH_STARTED` 수신 시 이전 경기 대기 요청 정리, 예전 경기 Snapshot·이벤트 폐기

`MATCH_STARTED` 없이 들어온 Snapshot은 채택하지 않는다. 동일 경기 ID에서 `FINISHED` 이후 `RUNNING`으로 되돌리는 Snapshot도 채택하지 않는다. 네트워크 이벤트는 경기 ID와 증가하는 eventId로 중복 표시를 막는다. `BattleState.version`은 경기마다 다시 시작하므로 새 경기의 0을 이전 경기의 마지막 버전과 비교하지 않는다. 최종 경기 시작 메시지는 더 높은 roomVersion과 새 matchId를 요구한다.

재접속 절차는 아직 없다. `CONNECTION_FAILED`, `ERROR`, `CLOSED`를 받으면 세션은 `FAILED`가 되고 미완료 요청을 실패 처리한다. 이후 도착한 방 상태·경기 시작·Snapshot·이벤트·요청 결과는 채택하지 않는다. 서버에서 경기 중 나가기 또는 연결 끊김이 발생하면 해당 참가자를 **FORFEIT 탈락**으로 판정하고 남은 연결에 종료 Snapshot을 전달한다. 다음 경기는 종료 뒤 두 참가자가 재대결 준비를 마쳤을 때 새 matchId로 시작한다.

현재 방의 식별자·단계·참가자별 ready 여부는 `RoomState`에 있다. 전체 방 목록 API는 후속 서버 구현 범위다. 보드·HP·승패는 `BattleState`에 있다. 연결 전에는 세션의 `localParticipantId`, `matchId`, `BattleState`가 null일 수 있다. `RUNNING`과 `FINISHED`에서는 로컬 참가자가 포함된 `BattleState`가 필수다.

## 콜백과 종료 계약

네트워크 수신 콜백은 EDT 실행을 보장하지 않는다. 실제 클라이언트는 한 연결의 갱신을 순서대로 전달하고, 내부 네트워크 잠금을 보유한 채 리스너를 호출하지 않는다. UI 바인딩이 EDT로 전달한다. 구독 즉시 현재 상태가 먼저 전달되고, 구독 해제 뒤 예약된 이전 구독 콜백은 화면에서 폐기한다.

`close()`는 반복 호출 가능하며 새 요청은 `CLOSED` 오류로 거절한다. 대기 중인 요청은 `CLOSED` 결과로 정리하고 구독을 해제한다. 실제 클라이언트는 송신·수신·콜백을 분리하고 큐 크기를 제한한다. 서버도 연결 수·대기 요청·발신 갱신 수를 제한하며 단일 방 작업 큐에서 400ms 중력 tick과 입력 순서를 관리한다. `FakeNetworkClient.drain()`은 콜백을 네트워크 잠금 밖에서 전달한다. `OnlineMatchSessionTest`에는 수신 스레드와 UI 요청·종료가 겹치는 교착 회귀 시험이 있다.

## 로컬 PvP 실행

프로젝트 루트에서 PowerShell 창 세 개를 사용한다. 서버 창은 다음 명령으로 켜 둔다.

```powershell
.\build.ps1 -Task Server -Port 28080
```

클라이언트 창 두 개에서 각각 다음 명령으로 게임을 실행한다.

```powershell
.\build.ps1 -Task Run
```

두 게임 창의 **Online PvP** 화면에서 `127.0.0.1:28080`에 접속한다. 첫 창에서 **방 만들기**를 누른 뒤 표시된 방 ID를 두 번째 창의 **입장** 입력란에 넣는다. 두 참가자가 **준비**하면 서버가 경기를 시작하고 양쪽에 같은 전투 상태를 보낸다. 종료 뒤 두 참가자가 **재대결**을 요청하면 새 경기 ID와 새 보드로 시작한다. 서버를 종료하거나 창을 닫으면 해당 연결의 경기는 종료된다.

실제 통신 검증은 `network.protocol.WireCodecTest`, `network.server.LocalGameServerTest`, `network.TcpNetworkClientTest`가 담당한다. 프레임 분할·병합·악성 필드, 두 소켓의 방·입력·동일 상태, 중복 요청·오래된 matchId, 나가기·연결 종료·재대결을 검사한다. `NetworkContractTest`와 `OnlineMatchSessionTest`는 공통 계약과 상태 전이를 확인한다. 테스트 전용 `OnlinePreviewScenario`와 `FakeNetworkClient`는 서버 없이 UI 상태를 재생하는 예제이며 실제 통신 검증과 구분한다.

## 후속 개발 경계

현재 서버는 loopback 전용이다. **서로 다른 PC의 LAN 대전, 외부 인증 서비스, 재접속·경기 복구**는 구현되지 않았다. LAN 전환에는 서버 바인드 주소와 클라이언트의 서버 주소 입력, 네트워크 경계 설정, 서로 다른 PC 실측 검증이 필요하다. 재접속에는 기존 연결의 참가자 식별·요청 순번·경기 상태를 다시 결합하는 별도 계약이 필요하다. 공개 네트워크 접속 또는 계정 기반 인증은 현재 로컬 개발 서버의 검증 범위 밖이다.

일반 2명 방 정책을 확장할 때는 UI의 두 화면만 늘리는 작업으로 끝나지 않는다. 서버의 방 정원·타깃 선택·결과·퇴장 규칙을 함께 정해야 한다. 데이터 구조는 참가자 ID Map을 계속 사용한다.

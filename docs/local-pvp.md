# 로컬 서버 Online PvP

2026-09-28. G0 공통 계약 위에 같은 PC의 두 클라이언트가 실제 TCP로 대전하는 기능을 추가했다. 기존 Fake 실행과 별개이며 제품 홈의 Online 버튼에서 사용한다.

## 실행 순서

프로젝트 폴더에서 서버용 터미널을 연다.

```powershell
.\build.ps1 -Task Server -Port 28080
```

`127.0.0.1:28080` 대기 메시지를 확인한 뒤 `run.cmd`를 두 번 실행한다. 새 빌드 전부터 열려 있던 게임은 새 코드가 적용되지 않으므로 새로 실행한다.

1. 두 게임 창에서 `Online PvP · 로컬 서버` 선택
2. 두 창 모두 포트 `28080` 확인 후 `접속` 선택
3. 첫 창에서 `방 만들기` 선택 후 표시된 방 번호 확인
4. 두 번째 창에서 해당 방 번호 입력 후 `입장` 선택
5. 양쪽이 `준비` 선택 시 대전 시작
6. 결과 화면에서 양쪽이 `재대결 준비` 선택 시 새 경기 시작

서버는 `Ctrl+C`로 종료한다. 종료하면 클라이언트에 연결 종료가 표시된다. 대전 중 `Esc`나 홈 버튼·창 종료는 서버의 기권 처리이며 상대가 승리한다. PvP는 P키로 정지하지 않는다. 재대결은 두 참가자가 같은 방에 남아 있어야 한다. 혼자 남은 경우 홈으로 돌아가 새 상대와 방을 만든다.

기본 서버는 IPv4 루프백 주소에만 바인딩하므로 다른 PC에서는 접속할 수 없다. 방화벽 변경은 필요하지 않다. 첫 구현의 방 정책은 1:1이며 저장 구조는 참가자 ID 기반 Map이다. PC 번호별 변수는 사용하지 않는다.

## 구현 경계

| 영역 | 위치 | 역할 |
|---|---|---|
| 실행 | `build.ps1 -Task Server`, `LocalGameServer.main` | 루프백 서버 실행·종료 |
| 서버 | `network/server/LocalGameServer` | 참가자 ID 할당, 방, 단일 작업 큐, 400ms 중력, BattleManager 판정 |
| 전송 | `network/TcpNetworkClient` | 비차단 송신, 수신 순서 유지, 연결 종료 |
| 프로토콜 | `network/protocol` | 4바이트 길이 + UTF-8 JSON v1, 최대 1MiB, 엄격한 필드·숫자·상태 검증 |
| 세션 | `OnlineMatchSession` | 요청 ID 연결·현재 경기 상태·늦은 메시지 폐기 |
| 화면 | `OnlineLobbyPanel`, `RoomPanel`, 기존 Battle/Result | 접속·방·대전·재대결·종료 표시 |

클라이언트는 PlayerIntent와 요청 ID·현재 matchId를 보내며 actorId·HP·피해·보드를 보내지 않는다. 서버는 연결에서 참가자 ID를 결정한다. 중력·피해·승패는 기존 BattleManager가 계산하고 두 클라이언트에 전체 Snapshot을 보낸다. 같은 연결의 요청 ID는 단조 증가하며 중복/역순과 구경기 요청을 거절한다. 요청 결과와 방 상태·경기 시작·Snapshot의 순서를 유지한다.

연결과 송수신 큐에 상한이 있으며 느린 연결이 서버의 방 작업자를 소켓 쓰기로 정지시키지 않도록 분리했다. v1의 별도 EVENTS 전송은 미지원이며 필요한 화면 상태는 Snapshot으로 전달한다. 서버 로그인/계정 인증·재접속·외부 공개 서버·LAN·Delta 상태는 후속 작업이다.

## 검증

`build.ps1 -Task Test`에서 JDK 8로 운영 소스 103개와 테스트/지원 소스 35개를 컴파일하고 **29개 테스트 묶음**을 통과했다. 실제 TCP 테스트는 포트 0으로 임시 포트를 배정하고 종료 시 서버와 클라이언트를 닫는다.

- WireCodecTest: 분할/연속 프레임, 길이·UTF-8·버전·중복/알 수 없는 필드, actorId/피해 주입 거절, 상태 사본 왕복
- LocalGameServerTest: 두 소켓의 생성·입장·준비·입력·같은 상태 수신, 중복/구경기 요청 거절, 기권·Top Out·양측 재대결
- TcpNetworkClientTest: 실제 클라이언트와 OnlineMatchSession 두 개, 서버 확정 HOLD·종료 기권·방 재사용·접속 실패
- OnlineUiFlowTest: 실제 서버와 두 Swing 앱의 로비·대전·Top Out 결과·재대결·홈 이동, 클라이언트 중력/AI 타이머 비사용
- 기존 Story·AI·Core·Battle·UI 회귀 테스트

현재 검증은 실제 TCP 통신과 헤드리스 Swing 흐름까지다. 두 물리 PC의 LAN이나 OS 창 포커스·실제 키보드 입력·최종 화면 디자인 검증은 포함하지 않는다. 로그는 `out/g0/local-pvp-test.log`, 종료 코드는 `out/g0/local-pvp-test-exit.json`에 저장한다. Git commit/push는 수행하지 않았다.

## AI 난이도 설정 위치

`src/main/resources/ai/profiles.properties` 상단 주석에 조절 방향·허용 범위·적용 방법을 기재했다. `delayMillis`를 낮추면 행동 간격이 짧아지고, `line/fourLineBonus`를 높이면 줄 삭제 선호가 증가한다. `holes/aggregateHeight/maximumHeight/bumpiness/wells`의 음수 절댓값을 키우면 해당 위험의 감점이 커진다. 탐색 범위와 시간은 `maxSearchStates/budgetMillis`로 조절한다.

같은 프로필을 참조하는 몬스터에는 함께 적용된다. 특정 몬스터만 바꾸려면 프로필을 복제해 `profiles` 목록에 등록하고 `story/stages.properties`의 해당 `aiProfile` 값을 변경한다(예: `stage.1.normal.aiProfile`). 코드의 필드·접근자는 `aiProfileId`/`getAiProfileId()`다. HP는 Stage 설정의 몬스터 `hp`에서 바꾼다. 설정 변경 후 재빌드·재실행이 필요하다. FIXED의 `maxWeightDeltaRatio`만 변경해도 가중치가 자동 변화하지 않으며, 실제 난이도 상승 여부는 별도 플레이 평가 대상이다.

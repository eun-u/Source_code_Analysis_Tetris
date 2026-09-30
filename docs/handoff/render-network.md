# Render 네트워크 인수인계

## 실행 경로

`RenderGameServer`는 `0.0.0.0:$PORT`에서 HTTP `/healthz`와 WebSocket `/ws`를 제공한다. `/healthz`는 단순 응답이며 절전 방지 작업이나 외부 호출은 없다. Render에서 TLS를 종료하고 데스크톱 클라이언트는 `wss://.../ws`를 사용한다. `ws://`는 명시적인 localhost 개발 주소에서만 허용한다. 기존 `LocalGameServer`와 `TcpNetworkClient`는 로컬 회귀 경로로 남는다.

클라이언트는 `new ConnectionOptions(URI, accessToken)`과 `WebSocketNetworkClient`를 사용한다. 기존 `NetworkClient`의 `connect/send/subscribe` 계약은 같다. WebSocket 텍스트 프레임에는 버전 1 JSON 본문만 넣는다. TCP의 4바이트 길이 접두어는 WebSocket에 넣지 않는다. `WireCodec`의 필드·형식 검증을 공통으로 재사용하고 분할 프레임을 합친 뒤 최대 1MiB를 적용한다.

## 인증과 한도

`Authorization: Bearer <Supabase access token>`은 업그레이드 때 별도 인증 작업 스레드에서 검증한다. 검증된 계정 UUID를 참가자 ID로 묶고 중복 연결을 거절한다. 인증 전 연결은 18초 안에 완료되어야 한다. 각 방은 1:1이며 접속자와 방을 동적으로 관리한다. 예상 접속 4명은 제품 상한이 아니다. 자원 보호를 위한 `TETRIS_MAX_WS_CONNECTIONS`(기본 128, 인증 대기 포함)와 `TETRIS_MAX_PENDING_AUTH`(기본 16)는 운영 환경 변수로 조정한다. 전체 TCP 한도는 게임 연결 한도에 운영 요청용 여유 8개를 더하여 게임 연결이 찬 상태에서도 health/admin 요청을 받는다. 요청 대기·송신 대기·초당 요청에도 각각 상한이 있다. 클라이언트의 actorId·HP·보드 필드는 검증 경계에서 거절한다.

`NetworkClient.refreshAuthentication(newToken)`은 기존 연결에 새 토큰을 보내고 요청 ID를 반환한다. 서버는 같은 계정인지 별도 인증 스레드에서 확인한 뒤 `REQUEST_OUTCOME`으로 답한다. 계정별 갱신은 한 번만 동시에 처리한다. 토큰 만료 전 갱신하지 못하면 서버가 연결을 닫는다. 정상 연결 중에만 ping/pong을 보낸다. 연결 종료 뒤 세션 복원은 없다.

클라이언트는 WebSocket 업그레이드를 최대 90초 기다리고, 연결 후 60초 동안 서버 메시지와 ping이 전혀 없으면 연결 장애로 종료한다. 업그레이드의 HTTP 401은 `AUTH_INVALID`, 503은 `SERVICE_UNAVAILABLE`로 전달한다. 이 타이머는 연결 중에만 동작하며 서버를 깨우는 외부 요청은 만들지 않는다.

운영 여유 소켓은 정상 게임 연결이 한도에 도달한 상황을 위한 것이다. HTTP 헤더를 보내지 않는 TCP 연결의 읽기 타임아웃은 이번 수정 범위에 포함하지 않았으며, 그런 연결로 전체 TCP 한도가 채워지는 경우까지 관리자 가용성을 보장하지는 않는다.

## 운영 제어

서버는 처음에 경기 접수를 닫고 시작한다. `RENDER_ADMIN_TOKEN`은 24자 이상이어야 한다. 운영 API는 `Authorization: Bearer <RENDER_ADMIN_TOKEN>`이 필요하다.

| 요청 | 용도 |
|---|---|
| `GET /admin/status` | 실행 ID, 접수·drain 상태, 계정·방·진행·미확정 수와 최대 16개 미확정 경기 ID 조회 |
| `POST /admin/open` | 신규 경기 접수 시작 |
| `POST /admin/drain` | 신규 접수 차단, 기존 경기 정리 대기 |
| `POST /admin/recover-stopped-run?runId=<old UUID>` | 이전 실행이 실제 종료된 것을 확인한 뒤 해당 실행의 미확정 DB 경기 무효화 |

새 서버를 시작한 사실만으로 이전 실행의 경기를 무효화하지 않는다. 배포 전 `/admin/drain` 뒤 `active=0`과 `unresolved=0`을 확인하고 이전 인스턴스 종료를 확인한다. 새 실행에서 이전 실행의 미확정 경기를 정리하고 `/admin/open`을 호출한다.

원격 등록 결과를 끝내 확인하지 못한 경우에는 DB에 행이 없더라도 `unresolved>0`을 보수적으로 유지한다. 이때는 신규 접수를 닫고 활성 경기가 모두 끝난 뒤 **해당 이전 서버 실행의 종료를 확인**한다. 새 서버에서 그 실행 ID로 `/admin/recover-stopped-run`을 호출한다. DB의 `server_runs` 잠금은 먼저 시작한 등록이 끝날 때까지 기다린 뒤 경기를 무효화하며, STOPPED로 기록한 실행 ID의 이후 등록을 거절한다. 복구와 DB 확인을 마친 다음 접수를 연다. 이 경로는 정상 drain의 `unresolved=0` 조건에 대한 수동 복구 절차이며, 실행 중인 다른 서버 ID에는 호출하지 않는다.

## 경기와 저장

두 참가자가 준비하면 서버는 경기 ID와 계정 ID 두 개를 `RankedMatchStore.beginMatch`에 먼저 기록한다. 등록에 성공한 현재 방과 참가자가 그대로 있을 때만 `BattleManager`를 시작한다. 등록 중 떠난 경우 늦게 완료된 등록은 해당 경기 ID로 무효화한다. 다른 경기의 콜백이 새 방 상태를 변경하지 못하도록 방 세대와 경기 ID를 확인한다.

정상 종료와 명시적 나가기는 서버가 결정한 승자를 `finishMatch`에 저장한다. 결과 저장 상태는 `RANKED_SAVE_STATUS`의 `SAVE_PENDING`, `SAVED`, `SAVE_FAILED`, `VOIDED`로 전달한다. 저장 확인 전에는 재대결이 거절된다. 저장 요청 응답이 유실되면 경기 ID로 조회하여 확정 여부를 확인한다. DB의 계정별 활성 경기 잠금도 이전 결과가 정리되기 전 새 경기 시작을 막는다.

한쪽 연결이 끊기면 5초 뒤 서버와 상대 연결이 유효할 때만 몰수패를 판정한다. 양쪽 중단, 서버 tick 실패, 승자를 정할 수 없는 종료는 무효 처리한다. 무효가 DB에서 확정되면 `VOIDED`와 종료 오류 `MATCH_VOIDED`를 순서대로 전송해 화면이 진행 중 상태에 머무르지 않도록 한다. 결과 저장을 끝내 확인할 수 없으면 `SAVE_FAILED`와 `MATCH_RESULT_UNRESOLVED`를 보낸다. 이 경우 운영자가 DB 상태를 확인해야 한다.

## 설정과 검증

`RenderGameServer.main`은 `PORT`, `RENDER_ADMIN_TOKEN`, `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY`를 읽는다. 마지막 비밀 키는 서버 환경에만 둔다. 모든 원격 인증·DB 호출은 전투 tick 작업 큐 밖에서 실행한다.

`RenderGameServerTest`는 Java 8 loopback에서 실제 WebSocket으로 6계정·3방 동시 대전과 각 경기 저장, 한 경기 종료 후 다른 두 경기 유지, 분할·잘못된·초과 크기 프레임, 인증 타임아웃과 설정 가능한 자원 보호, 만료 전 토큰 갱신, 등록 취소 콜백, DB 저장 응답 유실, 저장 중 재대결 차단, 무효 경기 종료를 검증한다. 가짜 인증/저장 구현을 쓰므로 Render와 Supabase 실계정 종단 검증은 별도로 필요하다.

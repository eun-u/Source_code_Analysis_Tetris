# 온라인 대전 설계 초안

> 이전 설계 이력. 현재 G0 계약과 첫 구현 범위는 [공통 계약](contracts/g0-contracts.md) 및 [Network 인수인계](handoff/network.md) 참조. Delta·재접속·기존 epoch 형식은 구현 완료 사항이 아닌 후속 검토 대상.

2026-09-28. 이 문서는 서로 다른 PC의 2인 대전을 위한 구현 계약이다. 현재 실행 코드에는 서버, 클라이언트, 로그인 인증, 원격 대전 화면이 없다. 로컬 `BattleManager`는 ID별 2~4명 참가자와 각각의 공통 `GameEngine`을 관리하므로 서버의 규칙 엔진으로 재사용한다.

## 소유권과 데이터 흐름

서버가 `GameRoom`의 `List<PlayerSession>`(초기 최대 2명, 자료구조와 전투 생성은 2~4명 확장 가능), `BattleManager`, 게임 시계, 난수 시드, 전투 결과를 소유한다. 클라이언트는 자기 키 입력을 `GameAction` 의도로 전송하고 서버가 배포한 상태와 이벤트만 표시한다. 원격 플레이어의 보드도 서버의 같은 `GameEngine`에서 진행한다. UI는 직접 피해, Garbage, HP, 아이템 효과를 계산하지 않는다.

```text
Swing 입력 → NetworkController → Action 메시지 → 서버 Room 작업 큐
                                              ↓
                                     BattleManager.submit/tick
                                              ↓
                    BattleEvent + STATE_DELTA + Snapshot → 클라이언트 표시
```

한 방의 모든 입력과 고정 간격 tick은 단일 작업 큐에서 처리한다. 그 큐 밖에서 방의 전투 상태를 바꾸지 않는다. 서버가 정한 시드로 참가자별 `SeededPieceGenerator`를 만들며, Garbage 구멍도 서버가 생성한다. 시드는 디버깅과 재현을 위한 내부 값이고 클라이언트의 신뢰 근거가 아니다. 방이 시작될 때 참가자 목록과 순서를 확정한다. 관전자, 지연 보상, 재접속과 3~4인 대상 선택 규칙은 별도 단계에서 명시한다.

## 프로토콜 계약

- TCP 위에 `4-byte big-endian 길이 + UTF-8 JSON` 프레임을 사용한다. 프레임 길이는 1~262144바이트로 제한하고, 길이·JSON 구조·필드 타입·문자열 길이 검증에 실패하면 연결을 종료한다. Java 객체 직렬화는 사용하지 않는다.
- 모든 메시지는 `protocolVersion`, `type`, `roomId`, `matchEpoch`, `messageId`를 가진다. 정수의 범위와 필수 필드는 메시지 타입별 스키마로 검증한다. 알 수 없는 버전은 명시적으로 거부하고, 알 수 없는 타입은 처리하지 않는다.
- `JOIN`, `LEAVE`, `READY`, `START_REQUEST`, `ACTION`, `SNAPSHOT_REQUEST`, `REMATCH_REQUEST`, `PING`을 클라이언트 명령으로 둔다. 서버는 `ROOM_STATE`, `MATCH_STARTED`, `ACTION_ACCEPTED/REJECTED`, `EVENT_BATCH`, `STATE_DELTA`, `SNAPSHOT`, `MATCH_RESULT`, `DISCONNECTED`, `PONG`을 보낸다.
- `ACTION`은 `actorSequence`, `actionType`, 필요한 경우 검증된 아이템 ID·대상 셀만 담는다. 현재 원격 입력 허용 목록은 이동, 회전, SOFT_DROP, HARD_DROP, HOLD이다. `USE_ITEM`은 전투 계층의 아이템 소유·대상 검증이 완성된 후에만 허용한다. `START`, `PAUSE`, `RESUME`, `GRAVITY_TICK`, `RECEIVE_GARBAGE`는 서버 전용이다. 클라이언트가 보낸 `actorId`, 피해량, HP, 보드, 시드, Garbage 수는 신뢰하지 않는다.
- 서버는 소켓의 `PlayerSession`과 참가자 ID를 결합한다. 세션·`matchEpoch`·명령 허용 목록·입력 빈도를 먼저 검증한다. 유효한 순서의 요청은 `BattleManager`에서 충돌로 `ACTION_REJECTED`가 나와도 마지막 **처리 순번**을 전진시킨다. 그렇지 않으면 해당 순번에서 다음 정상 입력이 영구적으로 막힌다. 신원/epoch/형식이 틀린 패킷은 순번을 소비하지 않는다. 중복 순번은 재실행하지 않고 epoch·세션·순번으로 캐시한 기존 응답을 다시 보낸다. 역순·미래 순번은 명시적으로 거부한다. 재연결을 허용할 경우 새 세션의 순번 재설정 절차를 먼저 정의한다.
- 서버가 확정한 `BattleEvent`에는 기존 `eventId`와 전투 `version`을 실어 순서대로 전송한다. 이벤트는 공격·사운드·로그용 의미 기록이다. 현재 `BattleEvent`만으로는 이동 후 활성 블록 위치, HOLD/NEXT, 보드 전체 변경을 재구성할 수 없으므로 클라이언트 렌더링의 유일한 상태 근거로 쓰지 않는다. `ACTION_ACCEPTED`는 입력 접수와 서버 상태 확정을 구분하는 응답이다.
- 서버는 수락한 명령과 tick마다 전투 버전이 붙은 `STATE_DELTA`를 보낸다. DTO는 변경된 보드 셀과 참가자별 활성 블록·위치, HOLD/NEXT, HP, Fever, 아이템, 종료 상태 중 변한 필드를 담는다. 빈 변화라도 버전 연속성을 표현한다. 클라이언트는 버전/epoch를 확인해 순서대로 적용하고 규칙을 다시 실행하지 않는다. 버전 또는 이벤트 번호가 끊기면 즉시 Snapshot을 요청한다. 동일 버전과 이벤트 번호는 한 번만 적용한다.
- 서버는 시작 시, 재동기화 요청 시, 그리고 예를 들어 2초마다 전체 `Snapshot`을 보낸다. 2초 주기는 복구 안전망이며 매 명령의 화면 갱신은 `STATE_DELTA`가 담당한다. Snapshot DTO에는 방/epoch/version/eventId, 참가자별 보드 셀·활성 블록·HOLD/NEXT·HP·Fever·아이템·종료 상태를 넣는다. 구현된 필드만 버전별로 명시한다. `GameEngine`이나 Swing 객체를 직렬화하지 않는다. 클라이언트 예측은 별도 검증 후 도입한다.

## 방 상태 전이와 실패 처리

`LOBBY → READY → RUNNING → RESULT → REMATCH_READY → RUNNING`을 기본 경로로 둔다. READY는 각 `PlayerSession`별 플래그이며, 초기 2인 모두 준비해야 서버가 새 epoch와 시드를 정해 시작한다. 방장 권한으로 다른 사람의 준비 상태를 대신 변경할 수 없다. RESULT가 되면 그 epoch의 Action을 받지 않는다. 재대결은 새 epoch, 새 시드, 초기화된 순번과 전투 상태로 시작한다. LEAVE는 로비에서는 자리 반환, RUNNING에서는 포기 판정과 명시적 결과 이벤트를 만든다.

연결이 끊기면 서버는 heartbeat/읽기 타임아웃으로 이를 감지하고 `DISCONNECTED` 이벤트를 모든 참가자에게 보낸다. 초기 버전은 제한된 유예 시간 뒤 연결이 돌아오지 않으면 기권 처리한다. 유예 시간 동안 서버 tick을 멈출지 계속할지, 계정 인증 기반 재접속을 허용할지는 정책과 테스트를 정한 뒤 구현한다. 사용자의 창을 닫을 때 클라이언트는 비동기 IO를 종료하고 UI는 서버 결과를 기다리는 무한 대기를 하지 않는다.

## 스레드, 보안, 배포 경계

소켓 읽기/쓰기와 JSON 파싱은 Swing EDT 밖에서 처리한다. 읽기 스레드는 검증된 메시지를 방 작업 큐에 넣고, 서버 큐가 단독으로 전투를 변경한다. 클라이언트는 수신 DTO를 불변 상태로 변환한 뒤 `SwingUtilities.invokeLater`로 화면을 갱신한다. 느린 소켓 쓰기가 서버 전투 큐를 막지 않도록 세션별 한도 있는 송신 큐를 둔다.

계정 로그인이 구현되기 전에는 로비 접속자에게 서버가 발행한 임시 세션 ID만 부여한다. 이는 신원 인증이 아니다. 인터넷 공개 배포에는 TLS, 계정 인증·권한, 재접속 토큰의 만료와 재사용 방지, 요청 제한, 방 코드의 추측 방지, 로그의 개인정보 최소화가 필요하다. 개발용 LAN 연결도 임의 패킷을 신뢰하지 않는 동일 입력 검증을 사용한다. 포트를 자동으로 열거나 공개 서버를 시작하지 않는다.

## 구현 순서와 검증 경계

1. 와이어 DTO·프레임 코덱·버전/길이/필드 검증을 만들고 잘린 프레임, 과대 프레임, 미지 타입, 중복 순번 테스트를 한다.
2. 서버 `GameRoom` 상태 전이, 2인 READY/START/RESULT/REMATCH, Action 허용 목록, 세션 ID 결합과 접속 종료 테스트를 한다. `BattleManager`의 기존 2~4인 API를 재사용하고 아이템/전투 규칙은 복제하지 않는다.
3. `NetworkController`와 로비/방 화면을 연결하고 Delta 누락/역순/중복, 즉시 Snapshot 복원, 이벤트 순서, EDT 갱신을 테스트한다. 충돌로 거부된 정상 순번 뒤의 다음 Action과 중복 요청 응답 재전송도 검증한다.
4. 별도 두 프로세스 loopback에서 경기 완료, 지연/역순 입력, 갑작스런 연결 종료를 확인한다. 이는 서로 다른 PC 검증과 구분해 기록한다.
5. 서로 다른 PC의 동일 LAN에서 방 생성/입장, 준비, 장시간 경기, 결과, 재대결, 연결 종료와 재접속 정책을 검증한다. 실제 인터넷 배포 전에는 TLS/인증/방화벽 환경의 별도 검증이 필요하다.

현재 `BattleManager.submit(actorId, GameAction.Type)`은 서버가 만든 ID와 Action 종류를 받는다. 와이어 프로토콜 도입 때는 서버 입력 검증기와 아이템 payload를 수용할 전투 API를 계약화한 뒤 확장한다. 현재 API에 임의 클라이언트 payload를 직접 전달하지 않는다.

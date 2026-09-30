# Render 배포·운영 인수인계

## 2026-09-30 배포 상태

[Render 서비스](https://dashboard.render.com/web/srv-dau62g2d0e5s73ed59g0)와 [Supabase 프로젝트](https://supabase.com/dashboard/project/gadzyccwxnxzdmdgjptx)를 생성하고 연결했다. 서버 주소는 `https://tetris-ranked-pvp.onrender.com`, 게임 연결은 `wss://tetris-ranked-pvp.onrender.com/ws`이다. 실제 로그인·인터넷 대전·기권·랭킹 저장을 확인했고 테스트 계정과 경기 데이터는 정리했다. 근거는 [배포 검증 기록](render-verification.md)에 있다.

주소가 설정된 클라이언트는 `out/tetris-online-client.zip`이다. 압축을 풀고 `play.cmd`를 실행한다. Java 8 이상이 필요하다. 일반 사용자 가입·비밀번호 복구 메일은 custom SMTP와 복구 코드 템플릿을 설정한 뒤 사용할 수 있다. SMTP는 사용자 요청에 따라 나중에 설정한다. 현재 메일 인증 설정은 켜져 있다.

검증 직후 대전 접수를 열어두었다. 무료 서비스의 절전·재시작 후에는 아래 운영 절차에 따라 접수를 다시 열어야 하며, 주기적인 절전 방지 요청은 설정하지 않았다.

## 준비된 구성

- Java 8 Swing 클라이언트와 headless Java WebSocket 서버. Render에는 서버만 배포한다.
- Render Free Web Service 1개, Singapore. 예상 동시 접속은 약 4명이지만 사용자 수·방 수를 이 숫자에 고정하지 않는다. 각 방은 1:1이다.
- 무료 요금제에서는 `maxShutdownDelaySeconds`를 지정하지 않는다. 실제 Blueprint 검증에서 이 설정을 거절하므로 기본 종료 유예시간을 사용한다. 재배포 전 경기 종료·저장 확인과 실행 복구 절차는 그대로 따른다.
- Supabase Auth + PostgreSQL. 공식 랭킹은 온라인 PvP 결과만 반영한다.
- 서버에는 HTTP `/healthz`, WSS `/ws`, 인증된 관리자 경로가 있다. 외부 주기 요청·절전 방지 예약 작업은 없다.
- 로컬 TCP 연습 서버는 유지하며 랭킹에 반영되지 않는다.

## 로컬 빌드

```powershell
.\build.ps1 -Task Test       # main/-ea headless 전체 회귀 + 배포 JAR
.\build.ps1 -Task Server     # 기존 loopback TCP 서버
.\build.ps1 -Task CloudServer # 실제 환경 변수 설정 후 HTTP/WS 서버
```

JDK 8 이상이 필요하다. `-JdkHome <JDK 경로>`도 지원한다. Maven은 Wrapper가 SHA-256을 검증해 다운로드하고, 의존성 버전은 `pom.xml`에 고정한다.

- `out/tetris.jar`: 플레이어용 단일 실행 JAR. 서버 진입점과 서버 전용 저장 구현을 제외한다.
- `out/tetris-server.jar`: 운영 서버용 JAR.
- `./mvnw verify` / Windows `mvnw.cmd verify`: 동일한 테스트와 `target/tetris-client.jar`, `target/tetris-server.jar` 생성.
- Linux Docker 검증: `docker build -t tetris-ranked .`. 빌드 단계에서도 전체 headless 테스트를 수행한다. Docker 엔진이 없는 환경에서는 실제 이미지 빌드 검증과 구분한다.
- GitHub의 `eunjin` 브랜치에서 서버 소스·Docker 빌드 파일이 바뀌면 `Validate Render container`가 같은 Dockerfile로 Linux 빌드와 테스트를 수행한다. 이후 실제 컨테이너의 health·관리자 인증·비루트 실행을 확인한다. 외부 서비스 키는 사용하지 않는다.

## 실제 계정에서 필요한 설정

1. **Supabase 프로젝트:** 프로젝트를 생성하고 `supabase/migrations/202609290001_ranked_pvp.sql`을 SQL Editor에서 적용한다. `supabase/tests/bootstrap.sql`은 로컬 시험용이므로 실제 프로젝트에 적용하지 않는다.
2. **메일:** 이메일 확인을 켠다. 실제 4명의 주소로 보내는 custom SMTP를 연결한다. 비밀번호 복구 템플릿에는 일회용 코드 `{{ .Token }}`을 표시한다. 자세한 내용은 [인증·랭킹 안내](auth-ranking.md)를 따른다.
3. **Render Blueprint:** 이 변경이 반영된 Git 브랜치를 Render에 연결하고 `render.yaml`을 선택한다. Free, Singapore, Docker, 자동 배포 꺼짐을 확인한다.
4. **환경 변수:** Render에 `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY`를 넣는다. `RENDER_ADMIN_TOKEN`은 Blueprint가 생성한다. `PORT`는 Render가 제공한다. 비밀값은 채팅·Git·클라이언트 파일에 넣지 않는다.
5. **초기 확인:** 배포 로그의 서버 실행 ID를 기록하고 `/healthz`가 200인지 확인한다. health 응답은 DB 연결이나 경기 접수 개방을 의미하지 않는다. 운영자는 아래 관리자 명령으로 현재 실행 ID와 접수 상태를 확인한다.
6. **접수 개방:** 첫 배포에는 이전 실행이 없다. 재배포에는 이전 실행 종료와 미확정 경기 정리를 먼저 확인한 뒤 `/admin/open`을 호출한다.
7. **클라이언트:** `tetris-client.properties.example`을 실행 폴더의 `tetris-client.properties`로 복사하고 공개 값 세 개만 채운다. `server.url=wss://서비스명.onrender.com/ws`, Supabase URL, publishable key다. secret/admin key는 넣지 않는다.
8. **실서비스 검증:** 실제 네 계정으로 메일 인증·복구, 로그인, 두 방 동시 대전, 기권·연결 종료, 랭킹 1회 반영, 재로그인과 서버 재시작 후 기록 유지 확인. 로컬 대역 시험은 이 검증을 대신하지 않는다.

클라이언트 설정 우선순위는 `-Dtetris.server.url` 등의 JVM 속성 → 환경 변수 → UTF-8 설정 파일이다. 환경 변수는 `TETRIS_SERVER_URL`, `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`다. 앱 토큰은 메모리에만 보관하므로 재실행 시 로그인한다.

서버 자원 보호 설정은 `TETRIS_MAX_WS_CONNECTIONS`(기본 128, 인증 대기를 포함한 게임 WebSocket 연결 수), `TETRIS_MAX_PENDING_AUTH`(기본 16, 인증 완료 대기 요청 수)다. 예상 사용자 수와 분리한 운영값이며 Render 환경 변수로 변경한다. 게임 연결이 한도에 도달해도 `/healthz`와 관리자 요청을 받을 수 있도록 전체 TCP 한도에는 여유 8개를 더 둔다. 이 값은 해당 수의 실제 대전 성능을 보장하지 않는다. 별도의 사용자 4명·방 2개 제한은 없고, 방은 참가자의 생성·나가기 동작에 따라 관리한다.

## 관리자 명령

운영자 셸에 `RENDER_ADMIN_TOKEN`을 설정한 뒤 다음 스크립트를 사용한다. 토큰은 요청 헤더로만 전송하며 URL에 포함하지 않는다.

```powershell
.\scripts\render-admin.ps1 -ServerUrl https://서비스명.onrender.com -Action status
.\scripts\render-admin.ps1 -ServerUrl https://서비스명.onrender.com -Action drain
.\scripts\render-admin.ps1 -ServerUrl https://서비스명.onrender.com -Action open
```

`status`의 `runId`, `active`, `unresolved`를 확인한다. 재배포 절차는 다음과 같다.

1. 구 실행을 `drain`하여 새로운 방/경기 시작을 막는다.
2. `active=0`, `unresolved=0`을 확인하고 구 실행 ID를 기록한다. 미확정 결과가 있으면 저장 실패를 먼저 해결한다.
3. 구 서버가 완전히 종료됐음을 Render에서 확인한다. 새 배포가 시작됐다는 사실만으로 구 실행 종료를 판단하지 않는다.
4. 새 서버는 접수를 닫고 기동한다. 종료 확인을 한 운영자가 다음 복구 요청을 실행한다. 현재 RUNNING 행이 보이지 않더라도 구 실행 ID를 DB에서 STOPPED로 확정하여 늦게 도착하는 등록을 차단한다.

```powershell
.\scripts\render-admin.ps1 -ServerUrl https://서비스명.onrender.com -Action recover `
  -StoppedRunId <종료를-확인한-구-실행-UUID> -StoppedRunConfirmed
```

5. 상태와 DB 결과를 확인하고 새 실행을 `open`한다. 이미 확정된 경기의 레이팅은 복구 작업으로 되돌리지 않는다.

복구와 경기 등록은 같은 `server_runs` 행을 잠근다. 먼저 시작한 등록 트랜잭션이 있으면 복구가 기다린 뒤 해당 경기를 무효화하며, 복구가 먼저 완료되면 이후 그 실행 ID의 등록을 거절한다. 이 차단 상태는 DB에 남으므로 같은 구 실행 ID로 복구를 반복해도 늦은 등록이 다시 허용되지 않는다.

등록/저장 응답을 끝내 확인하지 못한 경우에는 DB에 행이 보이지 않아도 `unresolved`가 남는다. 이때 `unresolved=0`을 무한히 기다리거나 접수를 다시 열지 않는다. `drain`을 유지하고 진행 경기가 끝났는지 확인한 뒤 `runId`와 상태에 표시된 미확정 경기 ID를 기록한다. 해당 실행을 완전히 중지하고, 접수가 닫힌 새 실행에서 기록한 구 실행 ID로 `recover`를 수행한다. DB의 해당 경기들을 재조회하여 지연 등록 또는 진행 상태가 남지 않은 것을 확인한 후에만 새 접수를 연다. 이미 확정된 결과는 보존하고, 확정할 수 없는 이전 경기는 무효 처리한다.

무료 서비스 유휴 정지나 예기치 않은 재시작 뒤에도 **운영자가 접수를 다시 개방**해야 한다. 이 버전은 실행 간 리더 선출·자동 복구를 구현하지 않았으며, 새 실행이 살아 있는 다른 실행의 경기를 무효화하지 않도록 닫힌 상태로 시작한다.

## 결과 저장과 장애

경기는 DB 시작 등록 후에만 실행한다. 결과 저장 중에는 재대결을 막으며, 응답 유실은 동일 경기 ID로 조회/재시도한다. 저장 재시도 한도에 도달하면 UI에 미확정으로 표시한다. 운영자는 원인을 해결하고 서버 실행 종료를 확인한 뒤 미확정 경기를 정리한다.

명시적 나가기는 기권이다. 단일 연결 종료는 짧은 판정 유예 후 처리하며, 양쪽 종료·판정 오류 등 결과를 확정할 수 없는 경우에는 무효 처리한다. 경기 중 재접속에 따른 상태 복원은 제공하지 않는다. 서버가 결과를 DB에 확정하기 전에 소실되면 그 결과를 복원할 수 있다고 보장하지 않는다.

## 제출 시점

외부 주기 요청은 사용자 요청대로 지금 설정하지 않는다. 제출할 때 무료 서비스 정책과 종료일을 확인하고 별도 설정한다. 실제 클라우드 지연·메모리·4인 동시 부하와 사람 대상 AI 난이도는 현장 검증 결과를 별도로 기록한다.

공식 설정 근거: [Render Blueprint](https://render.com/docs/blueprint-spec), [Render Docker](https://render.com/docs/docker), [Render 무료 제한](https://render.com/docs/free), [배포 수명주기](https://render.com/docs/deploys), [Supabase SMTP](https://supabase.com/docs/guides/auth/auth-smtp).

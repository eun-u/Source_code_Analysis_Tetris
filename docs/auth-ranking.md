# 로그인·온라인 PvP 랭킹 연동

Java 8 데스크톱은 Supabase publishable key로 Auth와 읽기 전용 랭킹 RPC에 접근한다. Render 게임 서버는 별도 환경 변수의 secret key로 경기 시작·종료·무효화 RPC만 호출한다. 이 키를 데스크톱 배포물, Git 또는 로그에 넣으면 안 된다. 코드는 값이 없으면 서비스 시작을 거부한다.

## 설정과 적용 순서

1. Supabase 프로젝트를 만들고 `supabase/migrations/202609290001_ranked_pvp.sql`을 SQL Editor에 적용한다. `profiles`, `player_stats`, `matches`, `match_participants`와 RPC가 생성된다.
2. 이메일 확인을 켜고 실제 사용자 4명의 메일이 수신되는 custom SMTP를 연결한다. 기본 SMTP만으로는 프로젝트 조직 밖 사용자에 대한 출시 검증이 끝나지 않는다.
3. Auth의 비밀번호 복구 이메일 템플릿에 `{{ .Token }}`을 표시한다. 데스크톱의 `completePasswordRecovery(email, otp, newPassword)`는 메일의 일회용 코드를 `/auth/v1/verify`에 제출한 뒤 새 비밀번호를 설정한다. 템플릿이 링크만 보내면 이 화면을 통해 복구할 수 없다.
4. 데스크톱에는 프로젝트 URL과 publishable key만 설정한다. 예: `new SupabaseConfig(url, publishableKey)`. Render 서버에는 `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY`를 비밀 환경 변수로 설정한다. 게임 서버의 `SupabaseConfig.fromEnvironment()`가 이를 읽는다.
5. 로그인·랭킹·서버 연결을 실제 계정 네 개로 확인한다. 메일 확인, 복구 코드, 토큰 만료/갱신, 2개 방의 경기 확정과 랭킹 1회 반영, 서버 재시작 후 조회를 각각 점검한다.

`SupabaseAuthService`의 호출은 동기 HTTP 요청이므로 Swing EDT 밖에서 실행한다. `signUp`, `signIn`, `refresh`, `signOut`, `recoverPassword`, `completePasswordRecovery`, `changePassword`가 제공된다. 세션의 access/refresh 토큰은 메모리에만 둔다. 앱 재시작 후에는 다시 로그인한다.

게임 서버의 `SupabaseTokenVerifier`는 토큰의 issuer, audience, role, subject, expiry를 확인하고 프로젝트의 `/auth/v1/user` 응답으로 서명 유효성과 계정 일치를 검증한다. 인증 서비스 연결 실패는 인증 성공으로 간주하지 않는다. 검증은 네트워크 I/O이므로 게임 tick 밖에서 실행한다.

## 경기 저장 규칙

- `beginMatch`는 두 계정의 진행 중 경기 잠금과 경기 등록을 한 DB 트랜잭션에 묶는다. 같은 경기 ID와 인자를 재전송하면 기존 상태를 반환한다. 다른 활성 경기가 있으면 시작을 거절한다.
- `finishMatch`는 저장된 두 계정과 서버 실행 ID를 확인하고 승자·Elo·전적·활성 경기 해제를 한 트랜잭션으로 처리한다. 초기 Elo 1000, K=32, 절반값은 PostgreSQL numeric `round()` 규칙이다. 이미 확정된 동일 결과 재전송은 전적을 다시 올리지 않는다.
- `voidMatch`는 시작 대기 취소 또는 판정 불가 장애에 사용한다. 무효 경기는 전적과 레이팅에 반영되지 않는다. 확정 경기의 무효화는 거절한다.
- `getMatch`는 저장 응답이 사라졌을 때 해당 경기의 실제 DB 상태를 확인한다. `voidStoppedRun`은 **해당 서버 실행이 완전히 종료됐음을 확인한 뒤에만** 호출한다. `server_runs` 행을 `STOPPED`로 확정하고 해당 실행의 진행 경기를 무효화하는 한 트랜잭션이므로, 늦게 도착한 이전 실행의 `beginMatch`는 거절된다. 이미 확정된 결과는 유지한다. 새 인스턴스 시작만으로 이전 실행의 경기를 무효화하지 않는다.
- 사용자 클라이언트는 `ranked_leaderboard`만 호출할 수 있다. 확정 경기가 한 번 이상 있는 계정 중 상위 100명을 표시하고, 동률은 공동 순위다. 직접 결과/레이팅 수정과 서버 전용 RPC 호출은 DB 권한에서 차단한다.

로컬 PostgreSQL 16/17 검증은 `TETRIS_TEST_POSTGRES_DSN`에 loopback PostgreSQL 관리용 연결 문자열을 지정하고 `python supabase/tests/run_postgres_tests.py`를 실행한다. Python에는 psycopg 3가 필요하다. 스크립트가 고유한 격리 DB를 만들고 `bootstrap.sql` → 마이그레이션 → `ranking.sql`을 적용한 뒤, 동시 중복 확정·중복 시작·계정 중복 시작·확정/무효화 경합과 시작/복구 양방향 순서 경합을 검사하고 해당 DB만 삭제한다. 실제 Supabase에는 `bootstrap.sql`을 적용하지 않는다. Java의 `SupabaseAuthTest`, `SupabaseRankingTest`는 로컬 HTTP 서버로 API 요청·오류·DTO를 검증한다.

2026-09-29 검증: Java 8 + Gson 2.13.2에서 두 HTTP stub 테스트 통과. 로컬 PostgreSQL 17.6에서 SQL 스키마/권한/순차 결과, 공동 순위, 동시 중복 저장, 서버 실행 중단과 늦은 경기 시작의 경합, 여러 경기/계정의 잠금 순서가 통과했다. 실제 메일 전달과 클라우드의 API key 동작은 실제 프로젝트에서 별도 확인해야 한다.

공식 자료: [Auth REST API](https://github.com/supabase/auth/blob/master/openapi.yaml), [API keys](https://supabase.com/docs/guides/getting-started/api-keys), [JWT 검증](https://supabase.com/docs/guides/auth/jwts), [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security).

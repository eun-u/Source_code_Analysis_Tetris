# Render 구현·배포 전 검증 기록

작성일: 2026-09-29, 갱신일: 2026-09-30. 작업 기준: `eunjin` / 변경 전 HEAD `68b43f3`, Linux 검증 커밋 `eec0f2b741f23392b75f71adb7cf118f1ff14ee4`. 이 기록은 구현과 로컬·CI 검증을 설명하며 실제 Render/Supabase 배포 완료 기록이 아니다.

## 구현

- HTTP/WebSocket 게임 서버, WSS 클라이언트, Supabase 계정 검증과 같은 계정 토큰 갱신.
- 각 방은 1:1이며 사용자·방 수는 동적 관리. 예상 접속 4명을 상한으로 두지 않으며 소켓·인증 대기 수는 별도 운영 설정. 프레임·요청·발신 상한, 보호된 운영자 제어.
- 경기 시작 등록, 결과·Elo 원자적 저장, 중복 확정 방지, 응답 유실 조회, 저장 중 재대결 차단, 무효 경기 처리.
- 이메일 가입·로그인·로그아웃·갱신·비밀번호 복구, 계정/랭킹 화면, 저장 상태 표시. 세션 토큰은 메모리에만 보관.
- 일반 FIXED·엘리트 ADAPTIVE·보스 BOSS 정책. 기존 탐색기와 전체 입력 수락 후 정책 상태 확정 계약 유지.
- Maven Wrapper, Java 8 빌드, 클라이언트/서버 JAR, Dockerfile, Render Blueprint, 공개 클라이언트 설정 예시와 운영 스크립트.
- 절전 방지용 주기 요청·cron·외부 모니터 등록은 없음.

## 검증 환경과 결과

| 항목 | 실행 근거 | 결과 |
|---|---|---|
| 변경 전 기준 | 기존 `build.ps1 -Task Test` | 29개 headless suite 통과 |
| 통합 회귀 | JDK 8u504, Maven 3.9.11, `build.ps1 -Task Test` | 35개 headless suite 통과 |
| WebSocket | 실제 loopback Netty 서버·클라이언트, 인증/저장 대역 | 6계정·3방 동시 대전과 각 결과 저장, 한 경기 종료 후 다른 경기 유지, 토큰 갱신, 프레임 오류/분할/상한, 인증 시간·자원 보호, 경기 저장 경합 검증 |
| 앱 종단 | `RankedOnlineUiFlowTest` | 두 Swing 앱에서 로그인→방→대전→저장 대기→확정→랭킹 표시, EDT 응답성·종료 검증 |
| 계정 수명주기 | `OnlineAccountControllerTest` 및 `SupabaseAuthTest` | 지연 중 EDT 응답, 토큰 갱신, 영구/일시 인증 실패 구분, 계정 전환·종료 후 응답 폐기 |
| 실제 PostgreSQL | 임시 loopback PostgreSQL 17.6 + `supabase/tests/run_postgres_tests.py` | 8개 검증 항목 통과: 스키마·권한/RLS·Elo·공동 순위, 중복 시작/종료, 계정 중복 경합, 확정/무효화 경합, 종료 실행의 지연 등록 차단과 잠금 순서 |
| 배포용 클라이언트 | 테스트를 `target/tetris-client.jar`에 대해 실행 | 로그인 서비스·화면 동작, 서버 구현 제외 후 공통 코드 참조 확인 |
| 배포용 서버 | `out/tetris-server.jar`를 별도 Java 8 프로세스로 실행 | PORT 반영, `/healthz=200`, 접수 닫힘, 관리자 무인증 요청 401 |
| Linux Docker | GitHub Actions Ubuntu 24.04, 운영 Dockerfile 빌드와 실제 컨테이너 실행 | 35개 headless suite 통과, `/healthz=200`, UID 10001, 접수 닫힘·미확정 기록 0건, 관리자 무인증 요청 401 |
| 화면 | 최소 760×680 및 기본 960×820 크기 offscreen 렌더링 | 실제 창을 열지 않고 계정·로비 포함 8개 앱 화면과 기존 preview 확인 |
| Render 설정 | 공식 Blueprint JSON Schema로 `render.yaml` 검증 | Free 1개·Singapore·자동 배포 꺼짐·health 경로·예약 작업 없음 |
| 파일 검사 | `git diff --check`, PowerShell parser, JAR 내용 확인 | 공백 오류 없음, 운영/패키징 스크립트 구문 통과, 테스트·로컬 설정 파일 배포물 제외 |

의존성은 Netty 4.2.18.Final, Gson 2.13.2로 고정했다. Maven Wrapper 배포 ZIP의 SHA-256 검증을 설정했다. 로컬 PostgreSQL은 저장소의 무시된 `.tools` 아래에만 준비했으며, 임시 테스트 DB를 정리하고 서버를 종료했다.

2026-09-30 [Linux 컨테이너 검증 실행](https://github.com/eun-u/Source_code_Analysis_Tetris/actions/runs/36649879391)이 성공했다. Docker 빌드 이미지에 `unzip`을 추가하여 Maven Wrapper가 검증 대상 ZIP을 그대로 사용하도록 했고, 체크섬 검증은 유지했다. 컨테이너 기동 확인은 로컬 포트가 준비될 때까지 유한 횟수 재시도한다. Supabase에는 대역 설정만 사용하므로 이 결과가 실제 Auth·Data API 연결을 증명하지는 않는다. 실행 로그는 `out/render-linux-ci-36649879391.log`에도 보관했다.

Linux 검증 중 발견한 `LocalGameServerTest`의 경기 종료 경합도 수정했다. 다음 입력 전에 중력 tick이 경기를 끝낸 경우 새 snapshot에서 `FINISHED`를 확인한다. 최소 한 번의 HARD_DROP 성공, 종료 후 입력 거절, 재대결 초기화와 이전 경기 ID 거절 검증은 유지했다. 수정된 테스트는 로컬에서 5회 연속 통과했고 독립 검토를 거쳤다.

전체 Maven 로그는 재생성 가능한 `out/render-final-tests.log`, PostgreSQL 검증 로그는 `out/ranked-postgres-tests.log`, 화면 이미지는 `out/g0/ui`, 배포 서버 기동 로그는 `out/server-jar-smoke.log`에 있다. 로그와 빌드 산출물은 Git에서 제외한다.

최종 `out/tetris-client.zip`은 JAR·공개 설정 예시·실행 스크립트·사용 안내의 네 파일만 포함하며, 내부 JAR가 검증한 `out/tetris.jar`와 동일한 것을 확인했다. Windows PowerShell에서 한글 안내가 깨지지 않도록 관련 스크립트의 UTF-8 BOM을 적용하고 재패키징했다. 최종 클라이언트 JAR로 계정 흐름을 다시 실행했고, 서버 JAR로 PORT·health·닫힌 접수·관리자 인증을 확인했다. 세 배포 산출물의 크기와 SHA-256은 `out/artifact-checksums.json`에 기록했다.

## 검토에서 반영한 경계

예상 접속 인원 4명을 제품 상한으로 잘못 해석했던 사용자 4명·방 2개 차단을 제거했다. 1:1 규칙과 동일 계정 중복 접속 방지는 유지하고, 소켓·인증 대기 자원 보호값은 별도 운영 설정으로 분리했다. 약 4명은 배포 비용·성능 검토와 실제 환경 시험의 예상 부하다.
작은 WebSocket 한도 2개를 주입한 회귀에서 두 계정이 접속한 동안 추가 게임 연결은 거절하고 health·관리자 상태·drain 요청은 허용하는 것을 확인했다. 연결 종료 후 슬롯 재사용과 인증 대기 한도 1개에서 타임아웃 후 회복도 확인했다.

독립 검토에서 인증 갱신 영구 실패, 동시 접속 슬롯 경합, 이전 경기 등록·무효화 콜백의 상태 오염, 인증 요청에 의한 DB 작업 지연, 무효 경기 후 화면 정지, 저장 상태의 실패 화면 누락을 확인하고 수정했다. 경기 상태 변경과 저장 작업은 경기 ID·시도 세대를 확인하며, 방이 사라져도 미확정 저장을 운영 상태에서 추적한다. DB 조회가 한 번 없음을 반환한 것만으로 지연 등록의 부재를 확정하지 않는다.
마지막 재검토에서 남은 미확정 기록의 운영 복구 경계를 확인했고, 한도 초과 시 실행 중지·종료 확인·구 실행 복구·DB 재조회 후 재개하는 예외 절차를 운영 안내에 추가했다. DB에 `server_runs`를 두어 종료 실행의 늦은 등록을 영구 차단한다. 실제 PostgreSQL에서 등록 선행·복구 선행·여러 경기와 새 실행 간 잠금 경합·결과 확정과 복구 경합을 검증했으며, 독립 재검토에서도 추가 코드 결함은 발견되지 않았다.

AI 합성 비교는 [별도 기록](ai-policy-verification.md)을 따른다. 동일 조건 16회 비교에서 고정 정책과 새 정책의 승패가 같았으므로 실제 난이도 개선을 입증했다고 해석하지 않는다.

## 실제 환경에서 남은 검증

- Render/Supabase 계정 연결과 프로젝트, custom SMTP, 실제 사용자 4명의 가입·인증·복구 메일.
- Supabase 실제 Data API 키·Auth·RLS 연동 및 인터넷 WSS 대전.
- Render 자체 빌드·배포와 외부 TLS/WSS 연결. GitHub Actions의 Linux Docker 빌드·기동은 위 실행으로 검증했다.
- 서로 다른 PC에서 4명·2경기 동시 지연·메모리·방 간 간섭, 실제 무료 서비스 초기 기동과 재시작.
- 사람의 체감 AI 난이도와 실제 창의 포커스·키 입력.

위 항목은 로컬 대역/DB 시험으로 대체하지 않는다. 필요한 설정과 실행 순서는 [배포·운영 안내](render-operations.md)에 정리했다. 외부 주기 요청은 제출 시점에 별도로 결정한다.

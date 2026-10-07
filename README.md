# Campus Quest Tetris

Java 8 호환 Swing 퍼즐 RPG입니다. 테트리스로 블록을 지우며 몬스터와 싸우는 9전투 스토리, 로컬 모드, 서버 판정 온라인 PvP와 공식 랭킹을 제공합니다.

## 실행

PowerShell에서 저장소 루트로 이동한 뒤 실행합니다.

```powershell
.\build.ps1 -Task Test -JdkHome 'C:\Program Files\Android\Android Studio\jbr'
.\build.ps1 -Task Run -JdkHome 'C:\Program Files\Android\Android Studio\jbr'
```

JDK 8 이상이 필요합니다. `-JdkHome`은 `JAVA_HOME`에 JDK가 설정돼 있으면 생략할 수 있습니다. 결과물은 `out/tetris.jar`와 `out/tetris-server.jar`입니다. 명령 프롬프트에서 빌드된 게임만 실행하려면 `"C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar "C:\Project\Source_code_Analysis_Tetris\out\tetris.jar"`를 사용합니다.

| 키 | 동작 |
|---|---|
| ← / → | 이동 |
| ↑ / ↓ | 회전 |
| D / Space | 한 칸 낙하 / 즉시 낙하 |
| C | HOLD |
| 1–4 | 아이템 슬롯 사용 |
| P / Esc | 로컬 일시정지 / 돌아가기 |

설정 화면에서 음량, 조작 튜토리얼, LAN 연결, 온라인 계정을 관리합니다. 사용자 진행과 설정은 홈 디렉터리의 `.tetris-monster`에 저장됩니다.

## 구성과 난이도

- 대학교: 술 → 교양 → 교수
- 졸업: 재수강 F → GPT → 캡스톤
- 취업: 코딩 테스트 → 면접관 → 기업

9전투의 HP, 플레이어 낙하 속도, 몬스터 행동 간격, AI 탐색 한도, 공격 배율, 추가 가비지, 몬스터 아이템 레벨은 [`stages.properties`](src/main/resources/story/stages.properties) 한 파일에서 수정합니다. 몬스터 등급과 이름도 각 전투에 함께 지정합니다. 공통 전투 공식은 [`balance.properties`](src/main/resources/battle/balance.properties), AI 평가 가중치는 [`profiles.properties`](src/main/resources/ai/profiles.properties)에 있습니다. 설정 변경 뒤 `-Task Test`로 검증합니다.

제품 화면은 `ui/seongeun`과 `app/SeongeunApplication`을 사용합니다. `core`, `battle`, `item`, `story`, `ai`, `network`, `ranking`이 게임 규칙과 통신을 담당합니다. 서버와 클라이언트 산출물은 `pom.xml`에서 분리합니다.

## 로컬 및 공식 온라인 대전

LAN 서버는 별도 터미널에서 `.\build.ps1 -Task Server -Port 28080`으로 실행하거나 게임 설정에서 시작할 수 있습니다. 두 게임 창을 `127.0.0.1:28080`에 연결해 방 생성·입장·준비 순서로 대전합니다. LAN 전적은 공식 랭킹에 반영되지 않습니다.

공식 PvP는 서버 판정 결과만 전적과 랭킹에 저장합니다. 계정은 아이디와 비밀번호로 가입·로그인합니다. 클라이언트의 `tetris-client.properties`에는 다음 공개 설정만 둡니다. 비밀번호, 계정 토큰, 서버 비밀 키는 배포 파일에 넣지 않습니다.

```properties
server.url=wss://YOUR_SERVICE.onrender.com/ws
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.publishableKey=YOUR_PUBLIC_KEY
```

[`render.yaml`](render.yaml)은 Render의 수동 배포 설정이며 서버 비밀 값은 Render Environment에서 관리합니다. DB 스키마는 [`supabase/migrations`](supabase/migrations)에 순서대로 있습니다. 접수 상태를 보존하는 최신 마이그레이션 적용 시에는 **기존 서버를 drain·중지 → SQL 적용 → 새 서버 배포 → `/admin/open` 1회** 순서를 지켜야 합니다. 이후 재시작은 DB의 접수 정책과 실행 임대를 확인해 자동으로 재개합니다. `RENDER_ADMIN_TOKEN`을 가진 운영자는 [`render-admin.ps1`](scripts/render-admin.ps1)로 접수 상태·개방·중지를 관리합니다. `/healthz` 성공은 로그인·대전·전적 저장의 성공을 뜻하지 않습니다.

### dev 배포 절차

배포 기준은 `dev`이며 `render.yaml`의 `branch`도 `dev`로 고정합니다. 자동 배포는 계속 꺼 두고, `dev` push 및 `dev` 대상 PR의 `Validate Render container` 검사에서 **linux-container와 postgres-contract가 모두 성공한 커밋**만 수동 배포합니다. CI는 Linux 컨테이너 빌드·전체 Java 시험·비관리자 사용자·관리 API 인증과, 격리 PostgreSQL의 마이그레이션·권한·경기 결과 동시성·실행 임대를 검증합니다.

1. Render 서비스의 연결 브랜치가 `dev`인지 확인합니다. YAML 수정만으로 기존 서비스 설정 변경을 확인한 것으로 간주하지 않습니다.
2. 운영 셸의 `RENDER_ADMIN_TOKEN`으로 `scripts/render-admin.ps1 -ServerUrl https://YOUR_SERVICE.onrender.com -Action drain`을 실행합니다. `-Action status`의 `active=0`, `unresolved=0`을 확인한 뒤 배포합니다. 미정산 경기가 있으면 배포를 보류합니다.
3. 위 DB 업그레이드 순서가 필요한 최초 임대 마이그레이션은 기존 서버를 완전히 중지한 뒤 적용합니다. 이후 일반 코드 배포에서는 마이그레이션을 다시 실행하지 않습니다.
4. Render에서 검증된 `dev` 커밋을 배포하고, 배포 기록의 커밋과 `/healthz` 응답을 확인합니다. drain은 DB 접수 정책을 닫으므로 새 서버의 `-Action status`를 확인한 뒤 `-Action open`으로 접수를 다시 엽니다.
5. `admission=true`, `draining=false`, `unresolved=0`을 확인하고 두 계정의 로그인 → 방 생성·참가 → 준비 → 종료 → 공식 전적·랭킹을 검증합니다. `/healthz`는 실행 생존 확인이며 DB와 실행 임대의 장애 여부는 접수 상태 및 실제 대전으로 확인합니다.

배포 실패 시 직전 검증된 커밋으로 재배포합니다. 임대/DB 계약과 호환되는 버전만 사용하며, 임대 도입 전 버전으로 코드를 되돌리지 않습니다. 기존 실행이 완전히 중지됐다는 증거 없이 `recover`를 호출하지 않습니다. DB 장애나 강제 종료로 남은 경기는 새 실행의 임대 인수 및 운영자 복구 절차로 확인합니다.

## 검증과 자료

`-Task Test`는 Java 단위·통합·로컬 TCP/HTTP 시험을 실행합니다. `-Task NetworkFixture`는 네트워크 재생, `-Task Preview -AllowVisibleDesktop`은 실제 GUI 확인에 사용합니다. 테스트 통과와 운영 서버 배포·실제 인터넷 2인 대전 검증은 별개입니다.

게임에 포함한 효과음은 Kenney의 [UI Audio](https://kenney.nl/assets/ui-audio), [Impact Sounds](https://kenney.nl/assets/impact-sounds), [Digital Audio](https://kenney.nl/assets/digital-audio) 팩(CC0)에서 변환했습니다. `audio/university`의 배경음과 `ui/university`의 일부 이미지는 팀의 `University_Simulation` 프로젝트에서 가져왔습니다. `ui/campus-rpg`, `ui/puzzle-rpg`, `ui/characters`의 신규 이미지는 이 게임을 위해 생성했습니다. 상세 원본 해시·제작 기록과 개발 문서는 제출용 소스 저장소 밖에 별도 보관합니다.

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

설정 화면에서 음량, 조작 튜토리얼, 온라인 계정을 관리합니다. 사용자 진행과 설정은 홈 디렉터리의 `.tetris-monster`에 저장됩니다.

## 구성과 난이도

- 대학교: 술 → 교양 → 교수
- 졸업: 재수강 F → GPT → 캡스톤
- 취업: 코딩 테스트 → 면접관 → 기업

9전투의 HP, 플레이어 낙하 속도, 몬스터 행동 간격, AI 탐색 한도, 공격 배율, 추가 가비지, 몬스터 아이템 레벨은 [`stages.properties`](src/main/resources/story/stages.properties) 한 파일에서 수정합니다. 몬스터 등급과 이름도 각 전투에 함께 지정합니다. 공통 전투 공식은 [`balance.properties`](src/main/resources/battle/balance.properties), AI 평가 가중치는 [`profiles.properties`](src/main/resources/ai/profiles.properties)에 있습니다. 설정 변경 뒤 `-Task Test`로 검증합니다.

제품 화면은 `ui/seongeun`과 `app/SeongeunApplication`을 사용합니다. `core`, `battle`, `item`, `story`, `ai`, `network`, `ranking`이 게임 규칙과 통신을 담당합니다. 서버와 클라이언트 산출물은 `pom.xml`에서 분리합니다.

### 프론트엔드 인계

현재 PvE 전투 화면은 왼쪽 캐릭터 전투 무대, 가운데 플레이어 보드, 오른쪽 HOLD·아이템·NEXT·FEVER 정보로 구성됩니다. 온라인 PvP는 양쪽 보드와 가운데 전투 무대를 사용합니다. 프론트엔드 담당자는 화면 크기와 전투 가독성에 맞춰 배치·비율·디자인을 자유롭게 수정해도 됩니다. 스토리 대사의 트리거 횟수·발생 시점·문구·기획·디자인도 담당자가 자유롭게 조정할 수 있습니다.

## 공식 온라인 대전

공식 PvP는 서버 판정 결과만 전적과 랭킹에 저장합니다. 계정은 아이디와 비밀번호로 가입·로그인합니다. 기본 클라이언트는 Firebase Auth·Firestore와 서울 리전의 Cloud Run 서버에 연결합니다. 서버 주소와 Firebase **공개 API 키**는 `online-defaults.properties`에 포함합니다. 다른 Firebase 서버로 연결할 때만 JAR 옆의 `tetris-client.properties`로 다음 네 값을 덮어씁니다. 비밀번호, 계정 토큰, 서버 비밀 키는 배포 파일에 넣지 않습니다.

```properties
online.provider=firebase
server.url=wss://YOUR_SERVICE.run.app/ws
firebase.projectId=YOUR_PROJECT
firebase.apiKey=YOUR_PUBLIC_API_KEY
```

이전 Render/Supabase 구현도 보존합니다. [`render.yaml`](render.yaml)은 이전 환경의 수동 배포 설정이며 서버 비밀 값은 Render Environment에서 관리합니다. DB 스키마는 [`supabase/migrations`](supabase/migrations)에 순서대로 있습니다. 이전 환경으로 연결하려면 `online.provider=supabase`, `server.url`, `supabase.url`, `supabase.publishableKey`를 함께 지정합니다. `RENDER_ADMIN_TOKEN`을 가진 운영자는 [`render-admin.ps1`](scripts/render-admin.ps1)로 접수 상태·개방·중지를 관리합니다. 데이터 이관 후 이전 서버의 접수는 닫아 두며, 되돌릴 때는 새 환경에서 추가된 데이터부터 확인해야 합니다.

`scripts/install-render-keepalive.ps1`은 이전 Render 환경을 위한 Windows 작업 스케줄러 도구입니다. Cloud Run 이전 후에는 이 작업을 사용하지 않습니다.

### Firebase + Cloud Run 이전

Firebase Auth가 계정을, Firestore가 전적·Elo·서버 실행 임대를 저장합니다. 대전 판정과 WebSocket은 기존 Java 서버가 Cloud Run에서 수행합니다. `TETRIS_BACKEND=firebase`로 서버 구현을 선택하며, 데스크톱에는 공개 API 키만 포함됩니다. Firebase Admin SDK와 이관 도구는 서버 JAR에만 들어갑니다. 기본 접속값은 실제 계정·전적 이관과 두 클라이언트 대전을 검증한 Firebase/Cloud Run 환경입니다.

Cloud Run은 Blaze 결제 연결이 필요합니다. 배포 설정은 **최소 0개·최대 1개, 1 vCPU/1 GiB, 인스턴스 수명 동안 CPU 할당, 요청 제한 60분**입니다. 백그라운드 임대 갱신이 있으므로 요청 중에만 CPU를 주는 설정으로 바꾸면 안 됩니다. 최대 인스턴스 수는 배포 중 중복 실행까지 막는 잠금이 아니므로 Firestore의 실행 임대도 유지합니다. 첫 접속과 순위표 조회는 일시적 503을 제한된 시간 동안 재시도합니다. 연결이 성립한 뒤 끊어진 대전은 자동 재생하지 않으며, 다시 접속해야 합니다.

월 접속 횟수가 적으면 무료 할당량 안에 들어갈 가능성이 높지만, 실제 실행 시간·네트워크·이미지 저장량에 따라 비용이 생길 수 있습니다. 예산 알림은 지출 상한이 아닙니다. Cloud Run에는 Render용 주기적 keepalive를 연결하지 않습니다. [CPU 과금 설정](https://docs.cloud.google.com/run/docs/configuring/billing-settings), [WebSocket 제한](https://docs.cloud.google.com/run/docs/triggering/websockets), [Firestore 무료 할당량](https://firebase.google.com/docs/firestore/quotas)을 참고합니다.

배포 전 준비:

1. 테트리스 전용 Firebase 프로젝트에서 이메일/비밀번호 Authentication을 켭니다. Web 앱을 등록해 공개 API 키를 받습니다. 기존 아이디는 `username@players.campus-quest.invalid` 주소를 사용하므로 이메일 재설정 메일은 사용할 수 없습니다.
2. Firestore **Standard / Native** 기본 DB를 Cloud Run과 같은 리전(기본 `asia-northeast3`)에 만듭니다. [`firebase/firestore.rules`](firebase/firestore.rules)는 클라이언트 직접 읽기·쓰기를 모두 거부합니다. 서버 서비스 계정만 IAM으로 접근합니다. 규칙과 인덱스를 `firebase deploy --only firestore --project PROJECT_ID`로 배포합니다.
3. Google Cloud CLI와 Firebase CLI로 로그인하고 승인된 결제 계정을 연결합니다. 배포 스크립트는 최초 준비 때 Secret Manager에 무작위 관리자 토큰을 만들며 기존 토큰은 교체하지 않습니다. 서버는 전용 `tetris-runtime` 계정의 ADC를 사용하며 서비스 계정 JSON 키를 배포하지 않습니다.

```powershell
# 기본 실행은 변경 없이 배포 대상을 출력합니다.
.\scripts\deploy-cloud-run.ps1 -ProjectId PROJECT_ID
# 권한·API·이미지 저장소 준비 및 검증을 포함한 이미지 빌드/배포:
.\scripts\deploy-cloud-run.ps1 -ProjectId PROJECT_ID -Provision -Deploy
```

[`cloudbuild.yaml`](cloudbuild.yaml)과 [`Dockerfile.cloudrun`](Dockerfile.cloudrun)은 JDK 21에서 Java 8 대상 코드를 빌드하고 전체 headless 테스트를 실행합니다. `.gcloudignore`의 허용 목록 때문에 로컬 인증 정보·DB 내보내기·도구 디렉터리는 빌드 서버로 올라가지 않습니다. 런타임에는 Firestore 쓰기와 Firebase Auth 조회 권한만 부여하고, 관리자 토큰은 Secret Manager로 주입합니다.

계정·전적 이관은 새 프로젝트가 비어 있는 상태에서 진행합니다. 기존 서버 접수를 중지하고 진행 중인 경기를 정산한 뒤 [`export-supabase-firebase.sql`](scripts/export-supabase-firebase.sql)로 일관된 스냅샷을 내보냅니다. 출력은 **비밀번호 해시가 포함된 비공개 자료**이므로 `.migration-private`에만 보관하고 저장소·채팅·빌드에 넣지 않습니다. `FirebaseMigration`은 기본적으로 오프라인 검증만 수행하고, 명시적 적용 때도 UID·bcrypt 비밀번호·전적·경기별 평점 변경을 보존합니다. 이관 중 접수는 닫힌 상태로 유지되며, 같은 내보내기의 재실행만 허용합니다. 실제 적용 전 기존 DB 백업을 별도로 보존합니다.

로컬 적용에는 일반 `gcloud auth login`과 별개인 Admin SDK 인증이 필요합니다. 일반 사용자 ADC만으로 Firebase Auth를 호출하는 대신 **임시 이관 서비스 계정의 impersonated ADC**를 사용합니다. Java SDK는 이 방식을 지원합니다. 다음 준비는 이관을 수행할 운영자 계정에서 하며, 기존 ADC가 있다면 별도 운영 셸/설정 디렉터리에서 진행해 덮어쓰지 않습니다. [서비스 계정 가장을 통한 ADC 설정](https://docs.cloud.google.com/docs/authentication/use-service-account-impersonation#local-adc), [Firebase 로컬 인증 제약](https://firebase.google.com/docs/admin/setup#test-with-gcloud-end-user-credentials)을 참고합니다.

```powershell
$project='PROJECT_ID'
$operator='OPERATOR_GOOGLE_EMAIL'
$migrationAccount="tetris-migration@$project.iam.gserviceaccount.com"
gcloud services enable iamcredentials.googleapis.com --project=$project
gcloud iam service-accounts create tetris-migration --project=$project
gcloud projects add-iam-policy-binding $project --member="serviceAccount:$migrationAccount" --role=roles/firebaseauth.admin --condition=None
gcloud projects add-iam-policy-binding $project --member="serviceAccount:$migrationAccount" --role=roles/datastore.user --condition=None
gcloud iam service-accounts add-iam-policy-binding $migrationAccount --member="user:$operator" --role=roles/iam.serviceAccountTokenCreator --project=$project
gcloud auth application-default login --impersonate-service-account=$migrationAccount
```

```powershell
java -cp target/tetris-server.jar kr.ac.jbnu.se.tetris.migration.FirebaseMigration --input .migration-private/supabase-export.json
# 검증된 파일의 SHA-256을 사용하며, 적용용 ADC는 서버 런타임보다 높은 이관 권한이 필요합니다.
$env:FIREBASE_PROJECT_ID='PROJECT_ID'
java -cp target/tetris-server.jar kr.ac.jbnu.se.tetris.migration.FirebaseMigration --input .migration-private/supabase-export.json --apply --confirm-sha256 VALIDATED_SHA256
```

이관과 실제 로그인 검증이 끝나면 위 서비스 계정에 부여한 `roles/firebaseauth.admin`, `roles/datastore.user` 및 운영자의 `roles/iam.serviceAccountTokenCreator` 바인딩을 제거합니다. 서비스 계정 비공개 키 파일은 만들지 않습니다. 런타임 서비스 계정에는 이관 권한을 추가하지 않습니다.

새 게임 ID는 최초 인증 때 UID와 영구 연결됩니다. Firebase 공개 API로 이메일이나 표시 이름을 바꿔도 게임 ID는 바뀌지 않습니다. 일반 이메일 계정은 이관 도구가 만든 신뢰 표시와 기존 이름 기록이 있어야 접속할 수 있습니다. Firebase 로그아웃은 현재 데스크톱의 메모리 세션을 지우고 연결을 종료하며, Firebase SDK의 일반 로그아웃처럼 다른 기기의 갱신 토큰까지 폐기하지 않습니다.

Cloud Run 배포 후 `/health`는 프로세스 응답만, `/ready`는 실행 임대와 접수 가능 여부를 뜻합니다. Render 호환 경로 `/healthz`와 `/readyz`도 유지하지만 Cloud Run의 상태 점검에는 사용하지 않습니다. 최초 접수는 닫혀 있으므로 `TETRIS_ADMIN_TOKEN`을 설정한 운영 셸에서 [`cloud-admin.ps1`](scripts/cloud-admin.ps1)의 `open`을 사용합니다. **기존 아이디 로그인 → 인터넷 2인 방 생성·참가 → 정상 종료/기권 → Elo와 순위표 → 유휴 후 재접속**을 확인한 다음 클라이언트 공개 설정을 전환합니다.

```properties
online.provider=firebase
server.url=wss://YOUR_CLOUD_RUN_SERVICE.run.app/ws
firebase.projectId=YOUR_FIREBASE_PROJECT_ID
firebase.apiKey=YOUR_PUBLIC_FIREBASE_WEB_API_KEY
```

기존 서비스와 DB는 검증이 끝날 때까지 보존합니다. 새 서비스에서 실경기 전적을 받기 시작한 뒤에는 접속 주소만 되돌리면 양쪽 DB가 갈라지므로, 다시 접수를 닫고 데이터 차이를 대조한 뒤 복구해야 합니다.

## 검증과 자료

`-Task Test`는 Java 단위·통합·로컬 WebSocket/HTTP 시험을 실행합니다. `-Task NetworkFixture`는 네트워크 재생, `-Task Preview -AllowVisibleDesktop`은 실제 GUI 확인에 사용합니다. 테스트 통과와 운영 서버 배포·실제 인터넷 2인 대전 검증은 별개입니다.

Firebase 검사는 JDK 21 이상과 Firebase CLI 15.33.0을 설치한 뒤 `scripts/test-firebase-emulators.ps1 -JdkHome 'JDK_PATH'`로 실행합니다. 인증·트랜잭션·2인 WebSocket 대전·기권 저장·이관/재실행을 새 `demo-` 프로젝트의 localhost 에뮬레이터에서 순서대로 확인합니다. CLI 에뮬레이터의 비밀번호 검증은 실제 bcrypt를 구현하지 않으므로, 이관 검사는 해시 바이트 보존까지 확인하고 **기존 비밀번호 로그인은 실제 Firebase의 별도 테스트 계정으로 검증해야 합니다**. 외부 Firebase에 연결하지 않는 테스트에서 이 항목을 성공으로 표시하지 않습니다.

게임에 포함한 효과음은 Kenney의 [UI Audio](https://kenney.nl/assets/ui-audio), [Impact Sounds](https://kenney.nl/assets/impact-sounds), [Digital Audio](https://kenney.nl/assets/digital-audio) 팩(CC0)에서 변환했습니다. `audio/university`의 배경음과 `ui/university`의 일부 이미지는 팀의 `University_Simulation` 프로젝트에서 가져왔습니다. `ui/campus-rpg`, `ui/puzzle-rpg`, `ui/characters`의 신규 이미지는 이 게임을 위해 생성했습니다. 상세 원본 해시·제작 기록과 개발 문서는 제출용 소스 저장소 밖에 별도 보관합니다.

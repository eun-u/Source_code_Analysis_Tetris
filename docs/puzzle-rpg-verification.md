# 퍼즐 RPG MVP 검증 기록

## 화면·소리·튜토리얼 개선 (2026-10-07, 현재 빌드)

`codex/feature-puzzle-rpg-mvp`에서 Java 8u504로 제품 소스 166개와 검증 소스 55개를 컴파일했다. `out/campus-ui-final-tests.log`의 **46개 헤드리스 테스트 모음이 모두 PASS**했다. 로컬 TCP를 쓰는 테스트는 제한된 실행 환경에서 연결되지 않아, 루프백이 허용된 실행에서 전체 모음을 다시 수행해 통과했다.

- 스토리 선택 화면은 대학교·졸업·취업 3행에 총 9전투를 한 번에 표시한다. 1020×720 및 820×650 화면을 렌더해 잠금·도전 버튼과 스테이지 정보를 확인했다.
- 전투 HUD는 HOLD와 NEXT 3개를 상단에 나란히 놓고, 아이템 1~4번 슬롯을 확대했다. 1024 및 820 너비 렌더에서 네 슬롯이 모두 보인다. 작은 창은 하단 내용에 스크롤이 필요할 수 있다.
- 홈은 `University_Simulation` 배경 이미지를 차용했고 다른 화면도 동일한 픽셀 테마로 정리했다. 설정 화면에 BGM·효과음별 음량/음소거, LAN, 실제 조작을 진행하는 5단계 튜토리얼과 `다시 보지 않기`를 배치했다. 첫 로컬 시작에서 튜토리얼을 자동 시작하며 튜토리얼 버튼·Esc의 복귀 목적지를 일치시켰다.
- BGM은 같은 원본 프로젝트의 배경곡을 WAV로 변환해 JAR에 넣었다. 동작별 합성 효과음과 기존 3개 효과음도 포함했다. 설정 종료 즉시 저장과 재시작 복원을 검사했고, JBR 장치에서 BGM·효과음 Clip을 열 수 있음을 확인했다. 실제 청감과 루프 연결감은 사람의 확인이 남아 있다.
- 실행 창은 내용 영역의 4:3 비율을 유지하며 크기를 조절한다. 실제 Windows 창에서 첫 화면과 첫 로컬 시작의 튜토리얼을 확인했다. 그 뒤 화면 복귀·오디오 저장 수정 후 최종 패키지를 재빌드했고 전체 테스트를 재실행했다. 사용자 창 입력이 감지되어 최종 빌드의 추가 자동 창 조작은 하지 않았다.
- 독립 정적 리뷰에서 찾은 튜토리얼 버튼 복귀 경로와 종료 시 오디오 저장 경쟁 조건 두 건을 수정·재검토했다. 추가로 조치할 확정 P1/P2는 발견하지 못했다.

현재 `out/tetris.jar`와 패키지의 JAR SHA-256은 `F8B87120912270BDD3FD92AAB26B5363424047604B297145B8D8C6F0BA29CF5E`로 일치한다. `out/campus-quest-client.zip` SHA-256은 `84A3B0A04C741D0D960A4A721FCC1EC43DD3D3B98BCEFABDA85185540F7BB90E`다. ZIP에는 JAR·공개 설정·실행 스크립트·안내 4개 파일만 있으며 공개 설정은 서버 주소·Supabase 주소·publishable key 3줄이다. JAR에 대학 배경 이미지와 배경음 WAV 및 효과음 WAV 3개가 포함됨을 확인했다. 운영 서버 재배포와 DB 변경은 수행하지 않았다.

---

아래 Campus Quest 통합 기록은 이번 화면·소리 개선 이전 패키지의 검증 이력이다. 당시 해시는 현재 패키지와 구별한다.

## Campus Quest 통합 (2026-10-07)

기준 브랜치: `codex/feature-puzzle-rpg-mvp`, 시작 커밋: `b0ff168`. 대학교·졸업·취업의 3개 Stage와 각 3전투, 총 9레벨을 반영했다. 상대는 술·교양·교수 / 재수강 F·GPT·캡스톤 / 코딩테스트·면접관·기업이며 각 세 번째 전투가 보스다. 낙하 간격 450→270ms, 공격 배율 1.0→1.5, 몬스터 아이템 레벨 0→5는 사용자 지정 수치다. 초반 HP·행동 간격·AI 탐색은 완화했고 패턴과 레벨을 분리했다.

Java 8u504로 제품 소스 164개와 검증 소스 54개를 컴파일했다. 최종 결과는 `out/campus-rpg-release-tests.log`의 **45개 헤드리스 테스트 모음 PASS**이며 제품은 `out/tetris.jar`다.

```powershell
.\build.ps1 -Task Test -JdkHome 'C:\Users\User\AppData\Local\Temp\codex-tetris-baseline-jdk8\jdk8u504-b01'
.\scripts\package-campus-client.ps1
```

- 스토리: 실제 엔진의 9전투 승리·해금·패배·재도전·최종 복귀, 같은 Stage의 엘리트 두 전투를 ID로 구별한다.
- 전투·아이템: 레벨별 몬스터 아이템 제한, 획득·사용·무효화 이벤트, 같은 ID 아이템의 사용 직후 재획득, 획득 실패·효과 없는 사용 시 이벤트 부재를 검사했다. 피격·공격·회복·Fever·착지·클리어·콤보·승패를 화면 효과와 연결했다. 8종 아이템은 각각 다른 모양·색·문구로 표시한다.
- 저장: 이전 15전투 형식의 순차 진행을 9레벨로 이관하고 코인·소유·장착을 유지한다. 읽기만으로 원본을 변경하지 않으며 첫 저장에서 바이트가 동일한 `.v1.bak`을 남긴다. 알 수 없는 전투나 완료 순서의 공백은 거부한다.
- 네트워크: 실제 루프백 TCP, 로컬 HTTP/WSS 프로토콜 대체 서버, 인증 헤더·토큰 갱신·WebSocket 프레임·직렬화·랭킹 테이블을 검사했다. 계정 A의 접속 종료 → B 로그인 → B 토큰으로 랭킹 조회까지 앱 흐름을 검사했다. 이 대체 서버 검증은 운영 DB 저장 증거가 아니다.
- 독립 리뷰: 계정 전환과 오래된 요청 응답의 충돌, 랭킹 재조회, 실제 아이템 이벤트 누락을 수정한 뒤 관련 회귀 검증을 통과했다. 최종 독립 정적 리뷰에서 남은 확정 P1/P2는 없었다.
- 이미지: built-in image_gen으로 전투 아틀라스 3개와 개별 캐릭터 컷아웃 6개를 제작했다. 최종 프롬프트는 `src/main/resources/ui/puzzle-rpg/README.md`에 기록했고 원본 이미지를 보존했다.
- 효과음: `University_Simulation`의 원본 MP3 3개를 Windows Media Transcoder로 WAV 변환하여 JAR에 포함했다. 출처·해시는 `src/main/resources/audio/university/README.md`에 기록했다. 설치된 Oracle JRE/Android Studio JBR에서 8개 믹서와 Clip 열기를 확인했으며 음량·음소거를 저장한다. 빌드용 임시 JDK는 믹서가 0개였다.

최종 헤드리스 화면은 `out/campus-preview/story.png`, `story-graduation.png`, `story-employment.png` 및 `battle-final-1240.png`, `battle-final-760.png`다. 8종 아이템 효과 비교는 `out/audiofx-validation/arena-items.png`다. 실행 패키지 `out/campus-quest-client.zip`에는 JAR·공개 설정·실행 스크립트·안내의 4개 파일만 포함한다.

패키지 내 JAR과 빌드 JAR의 SHA-256은 `BD5108BFD7B840070F8C6582E68BB2CE3997BE6488055D03198CDC529E6C57F1`로 일치한다. ZIP SHA-256은 `54E87E4B5516E56B63DA4AC3281B3EC7C7570A25609F669ECADDCAC4D07C0947`이다. 패키지의 공개 설정은 3줄이며 JAR에 새 PNG 9개와 WAV 3개가 포함됨을 확인했다. Android Studio JBR의 `javaw.exe`로 패키지 JAR을 콘솔 없이 실행했고 새 로그인 화면을 실제 Windows 창 캡처로 확인했다.

기존 Render 서버의 `/healthz`는 2026-10-07에 **200 OK / ok** 응답을 받았다. Supabase 로그인·공식 PvP·랭킹을 새 클라이언트에 연결했으며 운영 서버 재배포·DB 변경은 하지 않았다. 공식 온라인은 기존 서버 규칙을 사용한다. 공개 설정은 서버 주소·Supabase 주소·publishable key 3개뿐이며 비밀번호나 계정 토큰을 패키지에 넣지 않는다.

검증 한계: 사람의 체감 난이도와 승리 비율, 실제 스피커 청취, 물리 키 입력 전체, 다른 PC의 LAN, 새 클라이언트의 실계정 인터넷 2인 경기와 전적 저장은 이번 검증으로 입증하지 않는다. Render Free 절전 후 대전 접수 재개와 일반 가입·복구 SMTP는 기존 운영 설정에 따른다.

---

아래는 2026-10-06 초기 MVP의 이력이다. 당시 Stage 구성과 산출물 해시는 현재 버전과 구별한다. 기준 브랜치: `codex/feature-puzzle-rpg-mvp`, 시작 커밋: `c1aca5d`.

## 입력·복귀와 임시 UI 추가 검증

초기 MVP 이후 키 입력과 전투 복귀를 보완하고, 사용자가 지정한 `University_Simulation`의 색상·픽셀 패널·아바타 PNG를 차용했다. 제품 소스 150개와 검증 소스 50개를 Java 8로 컴파일했고 전체 41개 테스트 모음을 통과했다. 로그는 `out/keyboard-ui-integration.log`다. 이 버전 JAR SHA-256은 `81B0316CD737F7202337A829BEE61B25DC1AC58805DA5AA863EA15C5E9FE1211`이다.

- 기존 보드의 키가 버튼과 JScrollPane 바인딩에 선점되는 문제를 별도 JFrame에서 재현했다. 수정 후 같은 창의 표시 중인 게임 보드만 조작 키를 먼저 받고, 숨긴 카드와 다른 창의 입력은 가져오지 않는 경로를 확인했다.
- `BoardKeyboardDesktopSmoke`는 실제 Swing 버튼 포커스와 KeyboardFocusManager 이벤트 전달을 검사한다. OS 키 입력을 주입하는 시험과는 구분한다.
- Esc·돌아가기의 로컬/스토리 세션 종료와 타이머 해제, 온라인 상대 기권 전달을 앱·실제 TCP 테스트에서 확인했다.
- 실제 JScrollPane에 담은 1160×780 및 760×660 렌더에서 상단 돌아가기와 오른쪽 키 안내가 표시되는 것을 확인했다. 최종 PNG는 `out/university-skin/battle-final-1160.png`와 `battle-final-760.png`다.
- PNG 3개의 원본과 복사본 해시를 대조했고 원본 프로젝트는 수정하지 않았다. 출처는 `src/main/resources/ui/university/README.md`에 기록했다.
- 독립 정적 리뷰에서 남은 확정 P1/P2는 없었다. 새 제품 창을 실행했으나 사용자 입력 감지로 Windows 자동 키 시험은 중단했다. 실제 OS 키 조작 전체를 검증했다고 주장하지 않는다.

## 초기 MVP 통합 결과 (`7687350`)

JDK 8u504로 제품 소스 149개와 검증 소스 49개를 컴파일하고 전체 41개 헤드리스 테스트 모음을 통과했다. 실제 루프백 TCP와 파일 저장을 허용한 실행 환경에서 검증했다.

```powershell
.\build.ps1 -Task Test -JdkHome 'C:\Users\User\AppData\Local\Temp\codex-tetris-baseline-jdk8\jdk8u504-b01'
```

최종 로그는 `out/rpg-final-verified.log`, 실행 JAR은 `out/tetris.jar`다. JAR SHA-256은 `F3C284B859455BB74EE5034E55A59D3117EA664D2234EE605C633355360FA0EB`다.

- Core: 아이템 미노 5개 주기, 회전·HOLD 원본 유지, 부분/동시 줄 제거 시 중복 획득 방지, 전투 착지 경계와 가비지 적용.
- Battle: 실제 8종 아이템 획득·자동/수동 사용·실패 시 보존, PC 예외, 가비지 2초 FIFO·상쇄·최대 4줄, Fever와 시간 왜곡 만료, 캐릭터 패시브, PvP 기본형 HP100·60초 첫 가속.
- Monster: 난이도 LEVEL_1~9, 스토리 HP 연결, 보스 67%/33% 단계 경계와 허용 가중치 조정폭, AI 세션 종료/일시정지.
- App/save: 로컬·스토리 진입, 기권·결과·복귀, 구매·장착·최초 보상 저장과 중복 방지, 저장 실패 후 다음 저장의 진행 복구, 잘못된 저장 거부, 전투 중 상점 메뉴 진입 차단, 모달 안내 중 로컬 시간 정지·입력 차단과 복귀.
- Network: 실제 두 클라이언트 방 생성·입장·준비·대전·결과·재대결·연결 종료, 서버의 실제 아이템 보유 검사, 확장 전투 상태와 미노 메타데이터 직렬화.

## 화면과 독립 리뷰

최종 제품 클래스로 `BattlePanelRenderSmoke`를 실행해 `out/rpg-ui/battle-panel-final.png`를 생성하고 확인했다. 1160×780 및 최소 960×660 렌더에서 상단 전투 무대, 격자, HOLD/NEXT, 아이템 슬롯, 하단 조작 안내가 표시된다. Sprint 완료는 보드 오버레이로 표시한다.

독립 정적 리뷰에서 전투 중 상점 이동과 모달 안내 뒤에서 전투가 계속되는 문제를 발견했다. 상점은 로비/상점에서만 허용하고, 로컬 모달 동안 기존 RUNNING 세션을 일시정지한 뒤 복귀하며, 온라인 전투는 비모달 안내를 사용하도록 수정했다. 두 수정의 회귀 검증을 포함한 전체 테스트를 다시 통과했다. 최종 정적 리뷰에서 남은 확정 P1/P2는 없었다.

## 검증 범위

헤드리스 렌더와 세션/API·실제 루프백 TCP 검증이다. 실제 데스크톱 키보드 포커스, 다른 PC의 LAN, 공개 서버/계정 서비스, 인간 대상 난이도·승리 비율 밸런스는 이번 결과로 입증하지 않는다. 게임 규칙은 `docs/puzzle-rpg-rules.md`를 따른다.

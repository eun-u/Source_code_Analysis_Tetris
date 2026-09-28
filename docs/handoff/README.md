# 팀 인수인계 시작점

목표는 각 담당자가 다른 파트의 구현을 기다리지 않고 작업한 뒤 공통 계약으로 통합하는 것이다.

| 담당 | 소유 | 먼저 읽을 문서 | 첫 작업 |
|---|---|---|---|
| 통합 담당 | app·공통 세션·일반 입력 경계·빌드 | [공통 계약](../contracts/g0-contracts.md) | 변경된 API와 실제 구현 연결 검증 |
| 전예랑 | core·battle·item·character·Story 진행 | [Battle](battle.md), [Story](story.md) | Item 효과 1종과 실패 처리 |
| 심은진 | ai·AIController·network | [AI](ai.md), [Network](network.md) | Network 코덱/방 또는 적응 정책 1종 |
| 조성은 | ui·resource·이미지/음원/폰트 | [UI](ui.md) | fixture 기반 대전 화면 및 결과 화면 |

Story 콘텐츠 기획은 전예랑·조성은 공동이며 파일 반영 담당은 한 명으로 지정한다. AI 프로필은 별도 파일이므로 Story 이름/이미지 작업과 가중치 작업의 충돌을 줄인다. 심은진의 AI와 Network는 하나씩 작은 기능으로 통합한다.

## 공통 실행

현재 폴더 골격은 다음과 같다. 각 파트의 실제 클래스 목록과 호출 계약은 위 담당 문서와 공통 계약을 기준으로 한다.

```text
src/main/java/
  Main.java
  kr/ac/jbnu/se/tetris/
    Tetris.java                 실행 진입점
    app/                        앱 조립·모드·Story 연결
      session/                  공통/로컬/온라인 세션
    core/                       보드·피스·명령·불변 상태
    battle/                     참가자·HP·피해·승패
    controller/                 입력 전달·AI worker
    ai/                         공통 휴리스틱·프로필·가중치 정책
    story/                      Stage·몬스터·진행·해금
    network/                    TCP 클라이언트·방·메시지 계약
      protocol/                 길이 지정 JSON 코덱·상태 변환
      server/                   로컬 서버·방·서버 중력 시계
    item/                       Item 입력 명세
    character/                  기본 캐릭터 명세
    ui/                         Swing 화면·라우터·세션 바인딩
    resource/                   리소스 조회·캐시
src/main/resources/
  ai/profiles.properties        AI 설정 단일 소유 위치
  story/stages.properties       콘텐츠·HP·AI 프로필 참조
  assets.properties            리소스 키 매핑
  images/ sound/ fonts/         제품 리소스
src/test/java/                  운영 패키지별 검증
  .../support/                  Fake·샘플 상태·온라인 재생
  .../ui/UiPreviewMain.java      서버 없는 화면 개발 실행기
docs/contracts/                공유 API·스레드·상태 규칙
docs/handoff/                  담당별 시작점·후속 작업
out/g0/                        재생성 가능한 빌드·검증 산출물
```

`ui/component` 등 추가 분리는 실제 구현이 필요할 때 수행한다. PC 번호별 필드 대신 참가자 ID를 키로 사용하고, 현재 제품의 1:1 정책과 자료구조의 참가자 표현을 분리한다.

```powershell
.\build.ps1 -Task Test
.\build.ps1 -Task NetworkFixture
.\build.ps1 -Task Server -Port 28080
# 실제 창을 여는 개발 UI 미리보기
.\build.ps1 -Task Preview -AllowVisibleDesktop
```

실제 게임은 `Run` 또는 `run.cmd`로 실행한다. 테스트·미리보기 코드는 src/test에만 있으며 배포 JAR에서 제외된다. 필요 시 `-JdkHome`으로 설치된 JDK 8+ 경로를 지정한다.

## 병합 규칙

`main`은 검증된 공유 기준, `dev`는 통합 기준, 기능별 짧은 브랜치를 권장한다. `eunjin`은 공통 기반과 로컬 PvP를 포함한 `dev` 기준에서 시작하는 개인 개발 브랜치다. 개인 브랜치를 사용하더라도 AI·Network 등 서로 다른 기능은 작은 커밋으로 분리한다.

공통 타입·API 변경은 통합 담당과 영향받는 담당자가 함께 확인한다. app/controller 폴더 전체를 수정 금지하지 않으며, 담당 영역 내부 구현과 공유 계약 변경을 구분한다. 새 기능은 정상·실패·종료 경계를 검증하고 회귀 테스트와 문서를 함께 갱신한다.

## 완료와 후속 범위

G0에는 실제 휴리스틱 PvE, 잠금·해금, ID 기반 UI, 공통 입력/세션, 네트워크 테스트 대역, Item 거절 경계, 중립 캐릭터 설정을 제공한다. 이후 [로컬 PvP](../local-pvp.md)에 실제 TCP 서버·클라이언트와 방·대전 UI를 추가했다. 적응/보스 정책·LAN·재접속·Item 효과·캐릭터 특수 능력·Save/Shop/Fever·최종 아트·쉬움 난이도는 후속 작업이다.

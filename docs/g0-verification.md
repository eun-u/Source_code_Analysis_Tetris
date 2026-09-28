# G0 구현 및 검증 기록

검증일: 2026-09-28. Java 8 / Swing 프로젝트를 팀별 독립 개발이 가능한 공통 기반으로 정리한 로컬 작업이다. Git commit/push는 수행하지 않았다.

## 구현 결과

| 영역 | 이번 제공 범위 | 다음 담당 작업 |
|---|---|---|
| app/controller | ModeManager, MatchSession, 로컬 시계와 온라인 표시 분리, 종료·취소 처리 | 합의한 API를 통한 기능 연결 |
| Core/Battle | 기존 규칙 유지, 검증된 불변 Snapshot 생성, 참가자 ID, 기권·Item 거절 경계 | Item 실제 효과, Character/Fever |
| AI | 공통 휴리스틱 + FixedWeightPolicy, 프로필 분리, 관측 사본, 정책 실패 진단 | 적응·보스 국면별 가중치 정책과 밸런스 검증 |
| Story | 일반→엘리트→보스, 전투 결과 반영, 잠금·해금·재도전 | 최종 콘텐츠와 Save |
| Network | 메시지·방·클라이언트 계약, OnlineMatchSession, 테스트 전용 Fake | 코덱·실제 서버·TCP·LAN |
| UI | 참가자 ID 기반 표시, 공통 바인딩, 방·대전·결과 샘플 | 최종 화면·리소스·실제 연결 |

쉬움 난이도 선택/배율, MirrorStrategy, 사용하지 않던 LocalSession은 제거했다. 구 AdaptiveStrategy는 실행 경로에서 제외하고 계산 참고용 deprecated 코드로 남겼다. Stage 설정과 AI 프로필의 행동 간격·가중치 중복을 제거했다. 기존 phase 문서는 이력으로 보존하고 현재 기준은 [공통 계약](contracts/g0-contracts.md)과 [팀 인수인계](handoff/README.md)로 지정했다.

## 최종 검증

- JDK: Temurin 8u504-b01
- `build.ps1 -Task Test`: 운영 Java 96개, 테스트/지원 Java 31개 컴파일, **25개 헤드리스 테스트 묶음 통과**, 프로세스 종료 코드 0
- StoryFlowTest: 실제 엔진의 15전투 진행·패배 재도전·승리 재플레이·최종 복귀. 시드 생성기를 주입하고 테스트용 탐색을 상태 수 기준으로 고정하여 재현성 확보
- G0LifecycleTest: AI 대기 시간의 일시정지 보존, 요청 ID 반환과 결과 전달 순서, 구독 해제, 종료 이후 대기 중 화면 갱신의 역행 방지
- OnlineMatchSessionTest: 첫 상태 버전 0, 요청과 확정 결과 구분, 구경기·중복·지연 메시지 폐기, FAILED 이후 재활성화 차단, 두 스레드의 요청·수신·종료 처리
- 정책·Snapshot·Battle 경계: 잘못된 설정/정책 결과, 정책 복구 진단, 입력 경로, 불변 사본 검증, Item 거절 시 전투 상태 보존, 일시정지 중 기권
- 화면 렌더링: 제품 6개 상태와 개발용 8개 탭을 760×680 / 960×820에서 렌더링한 PNG **28개** 생성. Stage 선택 및 4명 참가자 샘플 이미지 직접 확인
- OnlinePreviewScenario: 서버 없이 `RUNNING → FINISHED → CLOSED` 재생 성공
- 제품 JAR 검사: AI/Story 리소스 포함, FakeNetworkClient·UiPreviewMain·OnlinePreviewScenario·테스트 클래스 제외
- 독립 코드 검토에서 확인한 종료 후 지연 수신 및 EDT 상태 역행 문제 수정 후 재검토 완료
- Java/설정 파일 UTF-8, 한국어 주석 서술형 종결 후보, 공백, 현재 인수 문서 링크 검사

AssetTest의 `Unknown asset ID: not.registered` 경고는 누락 리소스 대체 동작을 확인하는 의도된 테스트 출력이다. 검증 산출물은 `out/g0/test-final.log`, `test-exit.json`, `verification.json`, `ui/*.png`에 있으며 Git 관리 대상이 아니다. verification.json에는 검증 대상 소스/설정/빌드 스크립트와 JAR의 SHA-256을 기록한다.

## 검증의 범위

실제 TCP 연결·서버·서로 다른 PC의 LAN, OS 창 포커스·실제 키보드 입력, 인간 대상 난이도/밸런스는 검증하지 않았다. 현재 3/4명 화면은 ID 기반 자료구조와 표시 검증용이며 다인 온라인 게임의 출시 완료를 의미하지 않는다. 최소 크기의 다인 샘플 일부 보조 문구는 생략 표시되므로 최종 UI 담당의 배치 조정 대상이다. 진행 기록은 메모리에서만 유지된다.

수정 전 소스·문서·빌드 설정 백업: `C:\Users\User\AppData\Local\Temp\tetris-g0-before-20260928-212954\workspace-before.zip`. 기존 미추적 파일과 기존 Git 변경을 보존했으며 브랜치 생성·병합·원격 업로드는 수행하지 않았다.

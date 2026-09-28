# Phase 2 진행 범위: 튜토리얼과 대학 스토리 AI

2026-09-28. 현재 파일과 Phase 0/1 기록을 확인하고 기존 headless 11개 테스트 묶음을 변경 전에 통과했다. 메모리의 5개 소스 기준은 과거 상태이며 현재는 production 48개, test 12개다. 기존 미커밋 구조 변경은 보존한다.

## 이번 변경과 경계

- 혼자하기를 조작 목표가 있는 튜토리얼로 변경한다. 기본 Swing 컴포넌트로 목표/완료/재시도 UX만 연결한다. 색상·타이포·그림 등의 디자인 작업은 제외한다.
- 5개 대학 단계 각각 일반(전공책/시험) → 엘리트(대학원 입학) → 보스(학위) 사이클을 데이터로 정의한다. 난이도 선택은 HP/행동 지연과 탐색 설정에 적용한다.
- 일반 Heuristic, 플레이 통계를 반영하는 Adaptive, 실제 플레이 배치 로그 기반 Logistic Regression Mirror를 모두 실행 경로에 연결한다. 로그 부족/낮은 신뢰도에서는 Heuristic으로 대체한다. 아이템 판단은 AI에 넣지 않는다.
- Story 진행 중 원본 보드·현재/NEXT/HOLD·실제 선택을 별도 배치 로그로 수집한다. 현재 실행 중인 캠페인 메모리 범위이며 영속 저장이나 사용자 계정은 이번 범위가 아니다.
- 네트워크는 현재 BattleManager에 맞는 서버 권위형 설계와 검증 계획을 문서로 확정한다. 실제 서버/로그인/외부 연결을 이번에 구현했다고 표시하지 않는다.
- Character/Item/Fever, 저장/상점, 최종 이미지·사운드는 기존 후속 범위로 남긴다. 전체 Phase 2/3 완료로 표시하지 않는다.

## 책임과 계약

Core/Board의 규칙은 복제하지 않는다. 모든 전략은 `AIStrategy.plan(GameState): AIPlan`으로 합법적인 GameAction 목록을 반환하고 `AIController`가 EDT 밖에서 계산한다. app은 버전 확인 후 같은 BattleManager로 행동을 적용한다.

`PlayerProfile`과 `PlacementLog`는 app에서 사람의 Core 이벤트만 받아 갱신한다. 전략은 불변 스냅샷을 읽는다. 학습은 Mirror 판단 worker에서 실행하고 데이터/반복/시간을 제한한다. 튜토리얼/Story 진행 상태는 app 및 별도 도메인에 있고 UI는 표시와 콜백만 맡는다.

병렬 소유권: AI 담당은 ai와 AI 테스트, Story 담당은 story/설정/Story 테스트/네트워크 설계, 주석 담당은 core/battle/controller/resource의 설명만 수정한다. 통합 담당은 app/ui/튜토리얼/빌드/통합 테스트/최종 문서를 소유한다. 공통 계약 변경은 먼저 공유한다.

## 검증 순서

컴파일 → 기존 회귀 → 튜토리얼 수락된 입력/완료/일시정지 → 단계·난이도 설정 검증 → 세 전략의 합법 배치와 Mirror 학습/fallback → 실제 worker 통합 → 여러 seed 자동 대전 표본과 계산 시간 측정 → 독립 검토 → 수정 → 최종 headless 검증.

개발자 검증은 `java.awt.headless=true`이며 JFrame/Robot/키보드·마우스 자동 입력/사용자 포커스 변경을 하지 않는다. 이미지 버퍼 렌더링과 Swing EDT 계약만 검사한다. 실제 데스크톱 입력과 사람의 체감 난이도는 이 결과로 검증했다고 주장하지 않는다. Git commit/push/upload는 하지 않는다.

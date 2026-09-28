# Story 파트 인수인계

현재 `StageCatalog`는 5개 샘플 Stage의 일반·엘리트·보스 15전투를 읽는다. 최종 소재·Stage 수를 확정한 상태가 아니며 `src/main/resources/story/stages.properties`에서 콘텐츠를 교체한다. MonsterSpec에는 식별자·표시 이름·등급·HP·aiProfileId만 있다. AI의 행동 간격/가중치는 별도 profiles.properties가 소유한다.

`StoryProgressService.startStage(stageId,runId,localId,monsterId)`는 잠금 검사 후 첫 미완료 전투를 반환한다. 매번 고유한 runId를 제공한다. 완료된 Stage는 일반 전투부터 재플레이한다. `restartActive`는 같은 상대를 새 run으로, `nextEncounter`는 확정 승리 뒤 다음 상대를 반환한다. 최종 보스 다음은 null이다.

실제 BattleResult는 `recordBattleResult(runId,result)`에서 한 번만 반영한다. app의 LocalMatchSession 콜백이 실제 세션의 결과를 연결하고, UI는 승리 boolean이나 해금 setter를 호출하지 않는다. 이미 완료한 결과는 DUPLICATE, 다른 run은 STALE, 같은 종료 뒤 상충 결과는 CONFLICT다. CampaignProgress는 외부 수정이 불가능한 사본이다.

홈 전환·Stage 선택 재진입에도 완료 기록은 남는다. 실패·포기는 해금을 추가하지 않는다. 다음 전투와 재도전의 보드·HP는 초기화한다. 재도전은 관측 통계도 초기화하고, 같은 실행의 다음 상대는 관측을 유지한다. 파일 저장은 후속이며 앱 재시작 후에는 초기 상태다.

검증: StageCatalogTest, StageProgressTest, StoryProgressServiceTest, StoryFlowTest. StoryProgressServiceTest는 2개 Stage fixture로 잠금·해금·stale·중복을 검사한다. StoryFlowTest는 실제 엔진의 15전투 승리와 재도전·최종 복귀를 검사하며 AI 밸런스 평가는 아니다.

다음 작업은 최종 Stage/몬스터 구성, 클리어 연출, Save 연동이다. 콘텐츠 변경 시 stable ID와 다음 Stage 순서를 검토한다. 새로운 진행 정책은 화면에만 넣지 말고 서비스와 테스트에서 정의한다.

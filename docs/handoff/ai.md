# AI 파트 G0 인수인계

## 실행 기준

일반·엘리트·보스 몬스터는 모두 `PlacementSearch`와 `BoardEvaluator`를 거치는 공통 휴리스틱 전략을 사용한다. 현재 실제 연결된 정책은 일반 `FIXED`, 엘리트 `ADAPTIVE`, 보스 `BOSS`다. 각 몬스터의 `aiProfileId`가 기본 가중치, 행동 간격, 탐색 상태 상한, 판단 시간 예산을 선택한다. 기존 보스의 Mirror 학습 실행 경로는 제거했다. `AdaptiveStrategy`는 계산식 참고용 구버전이며 `MonsterStrategies`가 생성하지 않는다.

현재 프로필은 `src/main/resources/ai/profiles.properties`에 있다. `normal_default`, `elite_default`, `boss_default`는 순서대로 `policy=FIXED`, `ADAPTIVE`, `BOSS`를 선택한다. 적응형·보스 정책은 기본 가중치의 `maxWeightDeltaRatio` 범위 안에서만 변한다. 설정값이 다르다는 사실만으로 실제 승률 순위가 보장되지는 않는다. 쉬움 난이도 선택과 별도 배율은 이번 실행 범위가 아니다.

## 파일과 계약

| 파일 | 책임 |
|---|---|
| `ai/profiles.properties` | 프로필별 정책 ID·기본 가중치·행동 간격·탐색 한도 |
| `AIProfileCatalog` | 프로필 목록 로딩, 필수 설정·범위·미지원 정책 거절 |
| `AIProfile` | 불변 설정과 기준 가중치 대비 최대 조정 비율 검사 |
| `AIContext` | AI 요청 시점의 불변 게임 상태·HP·관측·이전 정책 상태 |
| `WeightPolicy` | 해당 요청에 사용할 가중치와 다음 정책 상태 산출 |
| `PolicyDecision` | 정책 ID·가중치·다음 상태·결정 번호·원본 버전·복구 진단 |
| `PolicyDrivenStrategy` | 정책 결과를 공통 탐색·평가 경로에 적용 |
| `AIController` | 단일 작업자에서 계산, 오래된 계획 폐기·취소 |
| `AIPlan` | 실행 입력 경로와 채택 전 정책 진단 정보 |

`AIProfileCatalog.loadDefault().get(profileId)`는 알 수 없는 ID를 거절한다. `FIXED`, `ADAPTIVE`, `BOSS` 이외의 정책 ID도 거절한다. 다른 정책 ID를 추가하려면 해당 정책의 등록과 설정 검증을 함께 구현해야 한다. **설정 오류는 전투 시작 전에 거절**한다.

가중치 항목은 `line`, `fourLineBonus`, `aggregateHeight`, `maximumHeight`, `holes`, `bumpiness`, `wells`이다. 앞의 두 값은 줄 삭제 보상이며 나머지는 보드 위험 지표의 가중치다. `maxWeightDeltaRatio`는 새 정책이 기본 가중치에서 각 항목을 얼마나 조정할 수 있는지 정한다. `AIProfile.allows(weights)`가 이 범위를 검사한다. 기준값이 0이면 해당 항목은 이 설정 아래에서 0으로 유지된다.

`delayMillis` 증가 시 상대 행동 간격이 길어진다. `maxSearchStates` 증가 시 탐색 범위와 CPU 비용이 늘 수 있다. `budgetMillis`는 탐색 루프의 시간 검사 상한이며 OS 수준의 엄격한 실행 시간 보장은 아니다. 현재 행동 간격은 `AIProfile.getDelayMillis()`가 소유하며 Story는 `aiProfileId`만 참조한다.

`AdaptiveWeightPolicy`는 실제 플레이어의 배치가 6회 미만이면 기본 가중치를 사용한다. 이후 공개 관측의 최근 공격·테트리스·콤보·하드드롭 성향이 줄 삭제 보상을, 최근 구멍·높이와 몬스터 HP 감소가 보드 위험 회피를 조절한다. `maxWeightDeltaRatio`는 조정 폭, 정책의 `RESPONSE`는 이전에 수락된 가중치에서 새 목표로 이동하는 속도다. 이전 가중치는 같은 경기의 정책 상태와 함께 전달된 경우에만 사용한다. Story에서 이어진 플레이어 관측은 다음 몬스터와 공유하지만, 가중치의 이동 상태는 새 경기에서 초기화된다.

`BossWeightPolicy`는 자기 HP가 최대치의 2/3 이하일 때 압박 국면, 1/3 이하일 때 생존 국면으로 전환한다. 이미 수락된 국면은 HP 회복 후에도 되돌아가지 않는다. 최소 6회 배치 뒤의 공개 공격 관측만 소폭 반영한다. 경기 ID가 달라지면 보스 국면 상태는 새 HP에서 다시 시작한다. 보스가 숨은 피스 순서나 상대 내부 상태를 읽지 않는다.

## 세션 연결 순서

1. Story의 `MonsterSpec.getAiProfileId()`로 `AIProfileCatalog`에서 프로필 조회
2. `MonsterStrategies.create(spec)`로 정책과 공통 전략 조립
3. AI 요청 시 `PlayerProfile.snapshot()`으로 관측 사본 확보
4. `AIContext(matchId, opponentState, selfHp, selfMaxHp, observation, decisionId, previousWeights, policyState)` 생성
5. `AIController.request(context)`와 `poll(currentState)`로 계획 계산·수령
6. 원본 버전·참가자·결정 번호가 유효한 계획의 전체 입력 적용
7. 모든 입력이 수락된 경우에만 `plan.getPolicyDecision()`의 가중치와 다음 정책 상태 확정

취소, 오래된 상태, 빈 계획, 중간 입력 거절 때는 정책 상태를 확정하지 않는다. 정책 상태는 해당 몬스터와 전투 실행의 세션 데이터다. 작업자 스레드가 세션 상태나 실시간 `PlayerProfile`을 수정해서는 안 된다. AI 계산 중에 플레이어 통계가 바뀌더라도 해당 요청은 처음 받은 사본만 사용한다.

정책 구현이 실행 중 예외를 던지거나, null·다른 정책 ID·다른 결정 번호·다른 원본 버전·허용 범위 밖의 가중치를 반환하면 `PolicyDrivenStrategy`가 **이번 판단에만** 해당 프로필의 검증된 기본 가중치를 사용한다. 이때 `AIPlan.getPolicyDecision().isFallback()`은 `true`이고 `getFallbackReason()`에 원인 코드가 들어간다. 복구 결정의 `nextPolicyState`는 요청의 이전 정책 상태 그대로다. 정상 정책 결정은 `isFallback() == false`이므로 평가·로그에서 별도로 집계한다. 공통 탐색 자체의 오류는 정책 복구 대상으로 감추지 않는다. 합법 후보가 없으면 빈 계획을 반환한다.

새 정책을 추가할 때는 `context.getObservation()`과 `context.getSelfHp()/getSelfMaxHp()` 같은 명시된 사본만 사용한다. `profile.allows(weights)` 범위, 결정 번호·원본 버전, `MonsterStrategies` 등록, 새 경기 상태 초기화를 함께 검증해야 한다.

## 독립 개발과 검증

`MonsterStrategiesTest`는 프로필 로딩 오류, 세 정책의 조립, Fixed 계획, 정책 실행 실패·잘못된 반환의 복구 진단, worker 관측 사본 고정을 확인한다. `WeightPoliciesTest`는 관측 부족·실제 6회 배치·이전 수락 가중치·새 경기 격리·HP 경계·극단 HP와 가중치 범위를 확인한다. `HeuristicStrategyTest`는 실제 엔진에서 입력 경로의 합법성을 확인한다. `AIControllerTest`는 취소·오래된 계획·작업자 오류를 확인한다. `PlacementLogTest`는 첫 줄 삭제의 콤보 값 0과 다음 연속 삭제의 값 1을 확인한다.

수동 비교 도구 `kr.ac.jbnu.se.tetris.app.MonsterBalanceBenchmark --smoke`는 현재 기본 프로필로 한 경기만 실행한다. `--compare-smoke`는 엘리트 한 시드에서 고정 기준과 적응형 정책을 대조한다. `--compare`는 두 Stage × 엘리트·보스 × 두 시드에 각각 고정 기준과 설정 정책을 적용해 16회 경기 결과를 출력한다. 같은 쌍은 시드·HP·행동 간격·탐색 한도를 공유하며 timeout·fallback 횟수도 표시한다. 출력은 합성 플레이어와 가상 시간의 샘플이며 실제 사용자 승률의 근거가 아니다. 실행 시간 예산에 따라 탐색 후보가 달라질 수 있으므로 시드만으로 완전한 재현성을 보장하지 않는다. 실행 기록과 판단은 `docs/ai-policy-verification.md` 참조.

실제 플레이어 체감 난이도와 승률 순서는 아직 검증 대상이다. 보드 직접 수정이나 숨은 플레이어 정보 참조는 계약 밖이다.

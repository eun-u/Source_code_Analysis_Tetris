# Core/Battle 파트 G0 인수인계

## 공통 실행 경계

`GameEngine`은 참가자 한 명의 보드와 입력 규칙을 관리한다. `BattleManager`는 참가자 ID별 엔진, HP, 피해, 가비지, 탈락, 승자를 관리한다. 참가자 자료구조는 `LinkedHashMap<String, Participant>`이고 전투 생성 범위는 2~4명이다. Story 1대1과 이후 Online 1대1은 같은 전투 계약을 사용한다.

`CoreSnapshots`와 `BattleSnapshots`는 네트워크 메시지 변환 및 UI 독립 개발용 **표시 사본** 생성 도구다. 이 사본을 수정해도 실제 엔진이나 전투에는 반영되지 않는다. 권위 있는 상태 변경은 `GameAction` 또는 `BattleManager`의 명령 경로에서만 수행한다.

## 블록 공급 (7-bag)

`BattleManager`는 참가자마다 `SevenBagGenerator(seed)`를 만든다. 일곱 종류를 한 번씩 섞은 가방을 순서대로 비우므로 같은 가방 안에서는 같은 블록이 나오지 않고, 같은 종류 사이의 간격은 최대 13칸, 같은 블록이 연속 세 번 나오는 경우는 없다. 모든 참가자가 같은 시드를 쓰므로 같은 순서의 블록을 받는다. Story PvE, 로컬 PvP, 온라인 PvP가 모두 `BattleManager`를 거치므로 같은 규칙이 적용된다. `SeededPieceGenerator`(독립 균등 추출)는 단일 `GameEngine` 테스트와 `TutorialSession`에서 계속 사용한다.

## 팀원이 바로 사용할 API

| API | 입력과 결과 | 주요 검증 |
|---|---|---|
| `CoreSnapshots.board(width, height, cells)` | 10×22 보드의 `BoardState` | 정확히 220개, null 셀 없음, 배열 방어 복사 |
| `CoreSnapshots.game(...)` | 참가자 한 명의 `GameState` | ID·버전·상태·피스·고스트·NEXT·콤보·가비지 범위 |
| `CoreSnapshots.copyOf(gameState)` | 실제 엔진 상태의 표시 사본 | 전체 보드와 NEXT 별도 복사 |
| `BattleSnapshots.participant(...)` | 참가자 상태·HP·엔진 상태 | 참가자 ID=GameState.actorId, HP 범위, 탈락 일치 |
| `BattleSnapshots.battle(...)` | 2~4명 전투 표시 사본 | Map 키=참가자 ID, 승자·종료 사유 |
| `BattleSnapshots.copyOf(battleState)` | 실제 전투 상태의 표시 사본 | 각 참가자의 엔진 상태까지 별도 복사 |
| `BattleManager.submit(actorId, type)` | 일반 입력 | 엔진·전투 규칙을 통한 결과 판정 |
| `BattleManager.submitItem(actorId, itemUse)` | 아이템 ID·대상·선택 셀 | 유효성 검사 후 현재 `ITEM_NOT_IMPLEMENTED` |
| `BattleManager.forfeit(actorId)` | 명시적 나가기 | 탈락 및 남은 인원에 따른 승자 확정 |

보드 배열은 `cells[y * 10 + x]` 순서이며 빈 칸은 `PieceType.EMPTY`다. `RUNNING`·`PAUSED`에서는 활성 피스가 있거나 `awaitingSpawn=true`인 상태여야 한다. 활성 피스가 있다면 앵커가 합법적이고 `ghostY`가 해당 보드의 실제 착지 위치여야 한다. `READY`는 활성 피스가 없다. 게임 한 명의 종료 상태는 `GameState.Status.GAME_OVER`, 대전의 종료 상태는 `BattleState.Status.FINISHED`다.

UI 샘플은 실제 `GameEngine` 또는 `BattleManager`의 `getState()`를 `copyOf`로 복사해서 만드는 것이 가장 안전하다. 직접 `game(...)`을 호출한다면 피스와 고스트를 임의 값으로 채우지 말고 `PlacementSimulator`와 같은 규칙을 사용해야 한다. 네트워크에서 수신한 DTO를 변환할 때도 동일한 검증을 거친다.

## 아이템과 캐릭터의 현재 범위

`GameAction.ItemUse`는 아이템 ID·대상 참가자 ID·선택 셀을 담는다. `TargetCell`은 음수 좌표를 거절하고, `BattleManager.submitItem`은 현재 보드 밖 좌표와 존재하지 않는 대상도 거절한다. 입력이 유효해도 효과 구현 전에는 `ITEM_NOT_IMPLEMENTED`를 반환하며 전투 버전, HP, 보드를 바꾸지 않는다. `submit(actorId, GameAction.Type.USE_ITEM)`처럼 payload 없이 종류만 전달한 경우는 `INVALID_PAYLOAD`다.

아이템 담당자는 소유·사용 가능 여부, 대상, 효과 순서, HP·가비지·보드 변화, 결과 이벤트를 `BattleManager` 경계에서 연결한다. UI가 HP나 보드를 직접 수정하는 경로는 없다. 어떤 아이템이 자신을 대상으로 할 수 있는지, 소모품 수량과 중복 사용 규칙은 후속 기능 계약에서 결정한다.

`CharacterSpec.DEFAULT`는 ID `student`, 최대 HP 100의 중립 캐릭터다. `new ParticipantSpec(id, name, character)`로 캐릭터의 최대 HP를 전투 시작 설정에 전달한다. 공격·회복·방어 특수 능력과 쉬움 난이도는 현재 구현 범위가 아니다. 캐릭터 효과는 전투 규칙을 통해 적용하고, 캐릭터 외형은 UI 리소스로 연결한다.

## 나가기와 참가자 수

`forfeit`은 `RUNNING`과 `PAUSED`에서 가능하다. 떠난 참가자는 탈락 처리되고, 둘 이상 살아 있으면 전투의 이전 일시정지 상태가 유지된다. 마지막 한 명만 남으면 `FINISHED`, 승자 ID, 종료 사유 `FORFEIT`가 확정된다. 2명·3명·4명에서 같은 자료구조와 규칙을 사용한다. 온라인 연결 종료를 곧바로 포기로 볼지, 유예 시간을 둘지는 네트워크 정책의 후속 결정이다.

포기로 종료된 전투에서는 생존 참가자의 개별 `GameState`가 `PAUSED`일 수 있다. 화면의 경기 종료 여부는 개별 보드 상태 대신 `BattleState.Status`로 판단한다. `BattleSnapshots`는 표시용 사본의 형태를 검증하며 서버의 승자 판정이나 권한 검증을 대체하지 않는다.

## 검증 기준

`SnapshotContractTest`는 실제 엔진의 `READY`, `RUNNING`, `PAUSED`, `awaitingSpawn`, `GAME_OVER` 상태를 복사하여 ID·버전·보드·활성 피스·고스트·NEXT 등을 비교한다. 잘못된 셀 수, null 셀, 불법 피스·고스트와 변경 가능한 입력 배열·목록도 검증한다.

`BattleBoundaryTest`는 실제 전투의 `READY`, `RUNNING`, `PAUSED`, `FINISHED` 사본, ID·HP·승자 경계, `CharacterSpec.DEFAULT`, 아이템 미구현 거절 후 상태 무변경, 2~4명 포기 흐름을 검증한다. `BattleManagerTest`는 기존 줄 삭제 피해·가비지·Top Out 회귀 기준이다. 새 효과를 붙인 뒤에는 같은 명령에 대한 Story와 Online의 판정 일치를 통합 테스트에서 확인한다.

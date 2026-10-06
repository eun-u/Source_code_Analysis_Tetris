package kr.ac.jbnu.se.tetris.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import kr.ac.jbnu.se.tetris.controller.Controller;
import kr.ac.jbnu.se.tetris.controller.PlayerController;
import kr.ac.jbnu.se.tetris.core.ActionResult;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameEngine;
import kr.ac.jbnu.se.tetris.core.GameEvent;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceGenerator;
import kr.ac.jbnu.se.tetris.core.SevenBagGenerator;

/** ID로 구분한 모든 참가자의 엔진과 전투 규칙을 소유하고 입력 순서를 관리 */
public final class BattleManager {
    private final Map<String, Participant> participants = new LinkedHashMap<String, Participant>();
    private final DamageManager damageManager = new DamageManager();
    private final HPManager hpManager = new HPManager();
    private final Random garbageRandom;
    private BattleState.Status status = BattleState.Status.READY;
    private long version;
    private long lastEventId;
    private String winnerId;
    private String reason;

    public BattleManager(List<ParticipantSpec> specs, long seed) {
        this(specs, seed, null);
    }

    /** 테스트용 생성기 주입 경계 및 실제 생성 경로의 공통 시드 사용 */
    BattleManager(List<ParticipantSpec> specs, long seed, Map<String, PieceGenerator> generators) {
        if (specs == null || specs.size() < 2 || specs.size() > 4) {
            throw new IllegalArgumentException("Battle requires 2 to 4 participants");
        }
        if (generators != null && generators.size() != specs.size()) {
            throw new IllegalArgumentException("One fixture generator is required per participant");
        }
        for (ParticipantSpec spec : specs) {
            if (spec == null || participants.containsKey(spec.getId())) {
                throw new IllegalArgumentException("Participants require unique, non-null IDs");
            }
            // 모든 참가자가 같은 시드의 7-bag을 받아 같은 순서의 블록으로 대전
            PieceGenerator generator = generators == null ? new SevenBagGenerator(seed)
                    : generators.get(spec.getId());
            if (generator == null) throw new IllegalArgumentException("Missing generator: " + spec.getId());
            GameEngine engine = new GameEngine(spec.getId(), generator);
            participants.put(spec.getId(), new Participant(spec, new PlayerController(engine, spec.getId())));
        }
        garbageRandom = new Random(seed ^ 0x5DEECE66DL);
    }

    public synchronized BattleState getState() {
        Map<String, ParticipantState> snapshots = new LinkedHashMap<String, ParticipantState>();
        for (Participant participant : participants.values()) {
            snapshots.put(participant.spec.getId(), new ParticipantState(participant.spec.getId(),
                    participant.spec.getName(), participant.hp, participant.spec.getMaxHp(),
                    participant.controller.getState(), participant.eliminated));
        }
        return new BattleState(status, version, snapshots, winnerId, reason);
    }

    /** 모든 참가자를 시작한 뒤에만 대전을 RUNNING 상태로 전환 */
    public synchronized BattleResult start() {
        if (status != BattleState.Status.READY) return reject("START requires READY", null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant participant : participants.values()) {
            ActionResult result = participant.controller.submit(GameAction.Type.START);
            appendCoreEvents(participant, result, events);
            if (!result.isAccepted()) {
                events.add(event(BattleEvent.Type.ACTION_REJECTED, participant.spec.getId(),
                        null, 0, result.getReason(), null));
                finish("START_FAILED: " + result.getReason(), events);
                return result(false, reason, events);
            }
            if (participant.eliminated) {
                finish("START_TOP_OUT", events);
                return result(false, reason, events);
            }
        }
        status = BattleState.Status.RUNNING;
        version++;
        events.add(event(BattleEvent.Type.MATCH_STARTED, null, null, 0, null, null));
        return result(true, null, events);
    }

    /** 참가자 입력을 코어로 보내고 그 결과 이벤트를 전투 규칙에 반영 */
    public synchronized BattleResult submit(String actorId, GameAction.Type type) {
        Participant participant = participants.get(actorId);
        if (participant == null) return reject("Unknown actor ID", actorId);
        if (status != BattleState.Status.RUNNING) return reject("Battle requires RUNNING", actorId);
        if (participant.eliminated) return reject("Actor is eliminated", actorId);
        if (type == GameAction.Type.USE_ITEM) return reject("INVALID_PAYLOAD", actorId);
        if (type == null || type == GameAction.Type.START || type == GameAction.Type.PAUSE
                || type == GameAction.Type.RESUME || type == GameAction.Type.GRAVITY_TICK
                || type == GameAction.Type.RECEIVE_GARBAGE) {
            return reject("Action is managed by BattleManager", actorId);
        }
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        ActionResult action = participant.controller.submit(type);
        appendCoreEvents(participant, action, events);
        if (isDeliveryFailure()) return result(false, reason, events);
        if (!action.isAccepted()) {
            return rejectWithEvents(action.getReason(), actorId, events);
        }
        version++;
        finishIfOneRemains(events);
        return result(true, null, events);
    }

    /** 아이템 대상 검증과 미구현 효과의 명시적 거절 및 상태 무변경 */
    public synchronized BattleResult submitItem(String actorId, GameAction.ItemUse itemUse) {
        Participant source = participants.get(actorId);
        if (source == null) return reject("UNKNOWN_ACTOR", actorId);
        if (status != BattleState.Status.RUNNING) return reject("BATTLE_NOT_RUNNING", actorId);
        if (source.eliminated) return reject("ACTOR_ELIMINATED", actorId);
        if (itemUse == null) return reject("INVALID_PAYLOAD", actorId);
        Participant target = participants.get(itemUse.getTargetActorId());
        if (target == null || target.eliminated) return reject("INVALID_TARGET", actorId);
        GameAction.TargetCell cell = itemUse.getTargetCell();
        if (cell != null && (cell.getX() >= 10 || cell.getY() >= 22)) return reject("INVALID_TARGET_CELL", actorId);
        return reject("ITEM_NOT_IMPLEMENTED", actorId);
    }

    /** 명시적 나가기에 따른 참가자 탈락과 최종 결과 생성 */
    public synchronized BattleResult forfeit(String actorId) {
        Participant participant = participants.get(actorId);
        if (participant == null) return reject("UNKNOWN_ACTOR", actorId);
        if (status != BattleState.Status.RUNNING && status != BattleState.Status.PAUSED) {
            return reject("BATTLE_NOT_RUNNING", actorId);
        }
        if (participant.eliminated) return reject("ACTOR_ELIMINATED", actorId);
        participant.eliminated = true;
        participant.eliminationReason = "FORFEIT";
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        version++;
        BattleState.Status previous = status;
        status = BattleState.Status.RUNNING;
        finishIfOneRemains(events);
        if (status != BattleState.Status.FINISHED) status = previous;
        return result(true, null, events);
    }

    /** 생존 참가자에게 순서대로 중력 틱을 적용하고 종료 여부를 확인 */
    public synchronized BattleResult tick() {
        if (status != BattleState.Status.RUNNING) return reject("TICK requires RUNNING", null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant participant : participants.values()) {
            if (participant.eliminated) continue;
            ActionResult action = participant.controller.submit(GameAction.Type.GRAVITY_TICK);
            appendCoreEvents(participant, action, events);
            if (isDeliveryFailure()) return result(false, reason, events);
            if (!action.isAccepted()) {
                events.add(event(BattleEvent.Type.ACTION_REJECTED, participant.spec.getId(),
                        null, 0, action.getReason(), null));
                finish("TICK_FAILED: " + participant.spec.getId() + ": "
                        + action.getReason(), events);
                return result(false, reason, events);
            }
            finishIfOneRemains(events);
            if (status == BattleState.Status.FINISHED) break;
        }
        version++;
        return result(true, null, events);
    }

    public synchronized BattleResult pause() {
        return changePauseState(GameAction.Type.PAUSE, BattleState.Status.RUNNING,
                BattleState.Status.PAUSED, BattleEvent.Type.PAUSED);
    }

    public synchronized BattleResult resume() {
        return changePauseState(GameAction.Type.RESUME, BattleState.Status.PAUSED,
                BattleState.Status.RUNNING, BattleEvent.Type.RESUMED);
    }

    private BattleResult changePauseState(GameAction.Type actionType, BattleState.Status required,
                                          BattleState.Status next, BattleEvent.Type eventType) {
        if (status != required) return reject(actionType + " requires " + required, null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant participant : participants.values()) {
            GameState.Status coreStatus = participant.controller.getState().getStatus();
            if (coreStatus == GameState.Status.GAME_OVER) continue;
            ActionResult action = participant.controller.submit(actionType);
            appendCoreEvents(participant, action, events);
            if (!action.isAccepted()) {
                events.add(event(BattleEvent.Type.ACTION_REJECTED, participant.spec.getId(),
                        null, 0, action.getReason(), null));
                finish(actionType + "_FAILED: " + action.getReason(), events);
                return result(false, reason, events);
            }
        }
        status = next;
        version++;
        events.add(event(eventType, null, null, 0, null, null));
        return result(true, null, events);
    }

    /** 코어 이벤트를 보존하면서 줄 제거 공격과 탑아웃 탈락을 파생 */
    private void appendCoreEvents(Participant participant, ActionResult action,
                                  List<BattleEvent> events) {
        boolean topOut = false;
        for (GameEvent core : action.getEvents()) {
            events.add(event(BattleEvent.Type.CORE_EVENT, core.getActorId(), null,
                    core.getLineCount(), core.getReason(), core));
            if (core.getType() == GameEvent.Type.LINE_CLEAR) {
                attack(participant, core, events);
            } else if (core.getType() == GameEvent.Type.GARBAGE_RECEIVED) {
                events.add(event(BattleEvent.Type.GARBAGE_RECEIVED, participant.spec.getId(),
                        null, core.getLineCount(), core.getReason(), core));
            } else if (core.getType() == GameEvent.Type.TOP_OUT) {
                topOut = true;
            }
        }
        if (topOut && !participant.eliminated) {
            participant.eliminated = true;
            participant.eliminationReason = "TOP_OUT";
        }
    }

    /** 줄 제거 피해를 먼저 적용하고 대상이 생존한 경우 가비지를 예약 */
    private void attack(Participant source, GameEvent lineClear, List<BattleEvent> events) {
        if (source.eliminated) return;
        Participant target = nextLivingTarget(source.spec.getId());
        if (target == null) return;
        DamageManager.Attack attack = damageManager.forLineClear(lineClear);
        int before = target.hp;
        target.hp = hpManager.applyDamage(before, target.spec.getMaxHp(), attack.getDamage());
        events.add(event(BattleEvent.Type.DAMAGE, source.spec.getId(), target.spec.getId(),
                before - target.hp, null, lineClear));
        events.add(event(BattleEvent.Type.HP_CHANGED, source.spec.getId(), target.spec.getId(),
                target.hp, null, lineClear));
        if (target.hp == 0) {
            target.eliminated = true;
            target.eliminationReason = "HP_DEPLETED";
            return;
        }
        if (attack.getGarbageLines() > 0) {
            GameAction.Garbage garbage = new GameAction.Garbage(attack.getGarbageLines(),
                    garbageRandom.nextInt(target.controller.getState().getBoard().getWidth()));
            ActionResult queued = target.controller.submit(garbage);
            if (!queued.isAccepted()) {
                events.add(event(BattleEvent.Type.ACTION_REJECTED, source.spec.getId(),
                        target.spec.getId(), 0, "Garbage delivery failed: " + queued.getReason(), null));
                finish("GARBAGE_DELIVERY_FAILED: " + queued.getReason(), events);
                return;
            }
            events.add(event(BattleEvent.Type.GARBAGE_SENT, source.spec.getId(),
                    target.spec.getId(), attack.getGarbageLines(), null, lineClear));
            appendCoreEvents(target, queued, events);
        }
    }

    private Participant nextLivingTarget(String actorId) {
        List<Participant> ordered = new ArrayList<Participant>(participants.values());
        int index = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).spec.getId().equals(actorId)) { index = i; break; }
        }
        for (int i = 1; i < ordered.size(); i++) {
            Participant candidate = ordered.get((index + i) % ordered.size());
            if (!candidate.eliminated) return candidate;
        }
        return null;
    }

    private void finishIfOneRemains(List<BattleEvent> events) {
        if (status != BattleState.Status.RUNNING) return;
        String soleSurvivor = null;
        int alive = 0;
        String lastEliminationReason = null;
        for (Participant participant : participants.values()) {
            if (participant.eliminated) lastEliminationReason = participant.eliminationReason;
            else { soleSurvivor = participant.spec.getId(); alive++; }
        }
        if (alive <= 1) {
            winnerId = alive == 1 ? soleSurvivor : null;
            finish(alive == 0 ? "NO_SURVIVOR" : lastEliminationReason, events);
        }
    }

    private void finish(String why, List<BattleEvent> events) {
        status = BattleState.Status.FINISHED;
        reason = why;
        version++;
        events.add(event(BattleEvent.Type.MATCH_FINISHED, null, winnerId, 0, why, null));
    }

    private boolean isDeliveryFailure() {
        return status == BattleState.Status.FINISHED && reason != null
                && reason.startsWith("GARBAGE_DELIVERY_FAILED");
    }

    private BattleResult reject(String why, String actorId) {
        return rejectWithEvents(why, actorId, new ArrayList<BattleEvent>());
    }

    private BattleResult rejectWithEvents(String why, String actorId, List<BattleEvent> events) {
        events.add(event(BattleEvent.Type.ACTION_REJECTED, actorId, null, 0, why, null));
        return result(false, why, events);
    }

    private BattleResult result(boolean accepted, String why, List<BattleEvent> events) {
        return new BattleResult(accepted, why, getState(), events);
    }

    private BattleEvent event(BattleEvent.Type type, String actorId, String targetId,
                              int amount, String why, GameEvent core) {
        return new BattleEvent(type, ++lastEventId, actorId, targetId, amount, why, core);
    }

    private static final class Participant {
        private final ParticipantSpec spec;
        private final Controller controller;
        private int hp;
        private boolean eliminated;
        private String eliminationReason;

        private Participant(ParticipantSpec spec, Controller controller) {
            this.spec = spec;
            this.controller = controller;
            this.hp = spec.getMaxHp();
        }
    }
}

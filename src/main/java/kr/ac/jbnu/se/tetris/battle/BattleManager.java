package kr.ac.jbnu.se.tetris.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import kr.ac.jbnu.se.tetris.character.CharacterSpec;
import kr.ac.jbnu.se.tetris.controller.Controller;
import kr.ac.jbnu.se.tetris.controller.PlayerController;
import kr.ac.jbnu.se.tetris.core.ActionResult;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameEngine;
import kr.ac.jbnu.se.tetris.core.GameEvent;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceGenerator;
import kr.ac.jbnu.se.tetris.core.SevenBagGenerator;
import kr.ac.jbnu.se.tetris.item.ItemSpec;
import kr.ac.jbnu.se.tetris.item.ItemIds;

/** 가상 시간, 참가자 상태, 공격과 가비지를 처리하는 전투 규칙 소유자. */
public final class BattleManager {
    private static final List<String> ITEM_IDS = ItemIds.ALL;
    private static final String INVALID_TARGET_REASON = "INVALID_TARGET";
    private final Map<String, Participant> participants = new LinkedHashMap<String, Participant>();
    private final DamageManager damageManager = new DamageManager();
    private final HPManager hpManager = new HPManager();
    private final Random itemRandom;
    private final boolean pvp;
    /** advance()/tick()에서 실제로 적용된 중력 칸 수. 서버가 스냅샷 전송 여부를 판단하는 데 쓴다. */
    private long gravityStepCount;
    private BattleState.Status status = BattleState.Status.READY;
    private long version;
    private long lastEventId;
    private long elapsedMillis;
    private String winnerId;
    private String reason;

    public BattleManager(List<ParticipantSpec> specs, long seed) {
        this(specs, seed, null, new BattleSettings(false, 400, 400, 0, 0, -1));
    }
    BattleManager(List<ParticipantSpec> specs, long seed, Map<String, PieceGenerator> generators) {
        this(specs, seed, generators, new BattleSettings(false, 400, 400, 0, 0, -1));
    }
    public static BattleManager pvp(List<ParticipantSpec> specs, long seed) {
        return new BattleManager(specs, seed, null, new BattleSettings(true, 500, 500, 0, 0, -1));
    }
    public static BattleManager pve(List<ParticipantSpec> specs, long seed, int playerGravityMillis,
            int monsterGravityMillis, double monsterAttackBuff) {
        return new BattleManager(specs, seed, null, new BattleSettings(false, playerGravityMillis,
                monsterGravityMillis, monsterAttackBuff, 0, -1));
    }
    public static BattleManager pve(List<ParticipantSpec> specs, long seed, int playerGravityMillis,
            int monsterGravityMillis, double monsterAttackBuff, int monsterExtraGarbage) {
        return new BattleManager(specs, seed, null, new BattleSettings(false, playerGravityMillis,
                monsterGravityMillis, monsterAttackBuff, monsterExtraGarbage, -1));
    }
    public static BattleManager pve(List<ParticipantSpec> specs, long seed, int playerGravityMillis,
            int monsterGravityMillis, double monsterAttackBuff, int monsterExtraGarbage,
            int monsterItemLevel) {
        return new BattleManager(specs, seed, null, new BattleSettings(false, playerGravityMillis,
                monsterGravityMillis, monsterAttackBuff, monsterExtraGarbage, monsterItemLevel));
    }
    private BattleManager(List<ParticipantSpec> specs, long seed,
            Map<String, PieceGenerator> generators, BattleSettings settings) {
        validate(specs, generators, settings);
        this.pvp = settings.pvp;
        int index = 0;
        for (ParticipantSpec supplied : specs) addParticipant(supplied, index++, seed, generators, settings);
        itemRandom = new Random(seed ^ 0x2A6F19E5L);
    }

    private static void validate(List<ParticipantSpec> specs, Map<String, PieceGenerator> generators,
            BattleSettings settings) {
        if (isInvalidRoster(specs, generators) || isInvalidTuning(settings))
            throw new IllegalArgumentException("Invalid battle settings");
    }

    private static boolean isInvalidRoster(List<ParticipantSpec> specs, Map<String, PieceGenerator> generators) {
        return specs == null || specs.size() < 2 || specs.size() > 4
                || (generators != null && generators.size() != specs.size());
    }

    private static boolean isInvalidTuning(BattleSettings settings) {
        return isInvalidGravity(settings) || isInvalidMonsterBoost(settings);
    }

    private static boolean isInvalidGravity(BattleSettings settings) {
        return settings.playerGravityMillis < 100 || settings.monsterGravityMillis < 100;
    }

    private static boolean isInvalidMonsterBoost(BattleSettings settings) {
        return !Double.isFinite(settings.monsterAttackBuff) || settings.monsterAttackBuff < 0
                || settings.monsterExtraGarbage < 0 || settings.monsterExtraGarbage > 4
                || settings.monsterItemLevel < -1 || settings.monsterItemLevel > 5;
    }

    private void addParticipant(ParticipantSpec supplied, int index, long seed,
            Map<String, PieceGenerator> generators, BattleSettings settings) {
        if (supplied == null || participants.containsKey(supplied.getId()))
            throw new IllegalArgumentException("Participants require unique IDs");
        ParticipantSpec spec = settings.pvp ? new ParticipantSpec(supplied.getId(), supplied.getName(),
                CharacterSpec.DEFAULT) : supplied;
        PieceGenerator generator = generators == null ? new SevenBagGenerator(seed)
                : generators.get(spec.getId());
        if (generator == null) throw new IllegalArgumentException("Missing generator: " + spec.getId());
        GameEngine engine = new GameEngine(spec.getId(), generator);
        engine.setBattleManaged(true);
        boolean monster = !settings.pvp && index > 0;
        int itemLevel = monster ? settings.monsterItemLevel : -1;
        configureItemSpawns(engine, spec, seed, settings.pvp, itemLevel);
        double attackBuff = monster ? settings.monsterAttackBuff : 0;
        int extraGarbage = monster ? settings.monsterExtraGarbage : 0;
        participants.put(spec.getId(), new Participant(spec, engine,
                new PlayerController(engine, spec.getId()), seed ^ (index * 0x5DEECE66DL),
                baseGravityMillis(settings, index), attackBuff, extraGarbage, itemLevel));
    }

    private static void configureItemSpawns(GameEngine engine, ParticipantSpec spec, long seed,
            boolean pvp, int itemLevel) {
        long itemSeed = seed ^ spec.getId().hashCode();
        if (itemLevel < 0) engine.configureItemSpawns(
                !pvp && "utility".equals(spec.getCharacter().getId()) ? 3 : 5, itemSeed);
        else engine.configureItemSpawns(itemLevel == 0 ? 0 : 10 - itemLevel, itemSeed, monsterItems(itemLevel));
    }

    private static int baseGravityMillis(BattleSettings settings, int index) {
        if (settings.pvp) return 500;
        return index == 0 ? settings.playerGravityMillis : settings.monsterGravityMillis;
    }

    public synchronized BattleState getState() {
        Map<String, ParticipantState> snapshots = new LinkedHashMap<String, ParticipantState>();
        for (Participant p : participants.values()) {
            snapshots.put(p.spec.getId(), new ParticipantState(p.spec.getId(), p.spec.getName(),
                    p.hp, p.spec.getMaxHp(), p.controller.getState(), p.eliminated,
                    p.spec.getCharacter().getId(), p.spec.getCharacter().getItemSlots(), p.items,
                    p.itemCharges,
                    p.fever, Math.max(0, p.feverUntil - elapsedMillis),
                    Math.max(0, p.warpUntil - elapsedMillis), p.tank.getPendingLines(),
                    p.tank.getWaitRemainingMillis(elapsedMillis), gravityMillis(p),
                    p.maxCombo, p.totalDamage));
        }
        return new BattleState(status, version, snapshots, winnerId, reason, elapsedMillis);
    }

    public synchronized BattleResult start() {
        if (status != BattleState.Status.READY) return reject("START requires READY", null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant p : participants.values()) {
            ActionResult action = p.controller.submit(GameAction.Type.START);
            processCore(p, action, events);
            if (!action.isAccepted() || p.eliminated) {
                finish(!action.isAccepted() ? "START_FAILED: " + action.getReason() : "START_TOP_OUT", events);
                return result(false, reason, events);
            }
        }
        status = BattleState.Status.RUNNING;
        version++;
        events.add(event(BattleEvent.Type.MATCH_STARTED, null, null, 0, null, null));
        return result(true, null, events);
    }

    public synchronized BattleResult submit(String actorId, GameAction.Type type) {
        Participant p = participants.get(actorId);
        if (p == null) return reject("Unknown actor ID", actorId);
        if (status != BattleState.Status.RUNNING) return reject("Battle requires RUNNING", actorId);
        if (p.eliminated) return reject("Actor is eliminated", actorId);
        if (type == GameAction.Type.USE_ITEM) return reject("INVALID_PAYLOAD", actorId);
        if (isManagedAction(type)) return reject("Action is managed by BattleManager", actorId);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        ActionResult action = p.controller.submit(type);
        processCore(p, action, events);
        if (isInternalFailure()) return result(false, reason, events);
        if (!action.isAccepted()) return rejectWithEvents(action.getReason(), actorId, events);
        version++; finishIfOneRemains(events);
        return result(true, null, events);
    }

    private static boolean isManagedAction(GameAction.Type type) {
        return type == null || type == GameAction.Type.START || type == GameAction.Type.PAUSE
                || type == GameAction.Type.RESUME || type == GameAction.Type.GRAVITY_TICK
                || type == GameAction.Type.RECEIVE_GARBAGE;
    }

    public synchronized BattleResult submitItem(String actorId, GameAction.ItemUse use) {
        String rejection = itemRejection(actorId, use);
        if (rejection != null) return reject(rejection, actorId);
        Participant source = participants.get(actorId);
        Participant target = participants.get(use.getTargetActorId());
        String id = use.getItemId();
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        String removedByNullify = null;
        switch (id) {
            case ItemIds.HEAL:
                useHeal(source, actorId, events);
                break;
            case ItemIds.GARBAGE_BOMB:
                sendGarbage(source, target, source.tank.cancel(1), null, events);
                break;
            case ItemIds.LINE_CLEANER:
                String failure = useLineCleaner(source, events);
                if (failure != null) return rejectWithEvents(failure, actorId, events);
                break;
            case ItemIds.FEVER_CHARGE:
                chargeFever(source, 20);
                break;
            case ItemIds.TIME_WARP:
                source.warpUntil = elapsedMillis + 5000;
                source.warpSlotIndex = source.items.indexOf(id);
                break;
            case ItemIds.NULLIFY:
                removedByNullify = nullifyRandomItem(target);
                break;
            default:
                break;
        }
        if (!ItemIds.TIME_WARP.equals(id)) consumeItem(source, id);
        events.add(event(BattleEvent.Type.ITEM_USED, actorId, target.spec.getId(), 1, id, null));
        if (removedByNullify != null)
            events.add(event(BattleEvent.Type.ITEM_REMOVED, actorId,
                    target.spec.getId(), 1, removedByNullify, null));
        version++;
        return result(true, null, events);
    }

    /** 아이템 사용 요청의 거절 사유를 검사 순서대로 돌려준다. 문제가 없으면 null. */
    private String itemRejection(String actorId, GameAction.ItemUse use) {
        Participant source = participants.get(actorId);
        if (source == null) return "UNKNOWN_ACTOR";
        if (status != BattleState.Status.RUNNING) return "BATTLE_NOT_RUNNING";
        if (source.eliminated) return "ACTOR_ELIMINATED";
        if (use == null) return "INVALID_PAYLOAD";
        Participant target = participants.get(use.getTargetActorId());
        if (target == null || target.eliminated) return INVALID_TARGET_REASON;
        GameAction.TargetCell cell = use.getTargetCell();
        if (cell != null && (cell.getX() >= 10 || cell.getY() >= 22)) return "INVALID_TARGET_CELL";
        return itemRuleRejection(source, target, use.getItemId());
    }

    private String itemRuleRejection(Participant source, Participant target, String id) {
        if (source.itemLevel >= 0 && !monsterItems(source.itemLevel).contains(id)) return "ITEM_RESTRICTED";
        if (!ITEM_IDS.contains(id) || !source.items.contains(id)) return "ITEM_NOT_OWNED";
        if (ItemIds.DAMAGE_BOOST.equals(id) || ItemIds.SHIELD.equals(id)) return "AUTO_ITEM";
        boolean self = source == target;
        boolean offensive = ItemIds.GARBAGE_BOMB.equals(id) || ItemIds.NULLIFY.equals(id);
        if (offensive == self) return INVALID_TARGET_REASON;
        if (ItemIds.HEAL.equals(id) && source.hp == source.spec.getMaxHp()) return "NO_EFFECT";
        if (ItemIds.TIME_WARP.equals(id) && source.warpUntil > elapsedMillis) return "NO_EFFECT";
        return null;
    }

    private void useHeal(Participant source, String actorId, List<BattleEvent> events) {
        int amount = Math.max(1, (int) Math.ceil(source.spec.getMaxHp() * .1));
        source.hp = hpManager.applyHealing(source.hp, source.spec.getMaxHp(), amount);
        events.add(event(BattleEvent.Type.HP_CHANGED, actorId, actorId, source.hp, ItemIds.HEAL, null));
    }

    /** @return 거절 사유. 성공했거나 정리할 줄이 없으면 null */
    private String useLineCleaner(Participant source, List<BattleEvent> events) {
        ActionResult clean = source.engine.clearBottomGarbageLine();
        if (clean.isAccepted()) {
            processCore(source, clean, events);
            return null;
        }
        return "No garbage line to clean".equals(clean.getReason()) ? null : clean.getReason();
    }

    private String nullifyRandomItem(Participant target) {
        if (target.items.isEmpty()) return null;
        return removeItemAt(target, itemRandom.nextInt(target.items.size()));
    }

    public synchronized BattleResult forfeit(String actorId) {
        Participant p = participants.get(actorId);
        if (p == null) return reject("UNKNOWN_ACTOR", actorId);
        if (status != BattleState.Status.RUNNING && status != BattleState.Status.PAUSED)
            return reject("BATTLE_NOT_RUNNING", actorId);
        if (p.eliminated) return reject("ACTOR_ELIMINATED", actorId);
        p.eliminated = true; p.eliminationReason = "FORFEIT";
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        version++;
        BattleState.Status prior = status; status = BattleState.Status.RUNNING;
        finishIfOneRemains(events);
        if (status != BattleState.Status.FINISHED) status = prior;
        return result(true, null, events);
    }

    /** 지금까지 적용된 중력 칸 수(참가자 합계). 값이 변했으면 보드가 움직인 것이다. */
    public synchronized long getGravityStepCount() { return gravityStepCount; }

    public synchronized BattleResult advance(long deltaMillis) {
        if (status != BattleState.Status.RUNNING) return reject("TICK requires RUNNING", null);
        if (deltaMillis < 0 || deltaMillis > 600000) return reject("INVALID_ELAPSED_MILLIS", null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        long remaining = deltaMillis;
        while (remaining > 0 && status == BattleState.Status.RUNNING) {
            long step = Math.min(50, remaining);
            elapsedMillis += step; remaining -= step;
            expireTimeWarpItems();
            BattleResult failure = advanceParticipants(step, events);
            if (failure != null) return failure;
            finishIfOneRemains(events);
        }
        version++;
        return result(true, null, events);
    }

    /** @return 내부 실패 결과. 모두 정상이면 null */
    private BattleResult advanceParticipants(long step, List<BattleEvent> events) {
        for (Participant p : participants.values()) {
            if (p.eliminated) continue;
            p.gravityAccumulated += step;
            int interval = gravityMillis(p);
            while (p.gravityAccumulated >= interval && !p.eliminated) {
                p.gravityAccumulated -= interval;
                gravityStepCount++;
                BattleResult failure = gravityTick(p, events);
                if (failure != null) return failure;
            }
        }
        return null;
    }

    /** 중력 틱 한 번을 적용한다. @return 내부 실패 결과. 정상이면 null */
    private BattleResult gravityTick(Participant p, List<BattleEvent> events) {
        ActionResult action = p.controller.submit(GameAction.Type.GRAVITY_TICK);
        processCore(p, action, events);
        if (isInternalFailure()) return result(false, reason, events);
        if (!action.isAccepted()) {
            finish("TICK_FAILED: " + p.spec.getId() + ": " + action.getReason(), events);
            return result(false, reason, events);
        }
        return null;
    }

    /** 이전 테스트와 호출부가 사용하는 직접 중력 틱. 시간 기반 호출부는 advance를 사용한다. */
    public synchronized BattleResult tick() {
        if (status != BattleState.Status.RUNNING) return reject("TICK requires RUNNING", null);
        elapsedMillis += 400;
        expireTimeWarpItems();
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant p : participants.values()) {
            if (p.eliminated) continue;
            gravityStepCount++;
            BattleResult failure = gravityTick(p, events);
            if (failure != null) return failure;
        }
        finishIfOneRemains(events); version++;
        return result(true, null, events);
    }

    public synchronized BattleResult pause() {
        return changePause(GameAction.Type.PAUSE, BattleState.Status.RUNNING,
                BattleState.Status.PAUSED, BattleEvent.Type.PAUSED);
    }
    public synchronized BattleResult resume() {
        return changePause(GameAction.Type.RESUME, BattleState.Status.PAUSED,
                BattleState.Status.RUNNING, BattleEvent.Type.RESUMED);
    }
    private BattleResult changePause(GameAction.Type type, BattleState.Status required,
            BattleState.Status next, BattleEvent.Type eventType) {
        if (status != required) return reject(type + " requires " + required, null);
        List<BattleEvent> events = new ArrayList<BattleEvent>();
        for (Participant p : participants.values()) {
            if (p.controller.getState().getStatus() == GameState.Status.GAME_OVER) continue;
            ActionResult action = p.controller.submit(type);
            processCore(p, action, events);
            if (!action.isAccepted()) {
                finish(type + "_FAILED: " + action.getReason(), events);
                return result(false, reason, events);
            }
        }
        status = next; version++;
        events.add(event(eventType, null, null, 0, null, null));
        return result(true, null, events);
    }

    private void processCore(Participant p, ActionResult action, List<BattleEvent> events) {
        List<GameEvent.ItemExtraction> acquired = new ArrayList<GameEvent.ItemExtraction>();
        for (GameEvent core : action.getEvents()) processCoreEvent(p, core, events, acquired);
        for (GameEvent.ItemExtraction ore : acquired) acquire(p, ore, events);
        if (hasEvent(action, GameEvent.Type.PIECE_PLACED) && !p.eliminated
                && p.controller.getState().isAwaitingSpawn())
            afterPlacement(p, hasEvent(action, GameEvent.Type.LINE_CLEAR), events);
    }

    private static boolean hasEvent(ActionResult action, GameEvent.Type type) {
        for (GameEvent core : action.getEvents())
            if (core.getType() == type) return true;
        return false;
    }

    private void processCoreEvent(Participant p, GameEvent core, List<BattleEvent> events,
            List<GameEvent.ItemExtraction> acquired) {
        events.add(event(BattleEvent.Type.CORE_EVENT, core.getActorId(), null,
                core.getLineCount(), core.getReason(), core));
        GameEvent.Type type = core.getType();
        if (type == GameEvent.Type.LINE_CLEAR) {
            attack(p, core, events);
            p.maxCombo = Math.max(p.maxCombo, Math.max(0, core.getCombo()));
            acquired.addAll(core.getItemExtractions());
        }
        if (type == GameEvent.Type.TOP_OUT && !p.eliminated) {
            p.eliminated = true; p.eliminationReason = "TOP_OUT";
        }
        if (type == GameEvent.Type.GARBAGE_RECEIVED)
            events.add(event(BattleEvent.Type.GARBAGE_RECEIVED, p.spec.getId(), null,
                    core.getLineCount(), core.getReason(), core));
    }

    /** 말이 놓인 뒤: 줄을 지우지 않았다면 대기 중인 가비지를 전달하고, 다음 조각을 생성한다. */
    private void afterPlacement(Participant p, boolean cleared, List<BattleEvent> events) {
        if (!cleared && !deliverDueGarbage(p, events)) return;
        if (!p.eliminated) {
            ActionResult spawned = p.controller.submit(GameAction.Type.GRAVITY_TICK);
            if (spawned.isAccepted()) processCore(p, spawned, events);
            else finish("SPAWN_FAILED: " + spawned.getReason(), events);
        }
    }

    /** @return 전달에 실패해 전투가 종료되었으면 false */
    private boolean deliverDueGarbage(Participant p, List<BattleEvent> events) {
        List<GameAction.Garbage> due = p.tank.drainForPlacement(elapsedMillis);
        if (due.isEmpty()) return true;
        ActionResult applied = p.engine.applyGarbage(due);
        processCore(p, applied, events);
        if (applied.isAccepted()) return true;
        finish("GARBAGE_DELIVERY_FAILED: " + applied.getReason(), events);
        return false;
    }

    /** 기존 자동 아이템과 Fever로 공격을 처리한 뒤 획득 아이템과 충전을 적용한다. */
    private void attack(Participant source, GameEvent clear, List<BattleEvent> events) {
        if (source.eliminated) return;
        Participant target = nextLivingTarget(source.spec.getId());
        if (target == null) return;
        boolean perfect = clear.isPerfectClear();
        boolean boost = !perfect && source.items.contains(ItemIds.DAMAGE_BOOST);
        boolean shield = !perfect && target.items.contains(ItemIds.SHIELD);
        DamageManager.Attack computed = damageManager.forLineClear(clear,
                attackBuff(source, perfect, boost), shield ? .3 : 0);
        int damage = computed.getDamage();
        consumeAutoItems(source, target, boost, shield && damage > 0, events);
        dealDamage(source, target, damage, clear, events);
        int garbage = computed.getGarbageLines();
        if (!perfect) garbage += bonusGarbage(source, clear);
        garbage = source.tank.cancel(garbage);
        if (target.hp == 0) {
            target.eliminated = true; target.eliminationReason = "HP_DEPLETED";
        } else sendGarbage(source, target, garbage, clear, events);
        chargeFever(source, feverGain(clear.getLineCount()));
    }

    private double attackBuff(Participant source, boolean perfect, boolean boost) {
        if (perfect) return 0;
        return source.spec.getCharacter().getDamageBuff()
                + source.extraAttackBuff + (source.feverUntil > elapsedMillis ? .2 : 0)
                + (boost ? .5 : 0);
    }

    private void consumeAutoItems(Participant source, Participant target, boolean boost,
            boolean shield, List<BattleEvent> events) {
        if (boost && consumeItem(source, ItemIds.DAMAGE_BOOST))
            events.add(event(BattleEvent.Type.ITEM_USED, source.spec.getId(),
                    source.spec.getId(), 1, ItemIds.DAMAGE_BOOST, null));
        if (shield && consumeItem(target, ItemIds.SHIELD))
            events.add(event(BattleEvent.Type.ITEM_USED, target.spec.getId(),
                    target.spec.getId(), 1, ItemIds.SHIELD, null));
    }

    private void dealDamage(Participant source, Participant target, int damage, GameEvent clear,
            List<BattleEvent> events) {
        int before = target.hp;
        target.hp = hpManager.applyDamage(before, target.spec.getMaxHp(), damage);
        source.totalDamage = (int) Math.min(Integer.MAX_VALUE,
                (long) source.totalDamage + before - target.hp);
        events.add(event(BattleEvent.Type.DAMAGE, source.spec.getId(), target.spec.getId(),
                before - target.hp, null, clear));
        events.add(event(BattleEvent.Type.HP_CHANGED, source.spec.getId(), target.spec.getId(),
                target.hp, null, clear));
    }

    /** Fever와 캐릭터 특성으로 늘어나는 가비지 줄 수. 방어형은 자기 가비지의 가장 오래된 줄도 제거한다. */
    private int bonusGarbage(Participant source, GameEvent clear) {
        int bonus = source.extraGarbage;
        int lines = clear.getLineCount();
        String characterId = source.spec.getCharacter().getId();
        if (source.feverUntil > elapsedMillis && lines == 1) bonus++;
        if ("attacker".equals(characterId) && lines >= 2) bonus++;
        if ("defender".equals(characterId) && lines >= 2) source.tank.removeOldestLine();
        return bonus;
    }

    private void sendGarbage(Participant source, Participant target, int lines,
            GameEvent core, List<BattleEvent> events) {
        if (lines <= 0) return;
        target.tank.enqueue(lines, elapsedMillis);
        events.add(event(BattleEvent.Type.GARBAGE_SENT, source.spec.getId(), target.spec.getId(),
                lines, null, core));
    }
    private void acquire(Participant p, GameEvent.ItemExtraction ore, List<BattleEvent> events) {
        String id = ore.getItemId();
        if (ITEM_IDS.contains(id) && p.items.size() < p.spec.getCharacter().getItemSlots()) {
            int slot = p.items.size();
            p.items.add(id);
            p.itemCharges.add(ItemSpec.initialChargesOf(id));
            events.add(new BattleEvent(BattleEvent.Type.ITEM_ACQUIRED, ++lastEventId,
                    p.spec.getId(), p.spec.getId(), 1, id, null,
                    ore.getX(), ore.getY(), slot));
        }
    }
    private static boolean consumeItem(Participant p, String id) {
        int index = p.items.indexOf(id);
        if (index < 0) return false;
        int left = p.itemCharges.get(index) - 1;
        if (left > 0) p.itemCharges.set(index, left);
        else removeItemAt(p, index);
        return true;
    }
    private static String removeItemAt(Participant p, int index) {
        String removed = p.items.remove(index);
        p.itemCharges.remove(index);
        if (index == p.warpSlotIndex) p.warpSlotIndex = -1;
        else if (index < p.warpSlotIndex) p.warpSlotIndex--;
        return removed;
    }
    private void expireTimeWarpItems() {
        for (Participant p : participants.values()) {
            if (p.warpUntil > 0 && p.warpUntil <= elapsedMillis) {
                p.warpUntil = 0;
                if (p.warpSlotIndex >= 0) removeItemAt(p, p.warpSlotIndex);
            }
        }
    }
    static List<String> monsterItems(int level) {
        switch (level) {
            case 0: return java.util.Collections.emptyList();
            case 1: return Arrays.asList(ItemIds.HEAL);
            case 2: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD);
            case 3: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD, ItemIds.FEVER_CHARGE, ItemIds.LINE_CLEANER);
            case 4: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD, ItemIds.FEVER_CHARGE, ItemIds.LINE_CLEANER,
                    ItemIds.DAMAGE_BOOST, ItemIds.TIME_WARP);
            case 5: return ITEM_IDS;
            default: throw new IllegalArgumentException("Invalid monster item level");
        }
    }
    private void chargeFever(Participant p, int amount) {
        if (p.feverUntil > elapsedMillis) return;
        p.fever = Math.min(100, p.fever + amount);
        if (p.fever == 100) { p.fever = 0; p.feverUntil = elapsedMillis + 10000; }
    }
    private static int feverGain(int lines) {
        switch (lines) {
            case 1: return 2;
            case 2: return 5;
            case 3: return 10;
            default: return 20;
        }
    }
    private int gravityMillis(Participant p) {
        int interval = p.baseGravityMillis;
        if (pvp) interval = pvpGravityAt(elapsedMillis);
        if (p.warpUntil > elapsedMillis) interval += 100;
        return interval;
    }
    public static int pvpGravityAt(long elapsedMillis) {
        if (elapsedMillis < 0) throw new IllegalArgumentException("Negative elapsed time");
        if (elapsedMillis < 60000) return 500;
        long changes = 1 + (elapsedMillis - 60000) / 30000;
        return (int) Math.max(200, 500 - Math.min(300, changes * 50));
    }

    private Participant nextLivingTarget(String actorId) {
        List<Participant> ordered = new ArrayList<Participant>(participants.values());
        int index = 0;
        for (int i = 0; i < ordered.size(); i++)
            if (ordered.get(i).spec.getId().equals(actorId)) { index = i; break; }
        for (int i = 1; i < ordered.size(); i++) {
            Participant p = ordered.get((index + i) % ordered.size());
            if (!p.eliminated) return p;
        }
        return null;
    }
    private void finishIfOneRemains(List<BattleEvent> events) {
        if (status != BattleState.Status.RUNNING) return;
        String survivor = null, lastReason = null;
        int alive = 0;
        for (Participant p : participants.values()) {
            if (p.eliminated) lastReason = p.eliminationReason;
            else { survivor = p.spec.getId(); alive++; }
        }
        if (alive <= 1) {
            winnerId = alive == 1 ? survivor : null;
            finish(alive == 0 ? "NO_SURVIVOR" : lastReason, events);
        }
    }
    private void finish(String why, List<BattleEvent> events) {
        if (status == BattleState.Status.FINISHED) return;
        status = BattleState.Status.FINISHED;
        reason = why; version++;
        events.add(event(BattleEvent.Type.MATCH_FINISHED, null, winnerId, 0, why, null));
    }
    private boolean isInternalFailure() {
        return status == BattleState.Status.FINISHED && reason != null
                && (reason.startsWith("SPAWN_FAILED") || reason.startsWith("GARBAGE_DELIVERY_FAILED"));
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
        private final GameEngine engine;
        private final Controller controller;
        private final GarbageTank tank;
        private final List<String> items = new ArrayList<String>();
        private final List<Integer> itemCharges = new ArrayList<Integer>();
        private final int baseGravityMillis;
        private final double extraAttackBuff;
        private final int extraGarbage;
        private final int itemLevel;
        private int hp;
        private int fever;
        private long feverUntil;
        private long warpUntil;
        private int warpSlotIndex = -1;
        private long gravityAccumulated;
        private int maxCombo;
        private int totalDamage;
        private boolean eliminated;
        private String eliminationReason;
        private Participant(ParticipantSpec spec, GameEngine engine, Controller controller,
                long tankSeed, int gravityMillis, double extraAttackBuff, int extraGarbage,
                int itemLevel) {
            this.spec = spec; this.engine = engine; this.controller = controller;
            this.tank = new GarbageTank(tankSeed); this.hp = spec.getMaxHp();
            this.baseGravityMillis = gravityMillis; this.extraAttackBuff = extraAttackBuff;
            this.extraGarbage = extraGarbage;
            this.itemLevel = itemLevel;
        }
    }

    /** 전투 규칙 튜닝 값을 한 번에 전달하기 위한 매개변수 객체. */
    private static final class BattleSettings {
        private final boolean pvp;
        private final int playerGravityMillis;
        private final int monsterGravityMillis;
        private final double monsterAttackBuff;
        private final int monsterExtraGarbage;
        private final int monsterItemLevel;
        private BattleSettings(boolean pvp, int playerGravityMillis, int monsterGravityMillis,
                double monsterAttackBuff, int monsterExtraGarbage, int monsterItemLevel) {
            this.pvp = pvp;
            this.playerGravityMillis = playerGravityMillis;
            this.monsterGravityMillis = monsterGravityMillis;
            this.monsterAttackBuff = monsterAttackBuff;
            this.monsterExtraGarbage = monsterExtraGarbage;
            this.monsterItemLevel = monsterItemLevel;
        }
    }
}

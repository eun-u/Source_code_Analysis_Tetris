package kr.ac.jbnu.se.tetris.battle;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import kr.ac.jbnu.se.tetris.ai.AIContext;
import kr.ac.jbnu.se.tetris.ai.AIProfile;
import kr.ac.jbnu.se.tetris.ai.BossPhaseWeightPolicy;
import kr.ac.jbnu.se.tetris.ai.DifficultyProfile;
import kr.ac.jbnu.se.tetris.ai.DifficultyProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.HeuristicWeights;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.ai.PolicyDecision;
import kr.ac.jbnu.se.tetris.character.CharacterCatalog;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameEngine;
import kr.ac.jbnu.se.tetris.core.PieceGenerator;
import kr.ac.jbnu.se.tetris.core.PieceType;
import kr.ac.jbnu.se.tetris.core.SevenBagGenerator;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** 시간, 탱크, PvP 기본형, 난이도 정책의 게임 규칙 회귀 검증. */
public final class RpgBattleRulesTest {
    public static void main(String[] args) {
        garbageTankTimingAndFifo();
        pvpForcesBasicAndInventoryOwnership();
        encounterProfilesAndBossBoundaries();
        actualItemPickupAndUse();
        perfectClearPreservesAutomaticItems();
        feverAndTimeWarpExpire();
        characterPassives();
    }

    private static void garbageTankTimingAndFifo() {
        GarbageTank tank = new GarbageTank(71);
        tank.enqueue(3, 100);
        int firstHole = tank.getPreviousHole();
        tank.enqueue(4, 900);
        check(tank.getWaitRemainingMillis(1900) == 200, "later attack does not reset first wait");
        check(tank.drainForPlacement(2099).isEmpty(), "before two seconds no garbage rises");
        check(tank.cancel(2) == 0 && tank.getPendingLines() == 5, "FIFO cancellation");
        List<GameAction.Garbage> risen = tank.drainForPlacement(2100);
        check(risen.size() == 2 && risen.get(0).getLines() == 1 && risen.get(1).getLines() == 3,
                "oldest chunk first, four-line cap and chunk boundaries preserved");
        check(risen.get(0).getHoleColumn() == firstHole, "first chunk keeps its own hole");
        check(tank.getPendingLines() == 1, "one line remains for next placement");
        check(tank.drainForPlacement(2100).get(0).getLines() == 1, "next placement drains rest");
        check(tank.getPendingLines() == 0 && tank.getPreviousHole() >= 0,
                "empty tank remembers last hole");
        tank.enqueue(1, 2200);
        check(tank.getWaitRemainingMillis(2200) == 2000, "new empty-to-nonempty period restarts wait");
        check(tank.removeOldestLine() && tank.getPendingLines() == 0,
                "defender passive can reduce the oldest chunk to zero");
    }

    private static void pvpForcesBasicAndInventoryOwnership() {
        CharacterCatalog catalog = CharacterCatalog.loadDefault();
        BattleManager pvp = BattleManager.pvp(Arrays.asList(
                new ParticipantSpec("a", "A", catalog.attacker()),
                new ParticipantSpec("b", "B", catalog.defender())), 7);
        ParticipantState a = pvp.getState().getParticipant("a");
        ParticipantState b = pvp.getState().getParticipant("b");
        check("student".equals(a.getCharacterId()) && "student".equals(b.getCharacterId()),
                "PvP ignores selected specialty characters");
        check(a.getMaxHp() == 100 && b.getMaxHp() == 100, "PvP HP also fixed at 100");
        check(a.getGravityMillis() == 500, "PvP starts at 500ms");
        check(BattleManager.pvpGravityAt(59999) == 500
                && BattleManager.pvpGravityAt(60000) == 450
                && BattleManager.pvpGravityAt(90000) == 400
                && BattleManager.pvpGravityAt(240000) == 200,
                "first acceleration at 60s, then every 30s, floor 200ms");
        check(pvp.start().isAccepted(), "PvP starts");
        BattleState before = pvp.getState();
        BattleResult noItem = pvp.submitItem("a", new GameAction.ItemUse("garbage_bomb", "b"));
        check(!noItem.isAccepted() && "ITEM_NOT_OWNED".equals(noItem.getReason()),
                "unowned item cannot be used");
        check(pvp.getState().getVersion() == before.getVersion(), "rejected use leaves state unchanged");
        BattleResult advancing = pvp.advance(499);
        check(advancing.isAccepted() && advancing.getState().getParticipant("a").getGameState().getTick() == 0,
                "499ms does not drop a cell");
        check(pvp.advance(1).getState().getParticipant("a").getGameState().getTick() == 1,
                "500ms drops exactly once");
    }

    private static void encounterProfilesAndBossBoundaries() {
        for (int level = 1; level <= 9; level++) {
            DifficultyProfile profile = DifficultyProfileCatalog.level(level);
            check(profile.getLevel() == level && profile.getAiStrength().getMaxSearchStates() > 0,
                    "level preset exists: " + level);
        }
        MonsterSpec first = StageCatalog.loadDefault().getStages().get(0).getEncounters().get(0);
        DifficultyProfile encounter = DifficultyProfileCatalog.forEncounter(first);
        check(encounter.getMonsterHp() == first.getHp(), "story HP preserved");
        AIProfile base = encounter.toAiProfile();
        GameEngine game = new GameEngine("monster", new SevenBagGenerator(1));
        game.dispatch(new GameAction(GameAction.Type.START, "monster", 1));
        for (double limit : new double[] {0, .1, .25}) {
            AIProfile profile = new AIProfile("boss_check", "BOSS_PHASE", HeuristicWeights.TETRIS,
                    900, 700, 40, limit);
            for (int hp : new int[] {100, 67, 66, 34, 33, 1}) {
                AIContext context = new AIContext("match", game.getState(), hp, 100,
                        new PlayerProfile().snapshot(), 1, null, null);
                PolicyDecision chosen = new BossPhaseWeightPolicy().decide(context, profile);
                check(profile.allows(chosen.getWeights()), "boss weights remain within profile bound");
                String expected = hp >= 67 ? "PREPARE" : hp >= 34 ? "PRESSURE" : "SURVIVE";
                check(expected.equals(chosen.getNextPolicyState()), "boss HP phase boundary " + hp);
            }
        }
        check(base.getMaxSearchStates() == encounter.getAiStrength().getMaxSearchStates(),
                "difficulty search capacity reaches AI profile");
    }

    private static void actualItemPickupAndUse() {
        BattleManager heal = squares(seedFor("heal", null), 10000);
        fillRows(heal, "a", false);
        check(heal.getState().getParticipant("a").getItems().contains("heal"), "item mino acquired by line clear");
        fillRows(heal, "b", false);
        check(heal.getState().getParticipant("a").getHp() == 60, "counterattack damaged player");
        check(heal.submitItem("a", new GameAction.ItemUse("heal", "a")).isAccepted(), "heal can be used");
        check(heal.getState().getParticipant("a").getHp() == 70
                && !heal.getState().getParticipant("a").getItems().contains("heal"),
                "one-use heal restores ten percent of max HP and frees slot");

        BattleManager bomb = squares(seedFor("garbage_bomb", null), 10000);
        fillRows(bomb, "a", false);
        fillRows(bomb, "b", false);
        fillRows(bomb, "b", false);
        check(bomb.getState().getParticipant("a").getPendingGarbageLines() == 8, "inbound queue exists");
        check(bomb.submitItem("a", new GameAction.ItemUse("garbage_bomb", "b")).isAccepted(),
                "bomb use accepted");
        check(bomb.getState().getParticipant("a").getPendingGarbageLines() == 7
                && bomb.getState().getParticipant("b").getPendingGarbageLines() == 0,
                "bomb cancels own oldest garbage before any remainder is sent");

        BattleManager fever = squares(seedFor("fever_charge", null), 10000);
        fillRows(fever, "a", false);
        check(fever.getState().getParticipant("a").getFever() == 5, "double clear charges five");
        check(fever.submitItem("a", new GameAction.ItemUse("fever_charge", "a")).isAccepted(),
                "fever charger can be used");
        check(fever.getState().getParticipant("a").getFever() == 25,
                "charger adds twenty after clear gain");

        BattleManager warp = squares(seedFor("time_warp", null), 10000);
        fillRows(warp, "a", false);
        check(warp.submitItem("a", new GameAction.ItemUse("time_warp", "a")).isAccepted(),
                "warp can be used");
        check(warp.getState().getParticipant("a").getGravityMillis() == 500
                && warp.getState().getParticipant("a").getTimeWarpRemainingMillis() == 5000
                && !warp.getState().getParticipant("a").getItems().contains("time_warp"),
                "warp consumes immediately and slows gravity by 100ms for five seconds");
        check(!warp.submitItem("a", new GameAction.ItemUse("time_warp", "a")).isAccepted(),
                "unowned repeated use rejected");

        BattleManager nullify = squares(seedFor("nullify", null), 10000);
        fillRows(nullify, "a", false);
        BattleState before = nullify.getState();
        check(!nullify.submitItem("a", new GameAction.ItemUse("nullify", "b")).isAccepted()
                && before.getParticipant("a").getItems().equals(nullify.getState().getParticipant("a").getItems()),
                "nullify with empty opposing inventory keeps item");
        fillRows(nullify, "b", false);
        check(nullify.submitItem("a", new GameAction.ItemUse("nullify", "b")).isAccepted(),
                "nullify removes one actual opposing item");
        check(nullify.getState().getParticipant("b").getItems().isEmpty(), "opposing slot emptied");

        BattleManager cleaner = squares(seedFor("line_cleaner", null), 10000);
        fillRows(cleaner, "a", false);
        check(!cleaner.submitItem("a", new GameAction.ItemUse("line_cleaner", "a")).isAccepted(),
                "cleaner on empty board has no effect");
        check(cleaner.getState().getParticipant("a").getItems().contains("line_cleaner"),
                "failed cleaner use is not consumed");
        fillRows(cleaner, "b", false);
        fillRows(cleaner, "b", false);
        cleaner.advance(2000);
        BattleResult eruption = cleaner.submit("a", GameAction.Type.HARD_DROP);
        check(eruption.isAccepted() && count(eruption, BattleEvent.Type.GARBAGE_RECEIVED) == 1,
                "mature tank erupts at next uncleared placement");
        BattleResult clean = cleaner.submitItem("a", new GameAction.ItemUse("line_cleaner", "a"));
        check(clean.isAccepted() && !cleaner.getState().getParticipant("a").getItems().contains("line_cleaner"),
                "cleaner removes one landed garbage row and consumes one use");
    }

    private static void perfectClearPreservesAutomaticItems() {
        BattleManager boost = squares(seedFor("damage_boost", null), 10000);
        fillRows(boost, "a", false);
        fillRows(boost, "b", false);
        check(boost.getState().getParticipant("a").getItems().contains("damage_boost"),
                "boost held before next attack");
        fillRows(boost, "a", false);
        check(boost.getState().getParticipant("a").getItems().contains("damage_boost"),
                "perfect clear leaves boost untouched");
        int before = boost.getState().getParticipant("b").getHp();
        fillRows(boost, "a", true);
        check(boost.getState().getParticipant("b").getHp() == before - 12,
                "next ordinary double gets +50% damage boost");

        BattleManager shield = squares(seedFor("shield", null), 10000);
        // Same deterministic item belongs to actor a; place b into defending role by switching attack order.
        fillRows(shield, "a", false);
        check(shield.getState().getParticipant("a").getItems().contains("shield"),
                "shield held before ordinary hit");
        fillRows(shield, "b", false);
        check(shield.getState().getParticipant("a").getItems().contains("shield"),
                "perfect clear leaves shield untouched");
        int hp = shield.getState().getParticipant("a").getHp();
        fillRows(shield, "b", true);
        check(shield.getState().getParticipant("a").getHp() == hp - 6,
                "next ordinary double applies thirty percent shield reduction");
        check(!shield.getState().getParticipant("a").getItems().contains("shield"),
                "shield consumed by ordinary HP hit");
    }

    private static void feverAndTimeWarpExpire() {
        Map<String, PieceGenerator> generators = new LinkedHashMap<String, PieceGenerator>();
        generators.put("a", new PieceGenerator() {
            private int issued;
            @Override public PieceType nextPiece() {
                return ++issued <= 100 ? PieceType.O : issued <= 102 ? PieceType.I : PieceType.O;
            }
        });
        generators.put("b", () -> PieceType.O);
        BattleManager fever = start(new BattleManager(Arrays.asList(
                new ParticipantSpec("a", "A", 100), new ParticipantSpec("b", "B", 10000)),
                7, generators));
        for (int i = 0; i < 20; i++) fillRows(fever, "a", false);
        check(fever.getState().getParticipant("a").isFeverActive()
                && fever.getState().getParticipant("a").getFeverRemainingMillis() == 10000,
                "twenty double clears auto-activate ten-second Fever");
        check(fever.submit("a", GameAction.Type.ROTATE_RIGHT).isAccepted(), "first I rotates flat");
        dropAt(fever, "a", 2);
        check(fever.submit("a", GameAction.Type.ROTATE_RIGHT).isAccepted(), "second I rotates flat");
        dropAt(fever, "a", 6);
        int beforeHp = fever.getState().getParticipant("b").getHp();
        int beforeGarbage = fever.getState().getParticipant("b").getPendingGarbageLines();
        dropAt(fever, "a", 8);
        check(fever.getState().getParticipant("b").getHp() == beforeHp - 5
                && fever.getState().getParticipant("b").getPendingGarbageLines() == beforeGarbage + 1,
                "active Fever adds twenty percent damage and one garbage on a single clear");
        check(fever.getState().getParticipant("a").getFever() == 0,
                "active Fever does not recharge from clears");
        check(fever.advance(9999).isAccepted(), "Fever time advances");
        check(fever.getState().getParticipant("a").getFeverRemainingMillis() == 1,
                "Fever remains through 9999ms");
        check(fever.advance(1).isAccepted()
                && !fever.getState().getParticipant("a").isFeverActive(),
                "Fever expires at exactly ten seconds");

        BattleManager warp = squares(seedFor("time_warp", null), 10000);
        fillRows(warp, "a", false);
        check(warp.submitItem("a", new GameAction.ItemUse("time_warp", "a")).isAccepted(),
                "warp activation");
        check(warp.advance(5000).isAccepted(), "warp time advances");
        check(warp.getState().getParticipant("a").getGravityMillis() == 400
                && warp.getState().getParticipant("a").getTimeWarpRemainingMillis() == 0,
                "warp expires at exactly five seconds");
    }

    private static void characterPassives() {
        CharacterCatalog catalog = CharacterCatalog.loadDefault();
        BattleManager defense = squares(9, catalog.defender(), 10000);
        fillRows(defense, "b", true);
        check(defense.getState().getParticipant("a").getPendingGarbageLines() == 1,
                "incoming one-line chunk for defender");
        fillRows(defense, "a", true);
        check(defense.getState().getParticipant("a").getPendingGarbageLines() == 0
                && defense.getState().getParticipant("b").getPendingGarbageLines() == 1,
                "defense passive removes last queued line before own attack cancellation");
        check(defense.getState().getParticipant("a").getMaxHp() == 150,
                "defender maximum HP is 150");

        BattleManager utility = squares(15, catalog.utility(), 10000);
        fillRows(utility, "a", true);
        check(utility.getState().getParticipant("a").getItemSlots() == 4
                && utility.getState().getParticipant("a").getItems().size() == 2,
                "utility gets four slots and one extra item on ordinary double clear");
    }

    private static BattleManager squares(long seed, int targetHp) {
        return squares(seed, CharacterCatalog.loadDefault().basic(), targetHp);
    }
    private static BattleManager squares(long seed, kr.ac.jbnu.se.tetris.character.CharacterSpec actor,
            int targetHp) {
        Map<String, PieceGenerator> generators = new LinkedHashMap<String, PieceGenerator>();
        generators.put("a", () -> PieceType.O);
        generators.put("b", () -> PieceType.O);
        return start(new BattleManager(Arrays.asList(new ParticipantSpec("a", "A", actor),
                new ParticipantSpec("b", "B", targetHp)), seed, generators));
    }
    private static BattleManager start(BattleManager battle) {
        check(battle.start().isAccepted(), "fixture battle starts");
        return battle;
    }
    private static void fillRows(BattleManager battle, String actor, boolean nonPerfect) {
        if (nonPerfect) dropAt(battle, actor, 0);
        for (int x : new int[] {0, 2, 4, 6, 8}) dropAt(battle, actor, x);
    }
    private static void dropAt(BattleManager battle, String actor, int x) {
        while (battle.getState().getParticipant(actor).getGameState().getPieceX() > x)
            check(battle.submit(actor, GameAction.Type.MOVE_LEFT).isAccepted(), "move left");
        while (battle.getState().getParticipant(actor).getGameState().getPieceX() < x)
            check(battle.submit(actor, GameAction.Type.MOVE_RIGHT).isAccepted(), "move right");
        check(battle.submit(actor, GameAction.Type.HARD_DROP).isAccepted(), "drop O");
    }
    private static long seedFor(String aItem, String bItem) {
        List<String> items = Arrays.asList("damage_boost", "garbage_bomb", "heal", "shield",
                "line_cleaner", "fever_charge", "time_warp", "nullify");
        Random candidates = new Random(991);
        for (int attempt = 0; attempt < 100000; attempt++) {
            long seed = attempt < 1000 ? attempt : candidates.nextLong();
            Random a = new Random(seed ^ (long) "a".hashCode());
            Random b = new Random(seed ^ (long) "b".hashCode());
            if (!items.get(a.nextInt(8)).equals(aItem)) continue;
            if (bItem != null && !items.get(b.nextInt(8)).equals(bItem)) continue;
            return seed;
        }
        throw new AssertionError("Item seed not found");
    }
    private static int count(BattleResult result, BattleEvent.Type type) {
        int count = 0;
        for (BattleEvent event : result.getEvents()) if (event.getType() == type) count++;
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

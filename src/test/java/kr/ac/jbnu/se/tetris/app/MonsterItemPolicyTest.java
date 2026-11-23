package kr.ac.jbnu.se.tetris.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kr.ac.jbnu.se.tetris.battle.BattleSnapshots;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.item.ItemIds;
import kr.ac.jbnu.se.tetris.support.SampleSnapshots;

/** Monster item choices preserve inventory order and the existing effect/target rules. */
public final class MonsterItemPolicyTest {
    public static void main(String[] args) {
        skipsAutomaticAndCurrentlyIneffectiveItems();
        choosesTargetAndTriesNextInventoryItemAfterRejection();
        System.out.println("PASS MonsterItemPolicyTest");
    }

    private static void skipsAutomaticAndCurrentlyIneffectiveItems() {
        List<String> inventory = Arrays.asList(ItemIds.DAMAGE_BOOST, ItemIds.SHIELD, ItemIds.HEAL,
                ItemIds.FEVER_CHARGE, ItemIds.TIME_WARP, ItemIds.NULLIFY,
                ItemIds.GARBAGE_BOMB, ItemIds.LINE_CLEANER);
        ParticipantState monster = participant("monster", 100, inventory, 200, 200);
        ParticipantState player = participant("local", 100, Collections.emptyList(), 0, 0);
        List<GameAction.ItemUse> choices = MonsterItemPolicy.candidates(monster, player);
        check(choices.size() == 2, "Only usable manual items remain");
        choice(choices.get(0), ItemIds.GARBAGE_BOMB, "local");
        choice(choices.get(1), ItemIds.LINE_CLEANER, "monster");
    }

    private static void choosesTargetAndTriesNextInventoryItemAfterRejection() {
        List<String> inventory = Arrays.asList(ItemIds.HEAL, ItemIds.FEVER_CHARGE,
                ItemIds.TIME_WARP, ItemIds.NULLIFY, ItemIds.GARBAGE_BOMB);
        ParticipantState monster = participant("monster", 60, inventory, 0, 0);
        ParticipantState player = participant("local", 100,
                Collections.singletonList(ItemIds.SHIELD), 0, 0);
        List<GameAction.ItemUse> choices = MonsterItemPolicy.candidates(monster, player);
        check(choices.size() == inventory.size(), "Every eligible item remains available for fallback");
        for (int i = 0; i < inventory.size(); i++) {
            String target = i < 3 ? "monster" : "local";
            choice(choices.get(i), inventory.get(i), target);
        }
    }

    private static ParticipantState participant(String id, int hp, List<String> items,
            long feverRemainingMillis, long timeWarpRemainingMillis) {
        GameState game = SampleSnapshots.running("local", "monster").getParticipant(id).getGameState();
        return BattleSnapshots.participant(id, id, hp, 100, game, false,
                "student", Math.max(3, items.size()), items, 0, feverRemainingMillis,
                timeWarpRemainingMillis, 0, 0, 500, 0, 0);
    }

    private static void choice(GameAction.ItemUse actual, String item, String target) {
        check(item.equals(actual.getItemId()) && target.equals(actual.getTargetActorId()),
                "Choice preserves item identity and target");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

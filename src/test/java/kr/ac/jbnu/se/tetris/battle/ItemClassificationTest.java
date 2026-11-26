package kr.ac.jbnu.se.tetris.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.ac.jbnu.se.tetris.item.ItemIds;
import kr.ac.jbnu.se.tetris.item.ItemSpec;

/** 아이템 분류 규칙(DS-03): 자동 발동·사용 대상·효과 없음 판정이 한 곳을 기준으로 일관되는지 검증 */
public final class ItemClassificationTest {
    public static void main(String[] args) {
        automaticAndTargetClassification();
        rejectionFollowsClassification();
        noEffectTruthTable();
        monsterLevelsGrowMonotonically();
    }

    private static void automaticAndTargetClassification() {
        Set<String> automatic = new HashSet<String>(Arrays.asList(ItemIds.DAMAGE_BOOST, ItemIds.SHIELD));
        Set<String> opponent = new HashSet<String>(Arrays.asList(ItemIds.GARBAGE_BOMB, ItemIds.NULLIFY));
        for (String id : ItemIds.ALL) {
            check(ItemSpec.isAutomatic(id) == automatic.contains(id), "Automatic classification: " + id);
            check((ItemSpec.targetOf(id) == ItemSpec.Target.OPPONENT) == opponent.contains(id),
                    "Target classification: " + id);
        }
        check(!ItemSpec.isAutomatic(null) && ItemSpec.targetOf(null) == ItemSpec.Target.SELF, "Unknown id is plain self item");
    }

    private static void rejectionFollowsClassification() {
        List<String> owned = new ArrayList<String>(ItemIds.ALL);
        for (String id : ItemIds.ALL) {
            String onSelf = ItemRules.rejection(-1, owned, true, false, false, id);
            String onOpponent = ItemRules.rejection(-1, owned, false, false, false, id);
            if (ItemSpec.isAutomatic(id)) {
                check("AUTO_ITEM".equals(onSelf) && "AUTO_ITEM".equals(onOpponent), "Automatic item is never used directly: " + id);
            } else if (ItemSpec.targetOf(id) == ItemSpec.Target.OPPONENT) {
                check(ItemRules.INVALID_TARGET.equals(onSelf) && onOpponent == null, "Offensive item targets the opponent: " + id);
            } else {
                check(onSelf == null && ItemRules.INVALID_TARGET.equals(onOpponent), "Support item targets self: " + id);
            }
        }
        check("ITEM_NOT_OWNED".equals(ItemRules.rejection(-1, new ArrayList<String>(), true, false, false, ItemIds.HEAL)),
                "Not owned");
        check("ITEM_RESTRICTED".equals(ItemRules.rejection(1, owned, false, false, false, ItemIds.GARBAGE_BOMB)),
                "Restricted by monster level");
    }

    private static void noEffectTruthTable() {
        List<String> owned = new ArrayList<String>(ItemIds.ALL);
        check(ItemRules.hasNoEffect(ItemIds.HEAL, true, false) && !ItemRules.hasNoEffect(ItemIds.HEAL, false, true),
                "Heal is wasted only at full HP");
        check(ItemRules.hasNoEffect(ItemIds.TIME_WARP, false, true) && !ItemRules.hasNoEffect(ItemIds.TIME_WARP, true, false),
                "Time warp is wasted only while running");
        for (String id : ItemIds.ALL) {
            if (ItemIds.HEAL.equals(id) || ItemIds.TIME_WARP.equals(id)) continue;
            check(!ItemRules.hasNoEffect(id, true, true), "No other item can be wasted: " + id);
        }
        check("NO_EFFECT".equals(ItemRules.rejection(-1, owned, true, true, false, ItemIds.HEAL)), "Heal at full HP rejected");
        check("NO_EFFECT".equals(ItemRules.rejection(-1, owned, true, false, true, ItemIds.TIME_WARP)), "Active warp rejected");
        check(ItemRules.rejection(-1, owned, true, false, false, ItemIds.HEAL) == null, "Heal below full HP accepted");
    }

    private static void monsterLevelsGrowMonotonically() {
        for (int level = 0; level < 5; level++) {
            check(ItemRules.monsterItems(level + 1).containsAll(ItemRules.monsterItems(level)),
                    "Higher monster level keeps lower level items: " + level);
        }
        check(ItemRules.monsterItems(5).containsAll(ItemIds.ALL), "Top level allows every item");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

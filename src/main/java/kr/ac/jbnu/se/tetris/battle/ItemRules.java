package kr.ac.jbnu.se.tetris.battle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kr.ac.jbnu.se.tetris.item.ItemIds;

/** 아이템 사용 규칙: 몬스터 레벨별 허용 아이템과, 사용 요청이 규칙상 거절되는 사유를 판정한다. */
final class ItemRules {
    static final String INVALID_TARGET = "INVALID_TARGET";

    private ItemRules() { }

    static boolean isKnown(String id) {
        return ItemIds.ALL.contains(id);
    }

    /** 몬스터 아이템 레벨이 허용하는 아이템. 레벨은 0~5. */
    static List<String> monsterItems(int level) {
        switch (level) {
            case 0: return Collections.emptyList();
            case 1: return Collections.singletonList(ItemIds.HEAL);
            case 2: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD);
            case 3: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD, ItemIds.FEVER_CHARGE, ItemIds.LINE_CLEANER);
            case 4: return Arrays.asList(ItemIds.HEAL, ItemIds.SHIELD, ItemIds.FEVER_CHARGE, ItemIds.LINE_CLEANER,
                    ItemIds.DAMAGE_BOOST, ItemIds.TIME_WARP);
            case 5: return ItemIds.ALL;
            default: throw new IllegalArgumentException("Invalid monster item level");
        }
    }

    /**
     * 규칙상의 거절 사유를 검사 순서대로 돌려준다. 문제가 없으면 null.
     * 순서: 레벨 제한, 보유 여부, 자동 아이템, 대상 종류, 효과 없음.
     */
    static String rejection(int itemLevel, List<String> ownedItems, boolean selfTarget,
                            boolean fullHp, boolean warpActive, String id) {
        if (itemLevel >= 0 && !monsterItems(itemLevel).contains(id)) return "ITEM_RESTRICTED";
        if (!isKnown(id) || !ownedItems.contains(id)) return "ITEM_NOT_OWNED";
        if (isAutomatic(id)) return "AUTO_ITEM";
        if (isOffensive(id) == selfTarget) return INVALID_TARGET;
        return hasNoEffect(id, fullHp, warpActive) ? "NO_EFFECT" : null;
    }

    /** 조건이 맞으면 자동으로 쓰여 직접 사용할 수 없는 아이템 */
    private static boolean isAutomatic(String id) {
        return ItemIds.DAMAGE_BOOST.equals(id) || ItemIds.SHIELD.equals(id);
    }

    /** 상대에게만 쓸 수 있는 공격형 아이템. 나머지는 자신에게만 쓴다. */
    private static boolean isOffensive(String id) {
        return ItemIds.GARBAGE_BOMB.equals(id) || ItemIds.NULLIFY.equals(id);
    }

    private static boolean hasNoEffect(String id, boolean fullHp, boolean warpActive) {
        return (ItemIds.HEAL.equals(id) && fullHp) || (ItemIds.TIME_WARP.equals(id) && warpActive);
    }
}

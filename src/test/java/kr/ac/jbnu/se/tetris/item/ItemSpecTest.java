package kr.ac.jbnu.se.tetris.item;

import java.util.Arrays;
import java.util.List;

// 아이템 자동 사용 및 대상 선택 공통 규칙 검증.
public final class ItemSpecTest {

    private ItemSpecTest() {
    }

    public static void main(String[] args) {

        // 1. 자동 사용 아이템 검증
        check(ItemSpec.isAutomatic(ItemIds.DAMAGE_BOOST),
                "공격 증폭 아이템은 자동 사용 대상이어야 한다.");

        check(ItemSpec.isAutomatic(ItemIds.SHIELD),
                "보호막 아이템은 자동 사용 대상이어야 한다.");

        // 2. 수동 사용 아이템 검증
        List<String> manualItems = Arrays.asList(
                ItemIds.GARBAGE_BOMB,
                ItemIds.HEAL,
                ItemIds.LINE_CLEANER,
                ItemIds.FEVER_CHARGE,
                ItemIds.TIME_WARP,
                ItemIds.NULLIFY
        );

        for (String item : manualItems) {
            check(!ItemSpec.isAutomatic(item),
                    item + "은 수동 사용 아이템이어야 한다.");
        }

        // 3. 상대방 대상 아이템 검증
        check(ItemSpec.targetOf(ItemIds.GARBAGE_BOMB)
                        == ItemSpec.Target.OPPONENT,
                "가비지 폭탄은 상대방에게 사용해야 한다.");

        check(ItemSpec.targetOf(ItemIds.NULLIFY)
                        == ItemSpec.Target.OPPONENT,
                "아이템 무효화는 상대방에게 사용해야 한다.");

        // 4. 자신 대상 아이템 검증
        List<String> selfItems = Arrays.asList(
                ItemIds.DAMAGE_BOOST,
                ItemIds.HEAL,
                ItemIds.SHIELD,
                ItemIds.LINE_CLEANER,
                ItemIds.FEVER_CHARGE,
                ItemIds.TIME_WARP
        );

        for (String item : selfItems) {
            check(ItemSpec.targetOf(item) == ItemSpec.Target.SELF,
                    item + "은 자신에게 사용해야 한다.");
        }

        // 5. 잘못된 아이템 ID 검증
        boolean rejected = false;

        try {
            ItemSpec.targetOf("unknown_item");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        check(rejected, "등록되지 않은 아이템 ID는 거부해야 한다.");

        System.out.println("PASS ItemSpecTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
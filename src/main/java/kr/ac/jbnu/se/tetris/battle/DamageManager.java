package kr.ac.jbnu.se.tetris.battle;

import kr.ac.jbnu.se.tetris.core.GameEvent;

/** LINE_CLEAR 이벤트 한 건에서 공격 한 건을 산출하는 1단계 공격 규칙 */
public final class DamageManager {
    private static final int[] BASE_DAMAGE = {0, 4, 8, 12, 20};
    private static final int[] BASE_GARBAGE = {0, 0, 1, 2, 4};

    public Attack forLineClear(GameEvent event) {
        if (event == null || event.getType() != GameEvent.Type.LINE_CLEAR) {
            throw new IllegalArgumentException("LINE_CLEAR event required");
        }
        return calculate(event.getLineCount(), event.getCombo(), event.isTSpin());
    }

    Attack calculate(int lines, int combo, boolean tSpin) {
        if (lines < 1 || lines > 4) {
            throw new IllegalArgumentException("A placement may clear 1 to 4 lines");
        }
        int baseDamage = BASE_DAMAGE[lines];
        int baseGarbage = BASE_GARBAGE[lines];
        int nonnegativeCombo = Math.max(combo, 0);
        int damage = baseDamage * (tSpin ? 2 : 1) + 2 * Math.min(nonnegativeCombo, 5);
        int garbage = baseGarbage + (tSpin ? 2 : 0) + (nonnegativeCombo >= 2 ? 1 : 0);
        return new Attack(damage, garbage);
    }

    public static final class Attack {
        private final int damage;
        private final int garbageLines;

        Attack(int damage, int garbageLines) {
            this.damage = damage;
            this.garbageLines = garbageLines;
        }

        public int getDamage() { return damage; }
        public int getGarbageLines() { return garbageLines; }
    }
}

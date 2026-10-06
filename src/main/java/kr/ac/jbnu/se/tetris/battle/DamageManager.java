package kr.ac.jbnu.se.tetris.battle;

import kr.ac.jbnu.se.tetris.core.GameEvent;

/** LINE_CLEAR 이벤트 한 건에서 공격 한 건을 산출하며 수치는 battle/balance.properties에서 조회 */
public final class DamageManager {
    private final BattleBalance balance;

    /** battle/balance.properties의 기본 수치로 생성 */
    public DamageManager() { this(BattleBalance.loadDefault()); }

    /** 테스트나 난이도별 수치 주입을 위한 지정 수치 생성 */
    public DamageManager(BattleBalance balance) {
        if (balance == null) throw new IllegalArgumentException("Battle balance is required");
        this.balance = balance;
    }

    public Attack forLineClear(GameEvent event) {
        if (event == null || event.getType() != GameEvent.Type.LINE_CLEAR) {
            throw new IllegalArgumentException("LINE_CLEAR event required");
        }
        return calculate(event.getLineCount(), event.getCombo(), event.isTSpin());
    }

    /** 줄 수·콤보·T-Spin 여부에서 공격 한 건을 계산하는 순수 계산 */
    Attack calculate(int lines, int combo, boolean tSpin) {
        if (lines < 1 || lines > 4) {
            throw new IllegalArgumentException("A placement may clear 1 to 4 lines");
        }
        // 피해 = 기본피해[줄 수] * (T-Spin 배율 또는 1) + 콤보당피해 * min(콤보, 콤보상한)
        // 가비지 = 기본가비지[줄 수] + (T-Spin 보너스) + (콤보 >= 기준이면 콤보 보너스)
        int baseDamage = balance.damageForLines(lines);
        int baseGarbage = balance.garbageForLines(lines);
        // core의 콤보는 줄 제거가 없으면 -1이므로 음수는 0으로 보정
        int nonnegativeCombo = Math.max(combo, 0);
        // T-Spin 배율은 기본 피해에만 곱하고 콤보 보너스는 상한까지만 반영
        int damage = baseDamage * (tSpin ? balance.getTSpinDamageMultiplier() : 1)
                + balance.getComboDamagePerStep() * Math.min(nonnegativeCombo, balance.getComboCap());
        int garbage = baseGarbage + (tSpin ? balance.getTSpinGarbageBonus() : 0)
                + (nonnegativeCombo >= balance.getComboGarbageThreshold() ? balance.getComboGarbageBonus() : 0);
        return new Attack(damage, garbage);
    }

    /** 상대 HP에서 차감할 피해와 상대에게 보낼 가비지 줄 수를 담은 불변 결과 */
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

package kr.ac.jbnu.se.tetris.battle;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** balance.properties의 기본값 보존, 수치 변경 반영, 잘못된 설정의 거절을 확인 */
public final class BattleBalanceTest {
    // balance.properties 기본값과 같은 정상 설정, 거절 시험은 이 문자열의 한 항목만 바꿔 사용
    private static final String VALID = "damage.lines=0,4,8,12,20\n"
            + "garbage.lines=0,0,1,2,4\n"
            + "damage.combo.perStep=2\n"
            + "damage.combo.cap=5\n"
            + "garbage.combo.threshold=2\n"
            + "garbage.combo.bonus=1\n"
            + "damage.tspin.multiplier=2\n"
            + "garbage.tspin.bonus=2\n";

    public static void main(String[] args) {
        defaultFileKeepsG0Rules();
        changedValuesReachDamageRules();
        invalidSettingsAreRejected();
    }

    /** 수치 분리 전 기존 규칙과 같은 결과를 내는지 확인, BattleManagerTest.damageRules와 같은 기대값 사용 */
    private static void defaultFileKeepsG0Rules() {
        BattleBalance balance = BattleBalance.loadDefault();
        check(balance.damageForLines(1) == 4 && balance.damageForLines(4) == 20, "default damage table");
        check(balance.garbageForLines(2) == 1 && balance.garbageForLines(4) == 4, "default garbage table");
        DamageManager manager = new DamageManager(balance);
        check(manager.calculate(1, 0, false).getDamage() == 4, "single damage");
        check(manager.calculate(2, 0, false).getGarbageLines() == 1, "double garbage");
        // 3줄 + 콤보 2 + T-Spin: 피해 12*2 + 2*2 = 28, 가비지 2 + 2 + 1 = 5
        check(manager.calculate(3, 2, true).getDamage() == 28, "T-spin plus combo damage");
        check(manager.calculate(3, 2, true).getGarbageLines() == 5, "T-spin plus combo garbage");
        // 콤보 20도 상한 5까지만 반영: 20 + 2*5 = 30
        check(manager.calculate(4, 20, false).getDamage() == 30, "combo bonus capped");
        // core는 줄 제거가 없을 때 콤보를 -1로 두므로 음수는 0으로 보정되어야 함
        check(manager.calculate(1, -1, false).getDamage() == 4, "negative combo ignored");
    }

    /** 파일의 수치를 바꾸면 계산 결과에 그대로 반영되는지 확인, 수치가 코드에 박혀 있지 않다는 증거 */
    private static void changedValuesReachDamageRules() {
        // 바꾸는 값: 기본 피해 표, 콤보 상한 5→3, T-Spin 배율 2→3, 콤보 가비지 보너스 1→2
        String tuned = VALID.replace("damage.lines=0,4,8,12,20", "damage.lines=0,5,10,15,30")
                .replace("damage.combo.cap=5", "damage.combo.cap=3")
                .replace("damage.tspin.multiplier=2", "damage.tspin.multiplier=3")
                .replace("garbage.combo.bonus=1", "garbage.combo.bonus=2");
        DamageManager manager = new DamageManager(load(tuned));
        check(manager.calculate(1, 0, false).getDamage() == 5, "tuned single damage");
        // 콤보 9를 줘도 3단계까지만 반영: 5 + 2*3
        check(manager.calculate(1, 9, false).getDamage() == 5 + 2 * 3, "tuned combo cap");
        // 2줄 기본 피해 10 * 배율 3
        check(manager.calculate(2, 0, true).getDamage() == 30, "tuned T-spin multiplier");
        // 2줄 기본 가비지 1 + 콤보 보너스 2 (콤보 2는 기준 2 이상)
        check(manager.calculate(2, 2, false).getGarbageLines() == 1 + 2, "tuned combo garbage bonus");
    }

    /** 잘못된 설정을 읽는 시점에 거절하는지 확인, 정상 설정에서 한 항목만 틀리게 바꿔 시험 */
    private static void invalidSettingsAreRejected() {
        // 비교 기준인 정상 설정이 읽혀야 아래 거절 시험이 의미를 가짐
        check(load(VALID) != null, "valid fixture loads");
        reject(VALID.replace("damage.lines=0,4,8,12,20", "damage.lines=0,4,8,12"), "wrong entry count");
        reject(VALID.replace("damage.lines=0,4,8,12,20", "damage.lines=1,4,8,12,20"), "first entry must be zero");
        reject(VALID.replace("damage.lines=0,4,8,12,20", "damage.lines=0,4,-8,12,20"), "negative value");
        reject(VALID.replace("damage.combo.cap=5", "damage.combo.cap=abc"), "non-numeric");
        reject(VALID.replace("damage.tspin.multiplier=2", "damage.tspin.multiplier=0"), "multiplier below one");
        reject(VALID.replace("garbage.combo.threshold=2", "garbage.combo.threshold=0"), "threshold below one");
        // 가비지 최댓값 4 + T-Spin 20 + 콤보 1 = 25가 22줄을 넘음
        reject(VALID.replace("garbage.tspin.bonus=2", "garbage.tspin.bonus=20"), "garbage above 22 lines");
        reject(VALID + "damage.typo=1\n", "unknown key");
        reject(VALID.replace("damage.combo.cap=5\n", ""), "missing key");
        reject("", "empty file");
        try {
            BattleBalance.load(null);
            throw new IllegalStateException("null input accepted");
        } catch (IllegalArgumentException expected) {
            // 입력 스트림 누락의 명시적 거절
        }
    }

    private static BattleBalance load(String text) {
        return BattleBalance.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** 예외 없이 읽히면 잘못된 설정을 받아들인 것이므로 message와 함께 실패 처리 */
    private static void reject(String text, String message) {
        try {
            load(text);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new IllegalStateException("Invalid balance accepted: " + message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

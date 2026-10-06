package kr.ac.jbnu.se.tetris.battle;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.jbnu.se.tetris.character.CharacterCatalog;
import kr.ac.jbnu.se.tetris.character.CharacterSpec;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.PieceGenerator;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** HP 피해 공식의 버프 합·방어·반올림·퍼펙트 클리어 처리와 캐릭터 버프의 전투 연결을 확인 */
public final class DamageManagerTest {
    public static void main(String[] args) {
        buffsAreSummedNotMultiplied();
        defenseRoundingAndFloor();
        garbageIgnoresBuffs();
        perfectClearIgnoresEverything();
        attackerCharacterBuffReachesBattle();
        participantSpecKeepsCharacter();
    }

    /** 공격 버프는 곱하지 않고 합산, 규칙 문서의 최대 조합 예시(공격형+피해 증가+Fever) 확인 */
    private static void buffsAreSummedNotMultiplied() {
        DamageManager manager = new DamageManager();
        // 4줄 20 + 4콤보 12 = 32, 버프 합 0.5 + 0.5 + 0.2 = 1.2이므로 32 * 2.2 = 70.4 -> 70
        check(manager.calculate(4, 4, false, false, 1.2, 0.0).getDamage() == 70, "summed buffs 2.2x");
        // 곱셈이었다면 32 * 1.5 * 1.5 * 1.2 = 86.4가 되어 달라야 함
        check(manager.calculate(1, 0, false, false, 0.5, 0.0).getDamage() == 6, "single with attacker buff 4 * 1.5");
        check(manager.calculate(1, 0, false, false, 0.0, 0.0).getDamage() == 4, "no buff keeps base");
    }

    /** 방어는 합산에서 빼고 결과는 반올림, 최소 0 */
    private static void defenseRoundingAndFloor() {
        DamageManager manager = new DamageManager();
        // 8 * (1 - 0.3) = 5.6 -> 6 (반올림)
        check(manager.calculate(2, 0, false, false, 0.0, 0.3).getDamage() == 6, "defense rounds half up");
        // 4 * 1.2 = 4.8 -> 5
        check(manager.calculate(1, 0, false, false, 0.2, 0.0).getDamage() == 5, "fever buff rounds up");
        // 공격 0.5와 방어 0.3은 합산: 8 * 1.2 = 9.6 -> 10
        check(manager.calculate(2, 0, false, false, 0.5, 0.3).getDamage() == 10, "buff minus defense");
        // 방어가 커서 배율이 음수가 되어도 피해는 0 아래로 내려가지 않음
        check(manager.calculate(4, 4, false, false, 0.0, 3.0).getDamage() == 0, "damage floors at zero");
    }

    /** 가비지 줄 수는 버프와 방어의 영향을 받지 않음 */
    private static void garbageIgnoresBuffs() {
        DamageManager manager = new DamageManager();
        int plain = manager.calculate(3, 2, false, false, 0.0, 0.0).getGarbageLines();
        check(plain == 3, "three lines 2 + two combos 1");
        check(manager.calculate(3, 2, false, false, 2.2, 0.0).getGarbageLines() == plain, "buff keeps garbage");
        check(manager.calculate(3, 2, false, false, 0.0, 0.3).getGarbageLines() == plain, "defense keeps garbage");
    }

    /** 퍼펙트 클리어는 콤보·버프·방어·T-Spin과 줄 수를 모두 무시한 고정값 */
    private static void perfectClearIgnoresEverything() {
        DamageManager manager = new DamageManager();
        for (int lines = 1; lines <= 4; lines++) {
            DamageManager.Attack attack = manager.calculate(lines, 9, true, true, 2.2, 0.3);
            check(attack.getDamage() == 40 && attack.getGarbageLines() == 8, "perfect clear constants: " + lines);
        }
    }

    /** 공격형 캐릭터의 버프가 실제 전투 피해에 반영되고 가비지는 그대로인지 확인 */
    private static void attackerCharacterBuffReachesBattle() {
        CharacterCatalog catalog = CharacterCatalog.loadDefault();
        int basicHp = doubleClearDamage(catalog.basic());
        int attackerHp = doubleClearDamage(catalog.attacker());
        check(basicHp == 100 - 8, "basic double clear damage 8");
        check(attackerHp == 100 - 12, "attacker double clear damage 8 * 1.5 = 12");
    }

    /** 퍼펙트가 아닌 더블 클리어를 만들어 상대 HP를 읽음, 먼저 쌓은 블록 하나가 남아 보드가 비지 않음 */
    private static int doubleClearDamage(CharacterSpec attacker) {
        Map<String, PieceGenerator> generators = new LinkedHashMap<String, PieceGenerator>();
        generators.put("a", constant(PieceType.O));
        generators.put("b", constant(PieceType.O));
        BattleManager battle = new BattleManager(Arrays.asList(new ParticipantSpec("a", "A", attacker),
                new ParticipantSpec("b", "B", CharacterSpec.DEFAULT)), 7, generators);
        check(battle.start().isAccepted(), "start");
        for (int x : new int[] {0, 0, 2, 4, 6, 8}) {
            while (battle.getState().getParticipant("a").getGameState().getPieceX() > x) {
                check(battle.submit("a", GameAction.Type.MOVE_LEFT).isAccepted(), "move left");
            }
            while (battle.getState().getParticipant("a").getGameState().getPieceX() < x) {
                check(battle.submit("a", GameAction.Type.MOVE_RIGHT).isAccepted(), "move right");
            }
            check(battle.submit("a", GameAction.Type.HARD_DROP).isAccepted(), "drop");
        }
        ParticipantState target = battle.getState().getParticipant("b");
        check(target.getGameState().getPendingGarbageLines() == 1, "double clear sends one garbage line");
        return target.getHp();
    }

    private static void participantSpecKeepsCharacter() {
        CharacterSpec defender = CharacterCatalog.loadDefault().defender();
        ParticipantSpec spec = new ParticipantSpec("d", "D", defender);
        check(spec.getCharacter() == defender && spec.getMaxHp() == 150, "spec keeps character and HP 150");
        ParticipantSpec plain = new ParticipantSpec("p", "P", 65);
        check(plain.getMaxHp() == 65 && plain.getCharacter().getDamageBuff() == 0.0, "plain HP spec has no buff");
    }

    private static PieceGenerator constant(final PieceType type) {
        return new PieceGenerator() { public PieceType nextPiece() { return type; } };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

package kr.ac.jbnu.se.tetris.battle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 참가자 역할 규칙(DS-04): PvE는 목록의 첫 참가자가 플레이어이고 나머지가 몬스터, PvP는 모두 플레이어 */
public final class ParticipantRoleTest {
    public static void main(String[] args) {
        pveFirstParticipantIsPlayer();
        reversingTheListSwapsRoles();
        pvpTreatsEveryoneAsPlayer();
        defaultBattleUsesSameGravityForEveryone();
    }

    private static void pveFirstParticipantIsPlayer() {
        BattleState state = BattleManager.pve(specs("p0", "m1"), 7, 700, 300, 0.5).getState();
        check(state.getParticipant("p0").getGravityMillis() == 700, "First PvE participant uses player gravity");
        check(state.getParticipant("m1").getGravityMillis() == 300, "Second PvE participant uses monster gravity");
    }

    private static void reversingTheListSwapsRoles() {
        BattleState state = BattleManager.pve(specs("m1", "p0"), 7, 700, 300, 0.5).getState();
        check(state.getParticipant("m1").getGravityMillis() == 700, "Role follows list position, not the ID");
        check(state.getParticipant("p0").getGravityMillis() == 300, "Later participant becomes the monster");
    }

    private static void pvpTreatsEveryoneAsPlayer() {
        BattleState state = BattleManager.pvp(specs("a", "b", "c"), 7).getState();
        for (ParticipantState participant : state.getParticipants().values()) {
            check(participant.getGravityMillis() == 500, "PvP participants share one gravity: " + participant.getId());
        }
    }

    private static void defaultBattleUsesSameGravityForEveryone() {
        BattleState state = new BattleManager(specs("a", "b"), 7).getState();
        check(state.getParticipant("a").getGravityMillis() == 400
                && state.getParticipant("b").getGravityMillis() == 400, "Default battle gravity");
    }

    private static List<ParticipantSpec> specs(String... ids) {
        List<ParticipantSpec> list = new ArrayList<ParticipantSpec>();
        for (String id : ids) list.add(new ParticipantSpec(id, id.toUpperCase(), 100));
        return Collections.unmodifiableList(list);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

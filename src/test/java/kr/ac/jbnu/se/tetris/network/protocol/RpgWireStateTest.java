package kr.ac.jbnu.se.tetris.network.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.jbnu.se.tetris.battle.BattleSnapshots;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.controller.PlayerController;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameEngine;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.SevenBagGenerator;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;

/** 실제 코어가 만든 아이템 미노와 전투 상태가 네트워크에서도 그대로 보이는지 검증. */
public final class RpgWireStateTest {
    public static void main(String[] args) throws Exception {
        GameEngine player = new GameEngine("p", new SevenBagGenerator(21));
        player.configureItemSpawns(1, 4);
        PlayerController input = new PlayerController(player, "p");
        check(input.submit(GameAction.Type.START).isAccepted(), "시작");
        check(player.getState().getActivePiece().getItemId() != null, "아이템 미노 생성");
        input.submit(GameAction.Type.HARD_DROP);
        input.submit(GameAction.Type.HOLD);
        GameState game = player.getState();
        check(game.getHoldItemId() != null, "HOLD 아이템 유지");
        GameEngine other = new GameEngine("m", new SevenBagGenerator(21));
        new PlayerController(other, "m").submit(GameAction.Type.START);
        Map<String, ParticipantState> participants = new LinkedHashMap<String, ParticipantState>();
        participants.put("p", BattleSnapshots.participant("p", "학생", 83, 100, game, false,
                "student", 3, Arrays.asList("heal", "time_warp", "shield"),
                100, 4000, 3000, 4, 1500, 550, 5, 74));
        participants.put("m", BattleSnapshots.participant("m", "몬스터", 95, 100,
                other.getState(), false));
        BattleState expected = BattleSnapshots.battle(BattleState.Status.RUNNING, 30,
                participants, null, null, 61234);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WireCodec.writeUpdate(bytes, NetworkUpdate.snapshot("room", "match", expected));
        BattleState actual = WireCodec.readUpdate(new ByteArrayInputStream(bytes.toByteArray())).getBattleState();
        ParticipantState p = actual.getParticipant("p");
        check(actual.getElapsedMillis() == 61234, "전투 시간 보존");
        check(p.getItems().equals(Arrays.asList("heal", "time_warp", "shield")), "슬롯 순서 보존");
        check(p.isFeverActive() && p.getFeverRemainingMillis() == 4000
                && p.getTimeWarpRemainingMillis() == 3000, "지속 효과 보존");
        check(p.getPendingGarbageLines() == 4 && p.getGarbageWaitRemainingMillis() == 1500
                && p.getGravityMillis() == 550 && p.getMaxCombo() == 5 && p.getTotalDamage() == 74,
                "가비지·속도·결과 통계 보존");
        GameState decoded = p.getGameState();
        check(game.getHoldItemId().equals(decoded.getHoldItemId()), "HOLD 메타데이터 보존");
        check(game.getActivePiece().getItemId().equals(decoded.getActivePiece().getItemId()),
                "활성 미노 메타데이터 보존");
        int marks = 0;
        for (int y = 0; y < 22; y++) for (int x = 0; x < 10; x++) {
            String before = game.getBoard().getItemId(x, y);
            String after = decoded.getBoard().getItemId(x, y);
            check(before == null ? after == null : before.equals(after), "정착 미노 메타데이터 보존");
            if (before != null) marks++;
        }
        check(marks == 4, "정착 미노 네 칸의 아이템 표시 유지");
        System.out.println("PASS RpgWireStateTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

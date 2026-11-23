package kr.ac.jbnu.se.tetris.app;

import java.util.ArrayList;
import java.util.List;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.item.ItemIds;

/** Chooses manual items the monster may attempt after its planned actions. */
final class MonsterItemPolicy {
    private MonsterItemPolicy() { }

    static List<GameAction.ItemUse> candidates(ParticipantState self, ParticipantState player) {
        List<GameAction.ItemUse> choices = new ArrayList<GameAction.ItemUse>();
        for (String item : self.getItems()) {
            if (shouldSkip(item, self, player)) continue;
            boolean againstPlayer = ItemIds.GARBAGE_BOMB.equals(item) || ItemIds.NULLIFY.equals(item);
            choices.add(new GameAction.ItemUse(item, againstPlayer ? player.getId() : self.getId()));
        }
        return choices;
    }

    private static boolean shouldSkip(String item, ParticipantState self, ParticipantState player) {
        switch (item) {
            case ItemIds.HEAL:
                return self.getHp() == self.getMaxHp();
            case ItemIds.FEVER_CHARGE:
                return self.isFeverActive();
            case ItemIds.TIME_WARP:
                return self.getTimeWarpRemainingMillis() > 0;
            case ItemIds.NULLIFY:
                return player.getItems().isEmpty();
            case ItemIds.DAMAGE_BOOST:
            case ItemIds.SHIELD:
                return true;
            default:
                return false;
        }
    }
}

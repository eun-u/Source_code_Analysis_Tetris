package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.jbnu.se.tetris.battle.BattleEvent;
import kr.ac.jbnu.se.tetris.core.GameEvent;
import kr.ac.jbnu.se.tetris.item.ItemIds;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.BattleLog;

/**
 * 한 번의 엔진 갱신에서 받은 전투 이벤트를 짝지어 전투 로그 문구로 만든다.
 * 줄 삭제 이벤트는 같은 코어 ID로 묶고, 단독 피해·가비지와 아이템 결과는 이벤트 순서대로 연결한다.
 */
final class BattleNoticeBuilder {
    private static final String LOCAL_LABEL = "나";

    interface BattleNotice {
        BattleLog.Side side();
        String text();
    }

    static final class ClearNotice implements BattleNotice {
        final BattleLog.Side side;
        final String subject;
        final int lines, combo;
        final boolean perfect;
        Integer damage, garbage;

        ClearNotice(BattleLog.Side side, String subject, GameEvent clear) {
            this.side = side;
            this.subject = subject;
            this.lines = clear.getLineCount();
            this.combo = clear.getCombo();
            this.perfect = clear.isPerfectClear();
        }

        @Override public BattleLog.Side side() { return side; }

        @Override public String text() {
            StringBuilder notice = new StringBuilder(subject).append(" · ").append(lines).append("줄 클리어");
            if (perfect) notice.append(" 퍼펙트!");
            if (combo > 0) notice.append(" · ").append(combo).append("콤보");
            if (damage != null || garbage != null) {
                notice.append(" → ");
                if (damage != null) notice.append(damage).append(" 피해");
                if (garbage != null) {
                    if (damage != null) notice.append(" · ");
                    notice.append("가비지 ").append(garbage).append("줄");
                }
            }
            return notice.toString();
        }
    }

    /** Damage and items without a LINE_CLEAR source are still legitimate battle facts. */
    static final class ActionNotice implements BattleNotice {
        final BattleLog.Side side;
        final String subject, actor, target;
        String action;
        Integer damage, garbage;
        String removedItem, removedFrom;

        ActionNotice(BattleLog.Side side, String subject, String actor, String target, String action) {
            this.side = side;
            this.subject = subject;
            this.actor = actor;
            this.target = target;
            this.action = action;
        }

        @Override public BattleLog.Side side() { return side; }

        boolean sameParticipants(BattleEvent event) {
            return java.util.Objects.equals(actor, event.getActorId())
                    && java.util.Objects.equals(target, event.getTargetId());
        }

        @Override public String text() {
            StringBuilder value = new StringBuilder(subject).append(" · ").append(action);
            if (removedItem != null) {
                value.append(" → ").append(removedFrom).append(' ').append(ItemLabels.name(removedItem)).append(" 제거");
            } else if (damage != null || garbage != null) {
                value.append(" → ");
                if (damage != null) value.append("피해 ").append(damage);
                if (garbage != null) {
                    if (damage != null) value.append(" + ");
                    value.append("가비지 ").append(garbage).append("줄");
                }
            }
            return value.toString();
        }
    }

    private final List<BattleNotice> output = new ArrayList<BattleNotice>();
    private final String enemyLabel;
    private final Map<String, ClearNotice> clearByCore = new LinkedHashMap<String, ClearNotice>();
    private ActionNotice attack;
    private ActionNotice item;

    BattleNoticeBuilder(String enemyLabel) {
        this.enemyLabel = enemyLabel;
    }

    List<BattleNotice> notices() { return output; }

    void add(BattleEvent event, String localId) {
        boolean local = localId != null && localId.equals(event.getActorId());
        BattleLog.Side side = local ? BattleLog.Side.PLAYER : BattleLog.Side.ENEMY;
        String subject = local ? LOCAL_LABEL : enemyLabel;
        BattleEvent.Type type = event.getType();
        GameEvent core = event.getCoreEvent();
        if (core != null && core.getType() == GameEvent.Type.LINE_CLEAR) {
            addClear(event, core, side, subject);
        } else if (type == BattleEvent.Type.DAMAGE) {
            addDamage(event, side, subject);
        } else if (type == BattleEvent.Type.GARBAGE_SENT) {
            addGarbageSent(event, side, subject);
        } else if (type == BattleEvent.Type.ITEM_USED) {
            addItemUsed(event, side, subject);
        } else if (type == BattleEvent.Type.ITEM_REMOVED) {
            addItemRemoved(event, local, side, subject);
        } else if (type == BattleEvent.Type.ITEM_ACQUIRED) {
            addItemAcquired(event, local, side, subject);
        } else if (type == BattleEvent.Type.CORE_EVENT) {
            // Placement starts a separate engine action; never group its attacks with the last one.
            resetPending();
        }
    }

    private void resetPending() {
        attack = null;
        item = null;
    }

    private void addClear(BattleEvent event, GameEvent core, BattleLog.Side side, String subject) {
        String actor = event.getActorId();
        String coreActor = actor == null ? core.getActorId() : actor;
        String key = coreActor + ":" + core.getEventId();
        ClearNotice notice = clearByCore.get(key);
        if (notice == null) {
            notice = new ClearNotice(side, subject, core);
            clearByCore.put(key, notice);
            output.add(notice);
        }
        if (event.getType() == BattleEvent.Type.DAMAGE) notice.damage = event.getAmount();
        if (event.getType() == BattleEvent.Type.GARBAGE_SENT) notice.garbage = event.getAmount();
    }

    private void startAttack(BattleEvent event, BattleLog.Side side, String subject) {
        attack = new ActionNotice(side, subject, event.getActorId(), event.getTargetId(), "공격");
        output.add(attack);
    }

    private void addDamage(BattleEvent event, BattleLog.Side side, String subject) {
        // A direct attack may have no backing Tetris line clear (synthetic/wire event).
        if (attack == null || !attack.sameParticipants(event) || attack.damage != null) {
            startAttack(event, side, subject);
        }
        attack.damage = event.getAmount();
        item = null;
    }

    private void addGarbageSent(BattleEvent event, BattleLog.Side side, String subject) {
        if (item != null && ItemLabels.GARBAGE_BOMB_TEXT.equals(item.action)
                && item.sameParticipants(event) && item.garbage == null) {
            item.garbage = event.getAmount();
            resetPending();
            return;
        }
        if (attack == null || !attack.sameParticipants(event) || attack.garbage != null) {
            startAttack(event, side, subject);
        }
        attack.garbage = event.getAmount();
    }

    private void addItemUsed(BattleEvent event, BattleLog.Side side, String subject) {
        String id = event.getReason();
        if (ItemIds.GARBAGE_BOMB.equals(id) && attack != null
                && attack.sameParticipants(event) && attack.damage == null
                && attack.garbage != null) {
            // BattleManager submits the bomb's garbage event before ITEM_USED.
            attack.action = ItemLabels.GARBAGE_BOMB_TEXT;
            resetPending();
            return;
        }
        String action = ItemLabels.action(id);
        item = new ActionNotice(side, subject, event.getActorId(), event.getTargetId(), action);
        output.add(item);
        attack = null;
        if (!ItemLabels.GARBAGE_BOMB_TEXT.equals(action) && !"무효화".equals(action)) item = null;
    }

    private void addItemRemoved(BattleEvent event, boolean local, BattleLog.Side side, String subject) {
        if (item != null && "무효화".equals(item.action) && item.sameParticipants(event)) {
            item.removedItem = event.getReason();
            item.removedFrom = local ? "상대" : "내";
        } else {
            output.add(new ActionNotice(side, subject, event.getActorId(), event.getTargetId(),
                    ItemLabels.name(event.getReason()) + " 제거"));
        }
        resetPending();
    }

    private void addItemAcquired(BattleEvent event, boolean local, BattleLog.Side side, String subject) {
        // Player pickups already have an ore-flight / inventory effect.
        if (!local) output.add(new ActionNotice(side, subject, event.getActorId(), event.getTargetId(),
                "아이템 획득 · " + ItemLabels.name(event.getReason())));
        resetPending();
    }
}

package kr.ac.jbnu.se.tetris.app.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.ai.AIPlan;
import kr.ac.jbnu.se.tetris.ai.AIStrategy;
import kr.ac.jbnu.se.tetris.ai.PlacementLog;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.app.MonsterSession;
import kr.ac.jbnu.se.tetris.battle.BattleEvent;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;

/** A completed AI plan must publish every command's events with its final battle state. */
public final class LocalMatchSessionAiEventsTest {
    private static final long LIMIT_NANOS = 5_000_000_000L;

    public static void main(String[] args) throws Exception {
        sequentialActionsAreDeliveredOnce();
        rejectedActionKeepsPriorEvents();
        System.out.println("PASS LocalMatchSessionAiEventsTest");
    }

    private static void sequentialActionsAreDeliveredOnce() throws Exception {
        verifyPlan(Arrays.asList(GameAction.Type.MOVE_LEFT, GameAction.Type.MOVE_RIGHT,
                GameAction.Type.HARD_DROP), false);
    }

    private static void rejectedActionKeepsPriorEvents() throws Exception {
        verifyPlan(Arrays.asList(GameAction.Type.HARD_DROP, GameAction.Type.START), true);
    }

    private static void verifyPlan(List<GameAction.Type> actions, boolean expectRejection) throws Exception {
        AIStrategy strategy = new AIStrategy() {
            @Override public AIPlan plan(GameState state) {
                return new AIPlan(state.getVersion(), actions, 1, 0, 0, false);
            }
        };
        AtomicReference<MonsterSession> engine = new AtomicReference<MonsterSession>();
        AtomicReference<LocalMatchSession> session = new AtomicReference<LocalMatchSession>();
        List<SessionUpdate> updates = new ArrayList<SessionUpdate>();
        SwingUtilities.invokeAndWait(() -> {
            engine.set(new MonsterSession(7, "몬스터", 100, 1000, strategy,
                    new PlayerProfile(), new PlacementLog()));
            session.set(new LocalMatchSession(engine.get()));
            session.get().subscribe(updates::add);
            session.get().advanceAi();
        });
        try {
            long deadline = System.nanoTime() + LIMIT_NANOS;
            while (engine.get().getLastPlan() == null && System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait(() -> session.get().advanceAi());
                Thread.sleep(2);
            }
            check(engine.get().getLastPlan() != null, "AI plan completed");
            List<SessionUpdate> eventUpdates = new ArrayList<SessionUpdate>();
            for (SessionUpdate update : updates) {
                if (!update.getEvents().isEmpty()) eventUpdates.add(update);
            }
            check(eventUpdates.size() == 1, "One update publishes the completed plan");
            SessionUpdate applied = eventUpdates.get(0);
            List<BattleEvent> events = applied.getEvents();
            check(events.size() > engine.get().getLastBattleResult().getEvents().size(),
                    "Events from commands before the last result are retained");
            long previous = -1;
            for (BattleEvent event : events) {
                check(event.getEventId() > previous, "Events retain battle order");
                previous = event.getEventId();
            }
            boolean rejected = false;
            for (BattleEvent event : events) {
                rejected |= event.getType() == BattleEvent.Type.ACTION_REJECTED;
            }
            check(rejected == expectRejection, "Rejected command is reported when expected");
            check(applied.getSnapshot().getBattleState().getVersion()
                    == engine.get().getLastBattleResult().getState().getVersion(),
                    "Final snapshot matches the last command result");
            SwingUtilities.invokeAndWait(() -> session.get().advanceAi());
            int after = updates.size();
            SwingUtilities.invokeAndWait(() -> session.get().advanceAi());
            check(updates.size() == after, "Idle pulses do not redeliver events");
        } finally {
            SwingUtilities.invokeAndWait(() -> session.get().close());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

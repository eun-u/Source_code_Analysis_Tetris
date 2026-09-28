package kr.ac.jbnu.se.tetris.app;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.util.Random;
import kr.ac.jbnu.se.tetris.ai.AIPlan;
import kr.ac.jbnu.se.tetris.ai.HeuristicStrategy;
import kr.ac.jbnu.se.tetris.ai.HeuristicWeights;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;

/** 실제 엔진의 승리·재도전·15전투 전환 확인 및 상대 AI 성능 검증과의 분리 */
public final class StoryFlowTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisApplication app = new TetrisApplication(new Random(20260928L));
            try {
                app.startStory(0, 1);
                String first = app.getBattleState().getParticipant("monster").getName();
                for (int i = 0; i < 100 && !finished(app); i++) app.submit(GameAction.Type.HARD_DROP);
                check(finished(app), "첫 상대 패배 결과 도달");
                app.showHome(); app.continueGame();
                check("result".equals(app.getRouter().getCurrentId()), "패배 후 홈에서 같은 스토리 결과 복귀");
                button(app.getRouter().getContainer(), "retry").doClick();
                check(first.equals(app.getBattleState().getParticipant("monster").getName()), "패배 재도전은 같은 상대");
                for (int encounter = 0; encounter < 15; encounter++) {
                    String name = app.getBattleState().getParticipant("monster").getName();
                    winUsingRealEngine(app);
                    app.showHome();
                    check(button(app.getRouter().getContainer(), "continueGame").isEnabled(), "승리 후 홈에서 결과 복귀 가능");
                    app.continueGame();
                    check("result".equals(app.getRouter().getCurrentId()), "승리 결과와 진행 상태 보존");
                    if (encounter == 0) {
                        button(app.getRouter().getContainer(), "retry").doClick();
                        check(name.equals(app.getBattleState().getParticipant("monster").getName()), "승리 후 다시 하기도 같은 상대");
                        winUsingRealEngine(app);
                    }
                    app.nextEncounter();
                    if (encounter < 14) {
                        check("battle".equals(app.getRouter().getCurrentId()), "다음 상대 전투 화면");
                        check(!name.equals(app.getBattleState().getParticipant("monster").getName()), "다음 상대 이름 변경");
                    }
                }
                check("stages".equals(app.getRouter().getCurrentId()), "최종 보스 뒤 단계 선택 복귀");
                check(!app.isGravityRunning() && !app.isAiTimerRunning(), "캠페인 종료 후 시계 정지");
                app.nextEncounter();
                check("stages".equals(app.getRouter().getCurrentId()), "중복 다음 입력 무시");
            } finally { app.close(); }
        });
        System.out.println("PASS StoryFlowTest: 15 encounters, loss retry, win replay, final return");
    }
    private static boolean finished(TetrisApplication app) {
        return app.getBattleState().getStatus() == BattleState.Status.FINISHED;
    }
    private static void winUsingRealEngine(TetrisApplication app) {
        HeuristicStrategy helper = new HeuristicStrategy(HeuristicWeights.SAFE,
                1800, Long.MAX_VALUE);
        // EDT 직접 진행을 통한 비동기 타이머 배제 및 난이도 측정 근거에서 제외
        for (int i = 0; i < 500 && !finished(app); i++) {
            if (app.getState().getActivePiece() == null) app.advanceGravity();
            if (finished(app)) break;
            AIPlan plan = helper.plan(app.getState());
            for (GameAction.Type action : plan.getActions()) app.submit(action);
            app.advanceGravity();
        }
        check(finished(app) && "local".equals(app.getBattleState().getWinnerId()), "실제 전투 승리");
        check("result".equals(app.getRouter().getCurrentId()), "승리 결과 화면");
    }
    private static JButton button(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton && name.equals(child.getName())) return (JButton) child;
            if (child instanceof Container) {
                JButton found = button((Container) child, name);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}

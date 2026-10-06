package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 ResultPanel의 제목·다섯 통계 줄·두 버튼 구성 */
public final class ResultPanel extends JPanel implements Screen {
    private final JLabel result = new JLabel("RESULT", JLabel.CENTER);
    private final JLabel player = new JLabel("Player");
    private final JLabel line = new JLabel("Line : —");
    private final JLabel combo = new JLabel("Max Combo : —");
    private final JLabel damage = new JLabel("Damage : —");
    private final JLabel reward = new JLabel("Reward : —");
    private final JButton returnButton = new GameButton("돌아가기");

    public ResultPanel(Runnable again, Runnable home) {
        this(again, home, () -> { });
    }

    /** 세 번째 인수는 구 호출자 호환용; 원본 결과 화면에는 두 버튼만 존재 */
    public ResultPanel(Runnable again, Runnable home, Runnable proceed) {
        super(new BorderLayout());
        result.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 28));
        add(result, BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        for (JLabel label : new JLabel[] {player, line, combo, damage, reward}) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        info.add(Box.createVerticalGlue());
        info.add(player);
        info.add(Box.createVerticalStrut(20));
        info.add(line);
        info.add(Box.createVerticalStrut(10));
        info.add(combo);
        info.add(Box.createVerticalStrut(10));
        info.add(damage);
        info.add(Box.createVerticalStrut(10));
        info.add(reward);
        info.add(Box.createVerticalGlue());
        add(info, BorderLayout.CENTER);

        returnButton.setName("retry");
        returnButton.addActionListener(event -> again.run());
        JButton lobby = new GameButton("로비로");
        lobby.setName("resultHome");
        lobby.addActionListener(event -> home.run());
        JPanel buttons = new JPanel();
        buttons.add(returnButton);
        buttons.add(lobby);
        add(buttons, BorderLayout.SOUTH);
    }

    private void setStatistics(String heading, String playerName, Integer cleared) {
        result.setText(heading);
        result.setToolTipText(null);
        player.setText(playerName);
        line.setText("Line : " + (cleared == null ? "—" : cleared));
        combo.setText("Max Combo : —");
        damage.setText("Damage : —");
        reward.setText("Reward : —");
    }

    public void setResult(GameState state) {
        setStatistics("GAME OVER", "Player", state.getLinesCleared());
        setReturnText("돌아가기");
    }

    public void setLocalResult(GameState state, boolean completed) {
        setStatistics(completed ? "COMPLETE" : "GAME OVER", "Player", state.getLinesCleared());
        setReturnText("Local Mode로");
    }

    public void setBattleResult(BattleState state, String playerId) {
        String reason = state.getReason() == null ? "" : state.getReason();
        String heading = reason.contains("FAILED") ? "경기 중단" : state.getWinnerId() == null ? "무승부"
                : playerId.equals(state.getWinnerId()) ? "승리" : "패배";
        ParticipantState local = state.getParticipant(playerId);
        setStatistics(heading, local.getName(), local.getGameState().getLinesCleared());
        result.setToolTipText(reason);
        setReturnText("돌아가기");
    }

    public void setTutorialResult(boolean completed, int step) {
        setStatistics(completed ? "튜토리얼 완료" : "튜토리얼 다시 도전",
                "완료한 목표: " + step + " / 5", null);
        setReturnText("Local Mode로");
    }

    public void setFailure(String message) {
        setStatistics("경기 중단", message == null ? "세션 실행 중 오류가 발생했습니다." : message, null);
        result.setToolTipText(message);
        setReturnText("돌아가기");
    }

    public void setStoryContinuation(boolean won, boolean lastEncounter) {
        setReturnText("Story로");
    }

    public void setOnlineRetry(boolean requested, boolean failed) {
        setReturnText("대기방으로");
        returnButton.setEnabled(true);
    }

    public void setReturnText(String text) { returnButton.setText(text); }
    @Override public String getId() { return "result"; }
    @Override public JPanel getPanel() { return this; }
}

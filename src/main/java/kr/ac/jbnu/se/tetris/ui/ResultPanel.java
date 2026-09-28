package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.battle.BattleState;

/** 세션이 확정한 결과·다음 행동 표시 및 승패·단계 규칙 계산 제외 */
public final class ResultPanel extends JPanel implements Screen {
    private final JLabel result = new JLabel("", JLabel.CENTER);
    private final JLabel title = new JLabel("GAME OVER", JLabel.CENTER);
    private final JButton next = new JButton("다음 상대");
    private final JButton retry = new JButton("다시 하기");

    public ResultPanel(Runnable again, Runnable home) {
        this(again, home, () -> { });
    }
    public ResultPanel(Runnable again, Runnable home, Runnable proceed) {
        super(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(64, 32, 64, 32));
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);
        add(result, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout());
        retry.setName("retry");
        retry.addActionListener(event -> again.run());
        JButton back = new JButton("홈으로");
        back.setName("resultHome");
        back.addActionListener(event -> home.run());
        buttons.add(retry);
        next.setName("nextEncounter");
        next.addActionListener(event -> proceed.run());
        next.setVisible(false);
        buttons.add(next);
        buttons.add(back);
        add(buttons, BorderLayout.SOUTH);
    }
    public void setResult(GameState state) {
        resetRetry();
        next.setVisible(false);
        title.setText("GAME OVER");
        result.setText("<html><div style='text-align:center'>블록이 보드 상단에 도달했습니다.<br><br>지운 줄: "
                + state.getLinesCleared() + "</div></html>");
    }
    public void setBattleResult(BattleState state, String playerId) {
        resetRetry();
        next.setVisible(false);
        String reason = state.getReason() == null ? "" : state.getReason();
        boolean failed = reason.contains("FAILED");
        title.setText(failed ? "경기 중단" : state.getWinnerId() == null ? "무승부"
                : playerId.equals(state.getWinnerId()) ? "승리" : "패배");
        String explanation = failed ? "경기 처리 중 오류가 발생했습니다."
                : reason.contains("HP") ? "HP가 모두 소진되었습니다."
                : reason.contains("TOP_OUT") ? "보드가 상단에 도달했습니다."
                : reason.contains("FORFEIT") ? "참가자가 대전에서 나갔습니다." : "경기가 종료되었습니다.";
        result.setText("<html><div style='text-align:center'>"
                + explanation
                + "<br><br>내 HP: " + state.getParticipant(playerId).getHp()
                + "<br>지운 줄: " + state.getParticipant(playerId).getGameState().getLinesCleared()
                + "</div></html>");
    }
    public void setTutorialResult(boolean completed, int step) {
        resetRetry();
        next.setVisible(false);
        title.setText(completed ? "튜토리얼 완료" : "튜토리얼 다시 도전");
        result.setText(completed ? "기본 조작 5개를 익혔습니다. 홈에서 스토리에 도전하세요."
                : "보드가 가득 찼습니다. 완료한 목표 " + step + "/5 · 다시 시도해 보세요.");
    }
    public void setFailure(String message) {
        resetRetry();
        next.setVisible(false); title.setText("경기 중단");
        result.setText(message == null ? "세션 실행 중 오류가 발생했습니다." : message);
    }
    public void setStoryContinuation(boolean won, boolean lastEncounter) {
        next.setVisible(won);
        next.setText(lastEncounter ? "단계 선택으로" : "다음 상대");
        if (won && lastEncounter) title.setText("대학 스토리 완료");
    }
    public void setOnlineRetry(boolean requested, boolean failed) {
        retry.setText(failed ? "다시 접속" : requested ? "상대 준비 대기" : "재대결 준비");
        retry.setEnabled(!requested || failed);
    }
    private void resetRetry() { retry.setText("다시 하기"); retry.setEnabled(true); }
    @Override public String getId() { return "result"; }
    @Override public JPanel getPanel() { return this; }
}

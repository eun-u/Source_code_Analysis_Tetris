package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 ResultPanel의 중앙 결과 카드와 실제 세션 결과 연결 */
public final class ResultPanel extends JPanel implements Screen {
    private final JLabel title = new JLabel("GAME OVER", JLabel.CENTER);
    private final JLabel summary = centeredLabel("");
    private final JLabel player = centeredLabel("");
    private final JLabel lines = centeredLabel("");
    private final JLabel hp = centeredLabel("");
    private final JButton next = new GameButton("다음 상대");
    private final JButton retry = new GameButton("다시 하기");

    public ResultPanel(Runnable again, Runnable home) {
        this(again, home, () -> { });
    }

    public ResultPanel(Runnable again, Runnable home, Runnable proceed) {
        super(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(40, 30, 32, 30));
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);

        JPanel resultInfo = new JPanel();
        resultInfo.setLayout(new BoxLayout(resultInfo, BoxLayout.Y_AXIS));
        resultInfo.setBorder(BorderFactory.createEtchedBorder());
        resultInfo.add(Box.createVerticalGlue());
        for (JLabel label : new JLabel[] {summary, player, lines, hp}) {
            resultInfo.add(label);
            resultInfo.add(Box.createVerticalStrut(12));
        }
        resultInfo.add(Box.createVerticalGlue());
        add(resultInfo, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        retry.setName("retry");
        retry.addActionListener(event -> again.run());
        buttons.add(retry);
        next.setName("nextEncounter");
        next.addActionListener(event -> proceed.run());
        next.setVisible(false);
        buttons.add(next);
        JButton back = new GameButton("홈으로");
        back.setName("resultHome");
        back.addActionListener(event -> home.run());
        buttons.add(back);
        add(buttons, BorderLayout.SOUTH);
    }

    private static JLabel centeredLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return label;
    }

    public void setResult(GameState state) {
        resetRetry();
        next.setVisible(false);
        title.setText("GAME OVER");
        summary.setText("블록이 보드 상단에 도달했습니다.");
        player.setText("");
        lines.setText("지운 줄: " + state.getLinesCleared());
        hp.setText("");
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
        ParticipantState local = state.getParticipant(playerId);
        summary.setText(explanation);
        player.setText(local.getName());
        lines.setText("지운 줄: " + local.getGameState().getLinesCleared());
        hp.setText("내 HP: " + local.getHp() + " / " + local.getMaxHp());
    }

    public void setTutorialResult(boolean completed, int step) {
        resetRetry();
        next.setVisible(false);
        title.setText(completed ? "튜토리얼 완료" : "튜토리얼 다시 도전");
        summary.setText(completed ? "기본 조작 5개를 익혔습니다." : "보드가 가득 찼습니다.");
        player.setText(completed ? "홈에서 스토리에 도전하세요." : "다시 시도해 보세요.");
        lines.setText(completed ? "" : "완료한 목표: " + step + " / 5");
        hp.setText("");
    }

    public void setFailure(String message) {
        resetRetry();
        next.setVisible(false);
        title.setText("경기 중단");
        summary.setText(message == null ? "세션 실행 중 오류가 발생했습니다." : message);
        player.setText("");
        lines.setText("");
        hp.setText("");
    }

    public void setStoryContinuation(boolean won, boolean lastEncounter) {
        next.setVisible(won);
        next.setText(lastEncounter ? "단계 선택으로" : "다음 상대");
        next.setPreferredSize(new Dimension(lastEncounter ? 140 : 120, 35));
        if (won && lastEncounter) title.setText("대학 스토리 완료");
    }

    public void setOnlineRetry(boolean requested, boolean failed) {
        retry.setText(failed ? "다시 접속" : requested ? "상대 준비 대기" : "재대결 준비");
        retry.setPreferredSize(new Dimension(requested ? 140 : 120, 35));
        retry.setEnabled(!requested || failed);
    }

    private void resetRetry() {
        retry.setText("다시 하기");
        retry.setPreferredSize(new Dimension(120, 35));
        retry.setEnabled(true);
    }

    @Override public String getId() { return "result"; }
    @Override public JPanel getPanel() { return this; }
}

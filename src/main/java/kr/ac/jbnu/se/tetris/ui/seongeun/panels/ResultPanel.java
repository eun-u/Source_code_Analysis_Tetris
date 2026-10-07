package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class ResultPanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {
    private final JLabel resultLabel = new JLabel("RESULT", SwingConstants.CENTER);
    private final JLabel playerNameLabel = new JLabel("Player", SwingConstants.CENTER);
    private final JLabel lineLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel comboLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel damageLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel rewardLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel rankedStatus = new JLabel("", SwingConstants.CENTER);
    private final JLabel milestone = new JLabel("", SwingConstants.CENTER);
    private final GameButton returnButton = new GameButton("돌아가기");
    private final GameButton lobbyButton = new GameButton("로비로");
    private final GameButton nextButton = new GameButton("다음 전투");
    private final GameButton retryButton = new GameButton("다시 도전");
    private final Banner banner = new Banner();
    private final Timer countTimer = new Timer(30, event -> refreshCount());
    private String[] targets = { "0", "0", "0", "0" };
    private long countStartedAt;
    private boolean victory;

    public ResultPanel() {
        setLayout(new GridBagLayout());
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(new EmptyBorder(16, 20, 16, 20));
        banner.setAlignmentX(Component.CENTER_ALIGNMENT);
        banner.setMaximumSize(new Dimension(620, 90));
        banner.setPreferredSize(new Dimension(620, 90));
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultLabel.setFont(UniversityPixelTheme.font(44, Font.BOLD));
        resultLabel.setForeground(UniversityPixelTheme.TEXT);
        banner.add(Box.createVerticalGlue()); banner.add(resultLabel); banner.add(Box.createVerticalGlue());
        content.add(banner); content.add(Box.createVerticalStrut(14));

        JPanel resultCard = new JPanel(new BorderLayout(0, 12));
        resultCard.setBackground(UniversityPixelTheme.PANEL);
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3),
                new EmptyBorder(14, 18, 14, 18)));
        resultCard.setPreferredSize(new Dimension(620, 200));
        resultCard.setMaximumSize(new Dimension(620, 200));
        resultCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerNameLabel.setForeground(UniversityPixelTheme.GOLD);
        playerNameLabel.setFont(UniversityPixelTheme.font(17, Font.BOLD));
        resultCard.add(playerNameLabel, BorderLayout.NORTH);
        JPanel stats = new JPanel(new GridLayout(1, 4, 8, 0)); stats.setOpaque(false);
        stats.add(stat("지운 줄", lineLabel)); stats.add(stat("최대 콤보", comboLabel));
        stats.add(stat("입힌 피해", damageLabel)); stats.add(stat("획득 코인", rewardLabel));
        resultCard.add(stats, BorderLayout.CENTER);
        JPanel notes = new JPanel(); notes.setOpaque(false);
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        for (JLabel label : new JLabel[] { milestone, rankedStatus }) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setForeground(UniversityPixelTheme.GOLD);
            label.setFont(UniversityPixelTheme.font(14, Font.BOLD));
            notes.add(label);
        }
        resultCard.add(notes, BorderLayout.SOUTH);
        content.add(resultCard); content.add(Box.createVerticalStrut(18));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        for (GameButton button : new GameButton[] { lobbyButton, returnButton, retryButton, nextButton })
            button.setPreferredSize(new Dimension(132, 42));
        lobbyButton.secondary();
        nextButton.setVisible(false); retryButton.setVisible(false);
        buttonPanel.add(lobbyButton); buttonPanel.add(returnButton);
        buttonPanel.add(retryButton); buttonPanel.add(nextButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(buttonPanel);
        add(content);
        applyButtonRoles();
    }

    private JPanel stat(String name, JLabel value) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        cell.setBorder(new EmptyBorder(10, 8, 10, 8));
        JLabel caption = new JLabel(name, SwingConstants.CENTER);
        caption.setForeground(UniversityPixelTheme.TEXT_SUB);
        caption.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        value.setForeground(UniversityPixelTheme.TEXT);
        value.setFont(UniversityPixelTheme.font(26, Font.BOLD));
        cell.add(caption, BorderLayout.NORTH); cell.add(value, BorderLayout.CENTER);
        return cell;
    }

    public void setResult(String result, String playerName, int line, int maxCombo, int damage, int reward) {
        setResultDetails(result, playerName, String.valueOf(line), String.valueOf(maxCombo),
                String.valueOf(damage), String.valueOf(reward));
    }

    /** 미집계 항목도 원본 정보 자리에서 그대로 표시할 수 있게 한다. */
    public void setResultDetails(String result, String playerName, String line,
                                 String maxCombo, String damage, String reward) {
        resultLabel.setText(result);
        boolean good = "VICTORY".equals(result) || "COMPLETE".equals(result);
        boolean bad = "DEFEAT".equals(result) || "GAME OVER".equals(result) || "CONNECTION LOST".equals(result);
        Color accent = good ? UniversityPixelTheme.MINT : bad ? UniversityPixelTheme.CORAL : UniversityPixelTheme.GOLD;
        resultLabel.setForeground(accent);
        banner.setAccent(accent);
        playerNameLabel.setText(playerName);
        targets = new String[] { line, maxCombo, damage, reward };
        countStartedAt = System.currentTimeMillis();
        victory = good;
        refreshCount();
        countTimer.restart();
        banner.flashAt = countStartedAt;
    }

    /** 숫자 결과는 0에서 목표값까지 0.8초 동안 올라간다. 숫자가 아닌 값('—')은 그대로 보인다. */
    private void refreshCount() {
        JLabel[] values = { lineLabel, comboLabel, damageLabel, rewardLabel };
        float t = Math.min(1f, (System.currentTimeMillis() - countStartedAt) / 800f);
        float ease = 1 - (1 - t) * (1 - t) * (1 - t);
        for (int i = 0; i < values.length; i++) {
            String target = targets[i] == null ? "" : targets[i];
            try {
                int number = Integer.parseInt(target);
                values[i].setText(Integer.toString(Math.round(number * ease)));
            } catch (NumberFormatException notNumber) { values[i].setText(target); }
        }
        if (t >= 1f) countTimer.stop();
    }

    /** 승리하면 배경 위로 색종이가 떨어진다. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!victory) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            long age = System.currentTimeMillis() - countStartedAt;
            Color[] colors = { UniversityPixelTheme.GOLD, UniversityPixelTheme.MINT, UniversityPixelTheme.CORAL,
                    new Color(104, 221, 235), new Color(201, 139, 255) };
            int w = getWidth(), h = getHeight();
            for (int i = 0; i < 70; i++) {
                long cycle = 2600 + (i % 7) * 300;
                float t = ((age + i * 173) % cycle) / (float) cycle;
                int x = Math.floorMod(i * 97 + 31, Math.max(1, w)) + (int) (Math.sin(t * 9 + i) * 18);
                int y = Math.round(-20 + (h + 40) * t);
                g.setColor(colors[i % colors.length]);
                int size = 4 + i % 4;
                g.fillRect(x, y, (i % 2 == 0) ? size : size / 2 + 1, (i % 2 == 0) ? size / 2 + 1 : size);
            }
        } finally { g.dispose(); }
    }

    public void setReturnButtonText(String text) { returnButton.setText(text); }
    public void setReturnAction(ActionListener listener) { returnButton.addActionListener(listener); }
    public void setLobbyAction(ActionListener listener) { lobbyButton.addActionListener(listener); }
    public void setRankedStatus(String text) { rankedStatus.setText(text); }
    public void setStoryActions(boolean story, boolean hasNext, String earnedBadge) {
        nextButton.setVisible(story && hasNext); retryButton.setVisible(story);
        milestone.setText(earnedBadge == null || earnedBadge.isEmpty() ? "" : "획득  ·  " + earnedBadge);
        applyButtonRoles();
    }
    public void setNextAction(ActionListener action) { nextButton.addActionListener(action); }
    public void setRetryAction(ActionListener action) { retryButton.addActionListener(action); }

    /** 가장 자연스러운 다음 행동 하나만 금색으로 강조한다. */
    private void applyButtonRoles() {
        returnButton.secondary(); retryButton.secondary(); nextButton.primary();
        if (!nextButton.isVisible()) {
            if (retryButton.isVisible()) retryButton.primary(); else returnButton.primary();
        }
    }

    /** 결과 색을 띠와 테두리에 써서 승패를 한눈에 구분한다. */
    private static final class Banner extends JPanel {
        private Color accent = UniversityPixelTheme.GOLD;
        long flashAt;

        Banner() { setOpaque(false); }

        void setAccent(Color accent) { this.accent = accent; repaint(); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int width = getWidth(), height = getHeight();
                g.setColor(UniversityPixelTheme.PANEL);
                g.fillRect(0, 0, width, height);
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 38));
                g.fillRect(0, 0, width, height);
                g.setColor(accent);
                g.fillRect(0, 0, width, 5);
                g.fillRect(0, height - 5, width, 5);
                // 결과가 바뀐 직후 띠 위를 밝은 빛이 한 번 훑고 지나간다.
                long age = System.currentTimeMillis() - flashAt;
                if (age >= 0 && age < 700) {
                    int x = Math.round((width + 160) * age / 700f) - 80;
                    g.setColor(new Color(255, 255, 255, 70));
                    g.fillPolygon(new int[] { x, x + 50, x + 20, x - 30 }, new int[] { 0, 0, height, height }, 4);
                }
            } finally { g.dispose(); }
        }
    }
}

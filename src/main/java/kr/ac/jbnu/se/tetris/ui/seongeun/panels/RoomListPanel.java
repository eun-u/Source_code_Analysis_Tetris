package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.RoomRow;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

/**
 * 온라인 대전 입구. 서버가 방 목록을 주지 않으므로 방 만들기와 번호 입장을 큰 선택지로 보여 주고,
 * 목록이 있을 때만 아래에 방 줄을 표시한다.
 */
public class RoomListPanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {

    private final GameButton backButton = new GameButton("로비로");
    private final GameButton createRoomButton = new GameButton("방 만들기");
    private final GameButton joinByIdButton = new GameButton("방 번호로 입장");
    private final GameButton retryButton = new GameButton("다시 연결");
    private final JPanel roomContainer = new JPanel();
    private final JScrollPane roomScroll;
    private final JLabel connectionLabel = new JLabel("서버 연결 중...");
    private final JLabel connectionDot = new JLabel("●");

    public RoomListPanel() {
        setLayout(new BorderLayout(0, UniversityPixelTheme.HEADER_GAP));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, UniversityPixelTheme.GUTTER, 18, UniversityPixelTheme.GUTTER));

        backButton.secondary();
        backButton.setPreferredSize(new Dimension(96, 38));
        add(UniversityPixelTheme.screenHeader("ONLINE BATTLE", "1:1 대전 로비", backButton), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14)); center.setOpaque(false);
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        status.setBackground(UniversityPixelTheme.PANEL);
        status.setBorder(UniversityPixelTheme.cardBorder(UniversityPixelTheme.LINE, 2, 8));
        connectionDot.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        connectionDot.setForeground(UniversityPixelTheme.GOLD);
        connectionLabel.setForeground(UniversityPixelTheme.TEXT);
        connectionLabel.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        retryButton.setPreferredSize(new Dimension(112, 34));
        status.add(connectionDot); status.add(connectionLabel); status.add(retryButton);
        center.add(status, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, 2, 16, 0)); choices.setOpaque(false);
        choices.add(choice("HOST", "새 방 만들기", createRoomButton, UniversityPixelTheme.GOLD));
        choices.add(choice("JOIN", "번호로 입장", joinByIdButton, UniversityPixelTheme.MINT));
        JPanel choiceArea = new JPanel(new GridBagLayout()); choiceArea.setOpaque(false);
        choices.setPreferredSize(new Dimension(640, 220));
        choiceArea.add(choices);
        center.add(choiceArea, BorderLayout.CENTER);

        roomContainer.setBackground(UniversityPixelTheme.BG);
        roomContainer.setLayout(new BoxLayout(roomContainer, BoxLayout.Y_AXIS));
        roomScroll = new JScrollPane(roomContainer);
        roomScroll.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2));
        roomScroll.getViewport().setBackground(UniversityPixelTheme.BG);
        roomScroll.getVerticalScrollBar().setUnitIncrement(16);
        roomScroll.setPreferredSize(new Dimension(10, 180));
        roomScroll.setVisible(false);
        center.add(roomScroll, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        setConnected(false);
    }

    private static JPanel choice(String eyebrow, String title, GameButton action, Color accent) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UniversityPixelTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 3), new EmptyBorder(22, 18, 20, 18)));
        JLabel tag = new JLabel(eyebrow, SwingConstants.CENTER);
        tag.setForeground(accent); tag.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setForeground(UniversityPixelTheme.TEXT); heading.setFont(UniversityPixelTheme.font(22, Font.BOLD));
        action.setAccent(accent);
        action.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        action.setMaximumSize(new Dimension(200, 44));
        action.setPreferredSize(new Dimension(200, 44));
        for (JComponent item : new JComponent[] { tag, heading, action })
            item.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(tag); card.add(Box.createVerticalStrut(6)); card.add(heading);
        card.add(Box.createVerticalGlue()); card.add(action);
        return card;
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void setCreateRoomAction(ActionListener listener) {
        createRoomButton.addActionListener(listener);
    }
    public void setJoinByIdAction(ActionListener listener) { joinByIdButton.addActionListener(listener); }
    public void setRetryAction(ActionListener listener) { retryButton.addActionListener(listener); }
    public void setRetryLabel(String label) { retryButton.setText(label); }
    public void setConnected(boolean connected) {
        connectionDot.setForeground(connected ? UniversityPixelTheme.MINT : UniversityPixelTheme.GOLD);
        connectionLabel.setText(connected ? "서버 연결됨" : "서버 연결 중...");
        retryButton.setVisible(false);
        retryButton.setText("다시 연결");
        createRoomButton.setEnabled(connected);
        joinByIdButton.setEnabled(connected);
    }

    /** 연결 시도가 끝났지만 실패한 상태. "연결 중"과 구분해 다음 행동을 알려 준다. */
    public void setConnectionFailed(String reason) {
        setConnected(false);
        connectionDot.setForeground(UniversityPixelTheme.CORAL);
        connectionLabel.setText("서버 연결 실패" + (reason == null ? "" : " · " + reason));
        retryButton.setVisible(true);
    }

    public void setRooms(List<RoomData> rooms, Consumer<RoomData> joinAction) {
        roomContainer.removeAll();
        roomScroll.setVisible(!rooms.isEmpty());

        for (RoomData room : rooms) {
            RoomRow row = new RoomRow(room);
            row.setBackground(UniversityPixelTheme.PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, UniversityPixelTheme.LINE),
                    new EmptyBorder(8, 12, 8, 12)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            for (Component child : row.getComponents()) {
                if (child instanceof JLabel) {
                    child.setForeground(UniversityPixelTheme.TEXT);
                    child.setFont(UniversityPixelTheme.font(14, Font.BOLD));
                }
            }
            row.setJoinAction(joinAction);
            roomContainer.add(row);
        }

        revalidate();
        repaint();
    }
}

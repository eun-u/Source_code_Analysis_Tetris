package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;

/** 방 번호를 크게 보여 주고 두 참가자의 준비 상태를 나란히 비교하는 대기실. */
public class WaitingRoomPanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {
    private final GameButton backButton = new GameButton("방 나가기");
    private final GameButton readyButton = new GameButton("준비 완료");
    private final GameButton copyButton = new GameButton("번호 복사");
    private final JLabel titleLabel;
    private final JLabel roomCode = UniversityPixelTheme.label("—", 30, Font.BOLD, UniversityPixelTheme.GOLD);
    private final JPanel playerPanel = new JPanel(new GridBagLayout());
    private String roomId;

    public WaitingRoomPanel() {
        setLayout(new BorderLayout(0, UniversityPixelTheme.HEADER_GAP));
        setBorder(new EmptyBorder(18, UniversityPixelTheme.GUTTER, 18, UniversityPixelTheme.GUTTER));
        setBackground(UniversityPixelTheme.BG);

        backButton.secondary();
        backButton.setPreferredSize(new Dimension(112, 38));
        JPanel header = UniversityPixelTheme.screenHeader("ONLINE BATTLE  ·  WAITING ROOM", "대기실", backButton);
        titleLabel = findTitle(header);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14)); center.setOpaque(false);
        center.add(codeCard(), BorderLayout.NORTH);
        playerPanel.setBackground(UniversityPixelTheme.PANEL);
        playerPanel.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3));
        center.add(playerPanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        setPlayers(null, null);

        readyButton.setName("waitingReady");
        readyButton.setFont(UniversityPixelTheme.font(16, Font.BOLD));
        readyButton.setPreferredSize(new Dimension(220, 50));
        readyButton.positive();
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.add(readyButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel codeCard() {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        card.setBorder(UniversityPixelTheme.cardBorder(UniversityPixelTheme.GOLD, 2, 12));
        JPanel text = new JPanel(); text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UniversityPixelTheme.label("방 번호", 12, Font.BOLD, UniversityPixelTheme.TEXT_SUB));
        text.add(Box.createVerticalStrut(2));
        roomCode.setToolTipText("다른 창의 '방 번호로 입장'에 이 번호를 입력하세요.");
        text.add(roomCode);
        card.add(text, BorderLayout.CENTER);
        copyButton.secondary();
        copyButton.setPreferredSize(new Dimension(110, 38));
        copyButton.setEnabled(false);
        copyButton.addActionListener(event -> copyRoomId());
        JPanel align = new JPanel(new GridBagLayout()); align.setOpaque(false);
        align.add(copyButton);
        card.add(align, BorderLayout.EAST);
        return card;
    }

    private void copyRoomId() {
        if (roomId == null) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(roomId), null);
            copyButton.setText("복사됨");
        } catch (HeadlessException | IllegalStateException unavailable) {
            copyButton.setText("복사 불가");
        }
    }

    private static JLabel findTitle(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel && "screenTitle".equals(child.getName())) return (JLabel) child;
            if (child instanceof Container) {
                JLabel found = findTitle((Container) child);
                if (found != null) return found;
            }
        }
        return new JLabel();
    }

    public void setRoom(RoomData room) {
        titleLabel.setText(room.getRoomName());
    }
    public void setRoomId(String id) {
        roomId = id == null || id.isEmpty() ? null : id;
        roomCode.setText(roomId == null ? "—" : roomId);
        copyButton.setEnabled(roomId != null);
        copyButton.setText("번호 복사");
    }
    // back 버튼 눌렀을 때 외부 결정
    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
    // ready 버튼 눌렀을 때 외부 결정
    public void setReadyAction(ActionListener listener) {
        readyButton.addActionListener(listener);
    }

    public void setPlayers(PlayerData myPlayer, PlayerData enemyPlayer) {
        playerPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.insets = new Insets(16, 16, 16, 16);
        c.gridx = 0; playerPanel.add(slot("나", myPlayer, UniversityPixelTheme.MINT), c);
        JLabel versus = UniversityPixelTheme.label("VS", 26, Font.BOLD, UniversityPixelTheme.GOLD);
        c.gridx = 1; playerPanel.add(versus, c);
        c.gridx = 2; playerPanel.add(slot("상대", enemyPlayer, UniversityPixelTheme.CORAL), c);
        boolean ready = myPlayer != null && myPlayer.isReady();
        readyButton.setText(ready ? "준비 취소" : "준비 완료");
        if (ready) readyButton.secondary(); else readyButton.positive();
        playerPanel.revalidate();
        playerPanel.repaint();
    }

    private static JPanel slot(String role, PlayerData player, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(220, 170));
        card.setBackground(player == null ? UniversityPixelTheme.PANEL : UniversityPixelTheme.PANEL_LIGHT);
        card.setBorder(UniversityPixelTheme.cardBorder(player == null ? UniversityPixelTheme.LINE : accent, 2, 14));
        JLabel roleLabel = UniversityPixelTheme.label(role, 13, Font.BOLD, player == null ? UniversityPixelTheme.TEXT_MUTED : accent);
        JLabel name = UniversityPixelTheme.label(player == null ? "입장 대기 중..." : shortName(player.getNickname()),
                18, Font.BOLD, player == null ? UniversityPixelTheme.TEXT_SUB : UniversityPixelTheme.TEXT);
        if (player != null) name.setToolTipText(player.getNickname());
        JLabel character = UniversityPixelTheme.label(player == null ? " " : player.getCharacterName(), 12,
                Font.PLAIN, UniversityPixelTheme.TEXT_SUB);
        JLabel state = UniversityPixelTheme.chip(player == null ? "WAITING" : player.isReady() ? "READY" : "NOT READY",
                player != null && player.isReady() ? UniversityPixelTheme.MINT : UniversityPixelTheme.BG);
        for (JComponent item : new JComponent[] { roleLabel, name, character, state })
            item.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(roleLabel); card.add(Box.createVerticalGlue());
        card.add(name); card.add(Box.createVerticalStrut(4)); card.add(character);
        card.add(Box.createVerticalGlue()); card.add(state);
        return card;
    }

    /** 참가자 ID가 긴 UUID여도 카드 폭 안에 들어가도록 앞부분만 보인다. */
    private static String shortName(String name) {
        if (name == null) return "";
        return name.length() > 14 ? name.substring(0, 8) + "…" : name;
    }
}

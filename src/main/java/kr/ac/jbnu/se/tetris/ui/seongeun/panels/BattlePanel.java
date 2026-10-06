package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.battle.*;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceType;
import kr.ac.jbnu.se.tetris.ui.seongeun.Board;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.MiniPiecePreview;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelArena;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;

/** 전투 무대를 보드 위에 두고 전투 상태를 실시간으로 읽는 게임 화면. */
public class BattlePanel extends JPanel {
    private static final Color BG = new Color(10, 18, 31), TEXT = new Color(234, 240, 244);
    private final PixelArena arena = new PixelArena();
    private final JLabel playerName = label("PLAYER"), enemyName = label("MONSTER");
    private final JLabel playerStatus = label("LINES 0"), enemyStatus = label("LINES 0");
    private final JLabel hold = label("HOLD"), next = label("NEXT");
    private final MiniPiecePreview holdPreview = new MiniPiecePreview("HOLD");
    private final MiniPiecePreview[] nextPreviews = {
        new MiniPiecePreview("1"), new MiniPiecePreview("2"), new MiniPiecePreview("3")
    };
    private final JLabel gauge = label("FEVER 0%"), pending = label("가비지 대기 0");
    private final JLabel warp = label(" ");
    private final JLabel status = label("← → 이동  ↑ ↓ 회전  D 빠른 낙하  SPACE 즉시 낙하  C HOLD  P 일시정지");
    private final JLabel clock = label("00:00");
    private final Board playerBoard = new Board(playerStatus, true);
    private final Board enemyBoard = new Board(enemyStatus, false);
    private final JButton[] slots = new JButton[4];
    private final JButton resultTestButton = button("전투 정보");
    private IntConsumer itemAction;
    private long feedbackUntil;
    private final JLabel title = label("MONSTER BATTLE");

    public BattlePanel() {
        setLayout(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(850, 660));
        setBackground(BG);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);
        top.add(clock, BorderLayout.EAST);
        JPanel hero = new JPanel(new BorderLayout(0, 4));
        hero.setOpaque(false);
        hero.add(top, BorderLayout.NORTH);
        hero.add(arena, BorderLayout.CENTER);
        add(hero, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 4, 0, 8);
        c.gridx = 0; c.weightx = .56;
        JPanel local = new JPanel(new BorderLayout(0, 4));
        local.setOpaque(false);
        local.add(playerName, BorderLayout.NORTH);
        local.add(playerBoard, BorderLayout.CENTER);
        local.add(playerStatus, BorderLayout.SOUTH);
        center.add(local, c);

        c.gridx = 1; c.weightx = .27; c.insets = new Insets(0, 8, 0, 8);
        JPanel hud = new JPanel();
        hud.setOpaque(false);
        hud.setLayout(new BoxLayout(hud, BoxLayout.Y_AXIS));
        for (JLabel hudLabel : new JLabel[] { hold, next, gauge, warp, pending }) {
            hudLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        warp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        holdPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel inventoryTitle = label("ITEM / 1–4");
        inventoryTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        inventoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel previews = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        previews.setOpaque(false);
        previews.setMaximumSize(new Dimension(210, 62));
        previews.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (MiniPiecePreview preview : nextPreviews) previews.add(preview);
        hud.add(hold); hud.add(holdPreview); hud.add(Box.createVerticalStrut(4));
        hud.add(next); hud.add(previews); hud.add(Box.createVerticalStrut(8));
        hud.add(gauge);
        hud.add(warp); hud.add(Box.createVerticalStrut(5));
        hud.add(pending); hud.add(Box.createVerticalStrut(8));
        hud.add(inventoryTitle); hud.add(Box.createVerticalStrut(6));
        for (int i = 0; i < slots.length; i++) {
            final int index = i;
            slots[i] = button((i + 1) + "  비어 있음");
            slots[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            slots[i].setMaximumSize(new Dimension(210, 30));
            slots[i].addActionListener(event -> { if (itemAction != null) itemAction.accept(index); });
            hud.add(slots[i]); hud.add(Box.createVerticalStrut(2));
        }
        hud.add(Box.createVerticalGlue());
        resultTestButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        hud.add(resultTestButton);
        center.add(hud, c);

        c.gridx = 2; c.weightx = .17; c.insets = new Insets(0, 8, 0, 4);
        JPanel opponent = new JPanel(new BorderLayout(0, 4));
        opponent.setOpaque(false);
        opponent.add(enemyName, BorderLayout.NORTH);
        enemyBoard.setPreferredSize(new Dimension(140, 308));
        enemyBoard.setMinimumSize(new Dimension(100, 220));
        JPanel preview = new JPanel(new GridBagLayout());
        preview.setOpaque(false);
        preview.add(enemyBoard);
        opponent.add(preview, BorderLayout.CENTER);
        opponent.add(enemyStatus, BorderLayout.SOUTH);
        center.add(opponent, c);
        add(center, BorderLayout.CENTER);
        status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(status, BorderLayout.SOUTH);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        return label;
    }
    private static JButton button(String text) {
        JButton button = new PixelButton(text);
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        button.setForeground(TEXT);
        button.setBackground(new Color(34, 51, 74));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(94, 139, 163), 2),
                new EmptyBorder(6, 8, 6, 8)));
        button.setFocusPainted(false);
        return button;
    }

    public void setPlayers(PlayerData player, PlayerData enemy) {
        playerName.setText(player.getNickname());
        enemyName.setText(enemy.getNickname());
        arena.update(player.getNickname(), 100, 100, enemy.getNickname(), 100, 100);
    }
    /** 기존 미리보기 API 호환. 실제 아이템은 setState의 스냅샷으로 표시한다. */
    public void setItems(ItemData[] playerItems, ItemData[] enemyItems) { }
    public void startBattle() { playerBoard.start(); }
    public void setResultAction(ActionListener listener) { resultTestButton.addActionListener(listener); }
    public void setItemAction(IntConsumer action) { itemAction = action; }
    @Override public void doLayout() {
        int arenaHeight = getHeight() < 740 ? 150 : 225;
        if (arena.getPreferredSize().height != arenaHeight)
            arena.setPreferredSize(new Dimension(850, arenaHeight));
        resultTestButton.setVisible(getHeight() >= 740);
        super.doLayout();
    }
    public void setMode(boolean online) { title.setText(online ? "PVP BATTLE" : "MONSTER BATTLE"); }
    public void setFeedback(String text) {
        feedbackUntil = System.currentTimeMillis() + 2500;
        status.setText(text);
        status.setForeground(new Color(255, 210, 133));
    }
    public Board getPlayerBoard() { return playerBoard; }
    public Board getEnemyBoard() { return enemyBoard; }

    public void setState(BattleState state, String localId) {
        if (state == null || localId == null) return;
        ParticipantState local = state.getParticipant(localId);
        if (local == null) return;
        ParticipantState enemy = null;
        for (ParticipantState participant : state.getParticipants().values())
            if (!localId.equals(participant.getId())) { enemy = participant; break; }
        if (enemy == null) return;
        playerName.setText(local.getName());
        enemyName.setText(enemy.getName());
        playerBoard.setState(local.getGameState());
        enemyBoard.setState(enemy.getGameState());
        arena.update(local.getName(), local.getHp(), local.getMaxHp(),
                enemy.getName(), enemy.getHp(), enemy.getMaxHp());
        GameState game = local.getGameState();
        holdPreview.setPiece(game.getHoldPiece(), game.getHoldItemId() != null);
        for (int index = 0; index < nextPreviews.length; index++)
            nextPreviews[index].setPiece(index < game.getNextPieces().size()
                    ? game.getNextPieces().get(index) : PieceType.EMPTY, false);
        gauge.setText(local.isFeverActive() ? "FEVER  발동 중 " +
                String.format("%.1fs", local.getFeverRemainingMillis() / 1000.0)
                : "FEVER  " + local.getFever() + "%");
        warp.setText(local.getTimeWarpRemainingMillis() > 0
                ? String.format("시간 왜곡 %.1fs · 낙하 %dms",
                local.getTimeWarpRemainingMillis() / 1000.0, local.getGravityMillis()) : " ");
        pending.setText("가비지 대기 " + local.getPendingGarbageLines()
                + (local.getPendingGarbageLines() == 0 ? ""
                : "  " + (local.getGarbageWaitRemainingMillis() / 1000.0) + "s"));
        List<String> items = local.getItems();
        for (int i = 0; i < slots.length; i++) {
            boolean locked = i >= local.getItemSlots();
            boolean available = !locked && i < items.size() && items.get(i) != null;
            String item = available ? items.get(i) : null;
            slots[i].setText((i + 1) + "  " + (locked ? "잠김"
                    : item == null ? "비어 있음" : itemName(item)));
            boolean automatic = "damage_boost".equals(item) || "shield".equals(item);
            slots[i].setToolTipText(locked ? "유틸형 캐릭터가 4번 슬롯을 사용할 수 있습니다."
                    : item == null ? "비어 있는 슬롯"
                    : automatic ? itemName(item) + " · 조건이 맞으면 자동 사용"
                    : itemName(item) + " · 클릭 또는 " + (i + 1) + " 키");
            slots[i].setEnabled(item != null && !automatic
                    && state.getStatus() == BattleState.Status.RUNNING);
        }
        clock.setText(String.format("%02d:%02d", state.getElapsedMillis() / 60000,
                state.getElapsedMillis() / 1000 % 60));
        if (System.currentTimeMillis() >= feedbackUntil) {
            status.setForeground(TEXT);
            status.setText(state.getStatus() == BattleState.Status.PAUSED ? "일시정지 · P 키로 계속"
                    : "← → 이동  ↑ ↓ 회전  D 빠른 낙하  SPACE 즉시 낙하  C HOLD  P 일시정지  1–4 아이템");
        }
    }
    private static String itemName(String id) {
        switch (id) {
            case "damage_boost": return "공격 증폭";
            case "garbage_bomb": return "가비지 폭탄";
            case "heal": return "회복";
            case "shield": return "보호막";
            case "line_cleaner": return "줄 정리";
            case "fever_charge": return "피버 충전";
            case "time_warp": return "시간 왜곡";
            case "nullify": return "무효화";
            default: return id;
        }
    }
}

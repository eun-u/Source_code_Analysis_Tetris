package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.battle.*;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.GameEvent;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.audio.AudioService;
import kr.ac.jbnu.se.tetris.core.PieceType;
import kr.ac.jbnu.se.tetris.ui.seongeun.Board;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.MiniPiecePreview;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelArena;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;

/** 전투 무대를 보드 위에 두고 전투 상태를 실시간으로 읽는 게임 화면. */
public class BattlePanel extends JPanel implements Scrollable {
    private static final Color BG = UniversityPixelTheme.BG, TEXT = UniversityPixelTheme.TEXT;
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
    private final boolean[] automaticSlots = new boolean[4];
    private final JButton resultTestButton = button("전투 정보");
    private final JButton backButton = button("돌아가기 [ESC]");
    private IntConsumer itemAction;
    private long feedbackUntil;
    private final JLabel title = label("MONSTER BATTLE");
    private final JProgressBar feverBar = new JProgressBar(0, 100);
    private AudioService audio;
    private long lastEventId;
    private ParticipantState previousLocal, previousEnemy;
    private boolean online;

    public BattlePanel() {
        setLayout(new BorderLayout(0, 8));
        setPreferredSize(new Dimension(850, 660));
        setBackground(BG);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        playerBoard.setAutomaticEffects(false); enemyBoard.setAutomaticEffects(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(clock);
        backButton.setBackground(UniversityPixelTheme.GOLD);
        backButton.setFocusable(false);
        backButton.setToolTipText("현재 전투를 종료하고 돌아갑니다. 진행 중인 대전은 기권 처리됩니다.");
        actions.add(backButton);
        top.add(actions, BorderLayout.EAST);
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
        c.gridx = 0; c.weightx = .39;
        JPanel local = new JPanel(new BorderLayout(0, 4));
        local.setOpaque(false);
        local.add(playerName, BorderLayout.NORTH);
        local.add(playerBoard, BorderLayout.CENTER);
        local.add(playerStatus, BorderLayout.SOUTH);
        center.add(local, c);

        c.gridx = 1; c.weightx = .40; c.insets = new Insets(0, 8, 0, 8);
        JPanel hud = new JPanel();
        hud.setBackground(UniversityPixelTheme.PANEL);
        hud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.GOLD, 2),
                new EmptyBorder(8, 12, 8, 12)));
        hud.setLayout(new BoxLayout(hud, BoxLayout.Y_AXIS));
        for (JLabel hudLabel : new JLabel[] { hold, next, gauge, warp, pending }) {
            hudLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        warp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        holdPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel inventoryTitle = label("아이템  /  1–4 키");
        inventoryTitle.setForeground(UniversityPixelTheme.GOLD);
        inventoryTitle.setFont(UniversityPixelTheme.font(16, Font.BOLD));
        inventoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel previews = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        previews.setOpaque(false);
        previews.setPreferredSize(new Dimension(218, 70));
        previews.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (MiniPiecePreview preview : nextPreviews) previews.add(preview);
        next.setForeground(UniversityPixelTheme.GOLD);
        next.setFont(UniversityPixelTheme.font(17, Font.BOLD));
        hold.setForeground(UniversityPixelTheme.GOLD);
        hold.setFont(UniversityPixelTheme.font(15, Font.BOLD));
        JPanel queue = new JPanel(new BorderLayout(4, 0)); queue.setOpaque(false);
        queue.setAlignmentX(Component.LEFT_ALIGNMENT);
        queue.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        JPanel upcoming = new JPanel(); upcoming.setOpaque(false);
        upcoming.setLayout(new BoxLayout(upcoming, BoxLayout.Y_AXIS));
        upcoming.add(next); upcoming.add(previews);
        JPanel saved = new JPanel(); saved.setOpaque(false);
        saved.setLayout(new BoxLayout(saved, BoxLayout.Y_AXIS));
        saved.add(hold); saved.add(holdPreview);
        queue.add(upcoming, BorderLayout.WEST); queue.add(saved, BorderLayout.EAST);
        hud.add(queue); hud.add(Box.createVerticalStrut(3));
        hud.add(gauge);
        feverBar.setForeground(UniversityPixelTheme.GOLD);
        feverBar.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        feverBar.setMaximumSize(new Dimension(260, 14));
        feverBar.setAlignmentX(Component.LEFT_ALIGNMENT); hud.add(feverBar);
        hud.add(warp); hud.add(Box.createVerticalStrut(5));
        hud.add(pending); hud.add(Box.createVerticalStrut(8));
        hud.add(inventoryTitle); hud.add(Box.createVerticalStrut(6));
        for (int i = 0; i < slots.length; i++) {
            final int index = i;
            slots[i] = button((i + 1) + "  비어 있음");
            slots[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            slots[i].setFont(UniversityPixelTheme.font(14, Font.BOLD));
            slots[i].setMaximumSize(new Dimension(270, 38));
            slots[i].setPreferredSize(new Dimension(250, 38));
            slots[i].addActionListener(event -> {
                if (automaticSlots[index]) setFeedback("자동 사용 아이템 · 조건이 맞으면 발동합니다.");
                else if (itemAction != null) itemAction.accept(index);
            });
            slots[i].setEnabled(false);
            hud.add(slots[i]); hud.add(Box.createVerticalStrut(2));
        }
        hud.add(Box.createVerticalGlue());
        resultTestButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        hud.add(resultTestButton);
        center.add(hud, c);

        c.gridx = 2; c.weightx = .21; c.insets = new Insets(0, 8, 0, 4);
        JPanel opponent = new JPanel(new BorderLayout(0, 4));
        opponent.setOpaque(false);
        opponent.add(enemyName, BorderLayout.NORTH);
        enemyBoard.setPreferredSize(new Dimension(90, 198));
        enemyBoard.setMinimumSize(new Dimension(80, 176));
        JPanel preview = new JPanel(new GridBagLayout());
        preview.setOpaque(false);
        preview.add(enemyBoard);
        JPanel sidebar = new JPanel(new BorderLayout(0, 6));
        sidebar.setOpaque(false);
        sidebar.add(preview, BorderLayout.CENTER);
        sidebar.add(keyGuide(), BorderLayout.SOUTH);
        opponent.add(sidebar, BorderLayout.CENTER);
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

    private static JPanel keyGuide() {
        JPanel guide = new JPanel(new GridLayout(0, 1, 0, 3));
        guide.setOpaque(false);
        String[] keys = { "← →  이동", "↑ ↓  회전", "D  한 칸 낙하", "SPACE  즉시 낙하",
                "C  HOLD", "P  일시정지", "1–4  아이템", "ESC  돌아가기" };
        for (String text : keys) {
            JLabel chip = label(text);
            chip.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            chip.setOpaque(true);
            chip.setBackground(UniversityPixelTheme.PANEL);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 1),
                    new EmptyBorder(1, 7, 1, 7)));
            guide.add(chip);
        }
        return guide;
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(16, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 16);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
    }

    public void setPlayers(PlayerData player, PlayerData enemy) {
        playerName.setText(player.getNickname());
        enemyName.setText(enemy.getNickname());
        arena.resetForEncounter();
    }
    /** 기존 미리보기 API 호환. 실제 아이템은 setState의 스냅샷으로 표시한다. */
    public void setItems(ItemData[] playerItems, ItemData[] enemyItems) { }
    public void startBattle() { playerBoard.start(); }
    public void setResultAction(ActionListener listener) { resultTestButton.addActionListener(listener); }
    public void setItemAction(IntConsumer action) { itemAction = action; }
    public void setBackAction(ActionListener action) { backButton.addActionListener(action); }
    @Override public void doLayout() {
        int arenaHeight = getHeight() < 740 ? 130 : 205;
        if (arena.getPreferredSize().height != arenaHeight)
            arena.setPreferredSize(new Dimension(850, arenaHeight));
        resultTestButton.setVisible(false);
        super.doLayout();
    }
    public void setMode(boolean online) {
        this.online = online;
        if (online) title.setText("ONLINE PvP BATTLE");
        arena.setOnline(online);
    }
    public void setAudio(AudioService service) { audio = service; }
    public void setEncounter(String chapter, int level, MonsterTier pattern, String art) {
        title.setText("CAMPUS QUEST  /  LV " + level);
        arena.setChapter(chapter); arena.setMonsterPattern(pattern); arena.setMonsterArt(art); arena.setOnline(false);
    }
    public void resetFeedback() {
        previousLocal = previousEnemy = null; lastEventId = 0; feedbackUntil = 0;
        playerBoard.resetEffects(); enemyBoard.resetEffects();
        arena.resetForEncounter();
    }
    public void finish(boolean won) { arena.showEffect(won ? PixelArena.Effect.VICTORY : PixelArena.Effect.DEFEAT, 0); }
    private void sound(AudioService.Event event) { if (audio != null) audio.play(event); }
    public void applyEvents(List<BattleEvent> events, String localId) {
        for (BattleEvent event : events) {
            if (event.getEventId() <= lastEventId) continue;
            lastEventId = event.getEventId();
            boolean local = localId != null && localId.equals(event.getActorId());
            Board board = local ? playerBoard : enemyBoard;
            if (event.getType() == BattleEvent.Type.DAMAGE) {
                arena.showEffect(local ? PixelArena.Effect.ATTACK : PixelArena.Effect.MONSTER_ATTACK,
                        event.getAmount(), local);
                sound(local ? AudioService.Event.ATTACK : AudioService.Event.HIT);
            } else if (event.getType() == BattleEvent.Type.GARBAGE_RECEIVED) {
                board.showGarbage(event.getAmount());
            } else if (event.getType() == BattleEvent.Type.ITEM_ACQUIRED) {
                arena.showEffect(PixelArena.Effect.ITEM_ACQUIRE, event.getAmount(), !local);
                if (local) { sound(AudioService.Event.ITEM_ACQUIRE); setFeedback("아이템 획득 · " + itemName(event.getReason())); }
            } else if (event.getType() == BattleEvent.Type.ITEM_USED) {
                boolean targetMonster = localId != null && !localId.equals(event.getTargetId());
                arena.showItemEffect(event.getReason(), targetMonster); sound(AudioService.Event.ITEM_USE);
                if (local) setFeedback("아이템 사용 · " + itemName(event.getReason()));
            } else if (event.getType() == BattleEvent.Type.ITEM_REMOVED) {
                if (localId != null && localId.equals(event.getTargetId())) setFeedback("아이템이 무효화됨 · " + itemName(event.getReason()));
            } else if (event.getType() == BattleEvent.Type.CORE_EVENT && event.getCoreEvent() != null) {
                GameEvent core = event.getCoreEvent();
                if (core.getType() == GameEvent.Type.PIECE_PLACED) {
                    board.showPlacement(core.getPiece(), core.getX(), core.getY());
                    if (local) sound(AudioService.Event.DROP);
                } else if (core.getType() == GameEvent.Type.LINE_CLEAR) {
                    board.showLineClear(core.getLineCount(), core.getCombo(), core.isPerfectClear());
                    arena.showEffect(PixelArena.Effect.LINE_CLEAR, core.getLineCount(), !local);
                    if (local) sound(AudioService.Event.LINE_CLEAR);
                } else if (local && core.getType() == GameEvent.Type.PIECE_ROTATED) sound(AudioService.Event.ROTATE);
            }
        }
    }
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
        if (previousLocal != null && local.getHp() > previousLocal.getHp()) sound(AudioService.Event.HEAL);
        if (previousEnemy != null && enemy.getHp() > previousEnemy.getHp()) sound(AudioService.Event.HEAL);
        if (online) {
            onlineEffects(previousLocal, local, playerBoard, false);
            onlineEffects(previousEnemy, enemy, enemyBoard, true);
        }
        if (previousLocal != null && !previousLocal.isFeverActive() && local.isFeverActive()) {
            arena.showEffect(PixelArena.Effect.FEVER, 0); sound(AudioService.Event.FEVER);
        }
        previousLocal = local; previousEnemy = enemy;
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
        feverBar.setValue(local.isFeverActive() ? 100 : local.getFever());
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
            slots[i].setText("[" + (i + 1) + "]  " + (locked ? "잠김"
                    : item == null ? "빈 슬롯" : itemName(item) + ("damage_boost".equals(item)
                    || "shield".equals(item) ? " · AUTO" : "")));
            boolean automatic = "damage_boost".equals(item) || "shield".equals(item);
            automaticSlots[i] = automatic;
            slots[i].setBackground(available ? automatic ? UniversityPixelTheme.MINT
                    : UniversityPixelTheme.GOLD : UniversityPixelTheme.PANEL_LIGHT);
            slots[i].setToolTipText(locked ? "유틸형 캐릭터가 4번 슬롯을 사용할 수 있습니다."
                    : item == null ? "비어 있는 슬롯"
                    : automatic ? itemName(item) + " · 조건이 맞으면 자동 사용"
                    : itemName(item) + " · 클릭 또는 " + (i + 1) + " 키");
            slots[i].setEnabled(item != null
                    && state.getStatus() == BattleState.Status.RUNNING);
        }
        clock.setText(String.format("%02d:%02d", state.getElapsedMillis() / 60000,
                state.getElapsedMillis() / 1000 % 60));
        if (System.currentTimeMillis() >= feedbackUntil) {
            status.setForeground(TEXT);
            status.setText(state.getStatus() == BattleState.Status.PAUSED ? "일시정지 · P 키로 계속"
                    : "테트리스로 공격하세요 · 조작은 오른쪽 키 안내 · ESC로 돌아가기");
        }
    }
    private void onlineEffects(ParticipantState previous, ParticipantState current, Board board, boolean monster) {
        if (previous == null) return;
        int lines = current.getGameState().getLinesCleared() - previous.getGameState().getLinesCleared();
        if (lines > 0) {
            board.showLineClear(lines, current.getGameState().getCombo(), false);
            arena.showEffect(PixelArena.Effect.LINE_CLEAR, lines, monster);
            if (!monster) sound(AudioService.Event.LINE_CLEAR);
        }
        if (current.getHp() < previous.getHp()) {
            arena.showEffect(monster ? PixelArena.Effect.ATTACK : PixelArena.Effect.MONSTER_ATTACK,
                    previous.getHp() - current.getHp(), monster);
            sound(monster ? AudioService.Event.ATTACK : AudioService.Event.HIT);
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

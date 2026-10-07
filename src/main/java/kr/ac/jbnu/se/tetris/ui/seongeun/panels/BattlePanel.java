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
import kr.ac.jbnu.se.tetris.item.ItemSpec;
import kr.ac.jbnu.se.tetris.ui.seongeun.Board;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GarbageMeter;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.MiniPiecePreview;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelArena;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelMeter;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;

/**
 * 전투 화면. 테트리스 게임처럼 보드 왼쪽에 HOLD·아이템, 오른쪽에 NEXT·피버를 붙이고,
 * 두 보드 사이의 넓은 무대에서 공격과 피격을 연출한다. 창 크기에 맞춰 보드 칸 크기를 계산한다.
 */
public class BattlePanel extends JPanel implements Scrollable {
    private static final Color BG = UniversityPixelTheme.BG, TEXT = UniversityPixelTheme.TEXT;
    private static final Color PINK = new Color(0xFF92E2);
    private static final int PAD = 10, GAP = 8, TOP = 50, BOTTOM = 28, METER = 12;

    private final PixelArena arena = new PixelArena();
    private final JLabel playerName = label("PLAYER"), enemyName = label("MONSTER");
    private final JLabel playerStatus = label("LINES 0"), enemyStatus = label("LINES 0");
    private final JLabel hold = label("HOLD"), next = label("NEXT");
    private final MiniPiecePreview holdPreview = new MiniPiecePreview("HOLD");
    private final MiniPiecePreview[] nextPreviews = {
        new MiniPiecePreview("1"), new MiniPiecePreview("2"), new MiniPiecePreview("3")
    };
    private final JLabel gauge = label("FEVER 0%"), pending = label("가비지 0");
    private final JLabel warp = label(" ");
    private final JLabel stats = label("<html>LINES 0<br>COMBO 0</html>");
    private final JLabel status = label("줄을 지우면 상대를 공격합니다");
    private final JLabel clock = label("00:00");
    private final JLabel levelChip = UniversityPixelTheme.chip("LV 1", UniversityPixelTheme.GOLD);
    private final JLabel title = label("MONSTER BATTLE");
    private final Board playerBoard = new Board(playerStatus, true);
    private final Board enemyBoard = new Board(enemyStatus, false);
    private final GarbageMeter garbage = new GarbageMeter();
    private final JButton[] slots = new JButton[4];
    private final boolean[] automaticSlots = new boolean[4];
    private final JButton resultTestButton = button("전투 정보");
    private final JButton backButton = button("돌아가기 [ESC]");
    private final PixelMeter feverBar = new PixelMeter(100, UniversityPixelTheme.GOLD);
    private final JPanel topBar = new JPanel(new BorderLayout(10, 0));
    private final JPanel leftColumn = column();
    private final JPanel rightColumn = column();
    private final JPanel enemyColumn = new JPanel(null);
    private IntConsumer itemAction;
    private long feedbackUntil;
    private AudioService audio;
    private long lastEventId;
    private ParticipantState previousLocal, previousEnemy;
    private boolean online;

    public BattlePanel() {
        setLayout(null);
        setPreferredSize(new Dimension(850, 600));
        setBackground(BG);
        playerBoard.setAutomaticEffects(false); enemyBoard.setAutomaticEffects(false);

        // 상단: 레벨·과정 이름과 시간, 포기 버튼.
        topBar.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); heading.setOpaque(false);
        levelChip.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        title.setFont(UniversityPixelTheme.font(17, Font.BOLD));
        heading.add(levelChip); heading.add(title);
        topBar.add(heading, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); actions.setOpaque(false);
        clock.setFont(UniversityPixelTheme.font(18, Font.BOLD));
        clock.setForeground(UniversityPixelTheme.GOLD);
        actions.add(clock);
        ((PixelButton) backButton).danger();
        backButton.setPreferredSize(new Dimension(136, 34));
        backButton.setFocusable(false);
        backButton.setToolTipText("현재 전투를 종료하고 돌아갑니다. 진행 중인 대전은 기권 처리됩니다.");
        actions.add(backButton);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar);

        buildLeftColumn();
        add(leftColumn);
        add(garbage);
        add(playerBoard);
        buildRightColumn();
        add(rightColumn);
        add(arena);
        buildEnemyColumn();
        add(enemyColumn);
        status.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        status.setForeground(UniversityPixelTheme.TEXT_SUB);
        add(status);
    }

    private void buildLeftColumn() {
        section(leftColumn, hold, UniversityPixelTheme.GOLD);
        holdPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(holdPreview);
        leftColumn.add(Box.createVerticalStrut(14));
        section(leftColumn, label("ITEM"), UniversityPixelTheme.GOLD);
        JLabel keyHint = label("숫자 1–4 키로 사용");
        keyHint.setFont(UniversityPixelTheme.font(10, Font.PLAIN));
        keyHint.setForeground(UniversityPixelTheme.TEXT_SUB);
        keyHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftColumn.add(keyHint);
        leftColumn.add(Box.createVerticalStrut(4));
        for (int i = 0; i < slots.length; i++) {
            final int index = i;
            slots[i] = button((i + 1) + "  빈 슬롯");
            slots[i].setFont(UniversityPixelTheme.font(12, Font.BOLD));
            slots[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            slots[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            slots[i].setPreferredSize(new Dimension(100, 38));
            slots[i].addActionListener(event -> {
                if (automaticSlots[index]) setFeedback("자동 사용 아이템 · 조건이 맞으면 발동합니다.");
                else if (itemAction != null) itemAction.accept(index);
            });
            slots[i].setEnabled(false);
            leftColumn.add(slots[i]);
            leftColumn.add(Box.createVerticalStrut(4));
        }
        leftColumn.add(Box.createVerticalGlue());
        String[][] keys = { {"←→", "이동"}, {"↑↓", "회전"}, {"SPACE", "낙하"}, {"C", "HOLD"}, {"P", "정지"} };
        for (String[] key : keys) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1)); row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel chip = UniversityPixelTheme.chip(key[0], UniversityPixelTheme.PANEL_LIGHT);
            chip.setFont(UniversityPixelTheme.font(10, Font.BOLD));
            row.add(chip);
            JLabel action = label(key[1]);
            action.setFont(UniversityPixelTheme.font(10, Font.PLAIN));
            action.setForeground(UniversityPixelTheme.TEXT_SUB);
            row.add(action);
            leftColumn.add(row);
        }
    }

    private void buildRightColumn() {
        section(rightColumn, next, UniversityPixelTheme.GOLD);
        for (MiniPiecePreview preview : nextPreviews) {
            preview.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightColumn.add(preview);
            rightColumn.add(Box.createVerticalStrut(4));
        }
        rightColumn.add(Box.createVerticalStrut(10));
        gauge.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        section(rightColumn, gauge, PINK);
        feverBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(feverBar);
        rightColumn.add(Box.createVerticalStrut(3));
        warp.setFont(UniversityPixelTheme.font(10, Font.BOLD));
        warp.setForeground(UniversityPixelTheme.MINT);
        warp.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(warp);
        rightColumn.add(Box.createVerticalStrut(10));
        pending.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        pending.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(pending);
        rightColumn.add(Box.createVerticalGlue());
        // 보드가 쓰는 한 줄 상태 대신 좁은 칸에 맞게 두 줄 기록을 직접 보여 준다.
        playerStatus.setVisible(false);
        rightColumn.add(playerStatus);
        stats.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        stats.setForeground(UniversityPixelTheme.TEXT_SUB);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightColumn.add(stats);
    }

    private void buildEnemyColumn() {
        enemyColumn.setOpaque(false);
        enemyName.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        enemyName.setForeground(UniversityPixelTheme.CORAL);
        enemyName.setHorizontalAlignment(SwingConstants.CENTER);
        enemyStatus.setFont(UniversityPixelTheme.font(11, Font.BOLD));
        enemyStatus.setForeground(UniversityPixelTheme.TEXT_SUB);
        enemyStatus.setHorizontalAlignment(SwingConstants.CENTER);
        enemyColumn.add(enemyName);
        enemyColumn.add(enemyBoard);
        enemyColumn.add(enemyStatus);
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static void section(JPanel parent, JLabel heading, Color color) {
        heading.setForeground(color);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setBorder(new EmptyBorder(0, 0, 4, 0));
        parent.add(heading);
    }

    /** 보드 칸 크기를 남은 높이와 너비에서 정하고, 나머지 너비를 무대에 준다. */
    @Override public void doLayout() {
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        topBar.setBounds(PAD, 8, width - PAD * 2, 36);
        status.setBounds(PAD + 4, height - BOTTOM + 2, width - PAD * 2, 22);
        int areaTop = TOP, areaHeight = Math.max(100, height - TOP - BOTTOM);
        int leftWidth = clamp(Math.round(width * 0.115f), 92, 130);
        int rightWidth = clamp(Math.round(width * 0.095f), 80, 108);
        int enemyWidth = clamp(Math.round(width * 0.13f), 96, 150);
        int minimumArena = Math.max(200, Math.round(width * 0.26f));
        int boardSpace = width - PAD * 2 - leftWidth - rightWidth - enemyWidth - METER - 2 - GAP * 5 - minimumArena;
        int cell = Math.max(10, Math.min((areaHeight - 2) / 22, (boardSpace - 2) / 10));
        int boardWidth = cell * 10 + 2, boardHeight = cell * 22 + 2;
        int boardY = areaTop + (areaHeight - boardHeight) / 2;

        int x = PAD;
        leftColumn.setBounds(x, boardY, leftWidth, boardHeight);
        x += leftWidth + GAP;
        garbage.setBounds(x, boardY + 1, METER, boardHeight - 2);
        x += METER + 2;
        playerBoard.setBounds(x, boardY, boardWidth, boardHeight);
        x += boardWidth + GAP;
        rightColumn.setBounds(x, boardY, rightWidth, boardHeight);
        x += rightWidth + GAP;
        int enemyX = width - PAD - enemyWidth;
        arena.setBounds(x, areaTop, Math.max(120, enemyX - GAP - x), areaHeight);

        enemyColumn.setBounds(enemyX, areaTop, enemyWidth, areaHeight);
        int enemyCell = Math.max(4, Math.min((enemyWidth - 2) / 10, (areaHeight - 52) / 22));
        int enemyBoardWidth = enemyCell * 10 + 2, enemyBoardHeight = enemyCell * 22 + 2;
        int enemyTop = (areaHeight - enemyBoardHeight) / 2;
        enemyName.setBounds(0, Math.max(0, enemyTop - 22), enemyWidth, 18);
        enemyBoard.setBounds((enemyWidth - enemyBoardWidth) / 2, enemyTop, enemyBoardWidth, enemyBoardHeight);
        enemyStatus.setBounds(0, enemyTop + enemyBoardHeight + 4, enemyWidth, 18);
        resultTestButton.setVisible(false);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        return label;
    }
    private static JButton button(String text) {
        PixelButton button = new PixelButton(text);
        button.setFont(UniversityPixelTheme.font(12, Font.BOLD));
        button.secondary();
        return button;
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(16, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 16);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    /** 칸 크기를 창에 맞춰 줄이므로 세로 스크롤 없이 항상 한 화면에 보인다. */
    @Override public boolean getScrollableTracksViewportHeight() { return true; }

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
    public void setMode(boolean online) {
        this.online = online;
        if (online) {
            UniversityPixelTheme.setChip(levelChip, "ONLINE", UniversityPixelTheme.MINT);
            title.setText("1:1 PvP 대전");
        }
        arena.setOnline(online);
    }
    public void setAudio(AudioService service) { audio = service; }
    public void setEncounter(String chapter, int level, MonsterTier pattern, String art) {
        UniversityPixelTheme.setChip(levelChip, "LV " + level,
                pattern == MonsterTier.BOSS ? UniversityPixelTheme.CORAL : UniversityPixelTheme.GOLD);
        String course = "graduation".equals(chapter) ? "졸업 과정" : "employment".equals(chapter) ? "취업 과정" : "대학교 과정";
        String tier = pattern == MonsterTier.BOSS ? "BOSS BATTLE" : pattern == MonsterTier.ELITE ? "ELITE" : "BATTLE";
        title.setText(course + "  ·  " + tier);
        arena.setChapter(chapter); arena.setMonsterPattern(pattern); arena.setMonsterArt(art); arena.setOnline(false);
    }
    public void resetFeedback() {
        previousLocal = previousEnemy = null; lastEventId = 0; feedbackUntil = 0;
        playerBoard.resetEffects(); enemyBoard.resetEffects();
        arena.resetForEncounter();
        garbage.setPending(0, 0);
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
                    arena.showClear(core.getLineCount(), core.getCombo(), core.isPerfectClear(), !local);
                    if (local) sound(AudioService.Event.LINE_CLEAR);
                } else if (local && core.getType() == GameEvent.Type.PIECE_ROTATED) sound(AudioService.Event.ROTATE);
            }
        }
    }
    public void setFeedback(String text) {
        feedbackUntil = System.currentTimeMillis() + 2500;
        status.setText("▶ " + text);
        status.setForeground(UniversityPixelTheme.GOLD);
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
        arena.setFever(local.isFeverActive());
        GameState game = local.getGameState();
        stats.setText("<html>LINES " + game.getLinesCleared() + "<br>COMBO " + Math.max(0, game.getCombo()) + "</html>");
        holdPreview.setPiece(game.getHoldPiece(), game.getHoldItemId() != null);
        for (int index = 0; index < nextPreviews.length; index++)
            nextPreviews[index].setPiece(index < game.getNextPieces().size()
                    ? game.getNextPieces().get(index) : PieceType.EMPTY, false);
        gauge.setText(local.isFeverActive() ? String.format("FEVER %.1fs", local.getFeverRemainingMillis() / 1000.0)
                : "FEVER " + local.getFever() + "%");
        feverBar.setValue(local.isFeverActive() ? 100 : local.getFever());
        feverBar.setFill(local.isFeverActive() ? PINK : UniversityPixelTheme.GOLD);
        warp.setText(local.getTimeWarpRemainingMillis() > 0
                ? String.format("시간 왜곡 %.1fs", local.getTimeWarpRemainingMillis() / 1000.0) : " ");
        int incoming = local.getPendingGarbageLines();
        garbage.setPending(incoming, local.getGarbageWaitRemainingMillis());
        pending.setText(incoming == 0 ? "가비지 0" : String.format("가비지 %d · %.1fs", incoming,
                local.getGarbageWaitRemainingMillis() / 1000.0));
        pending.setForeground(incoming == 0 ? UniversityPixelTheme.TEXT_SUB : UniversityPixelTheme.CORAL);
        List<String> items = local.getItems();
        List<Integer> charges = local.getItemCharges();
        for (int i = 0; i < slots.length; i++) {
            boolean locked = i >= local.getItemSlots();
            boolean available = !locked && i < items.size() && items.get(i) != null;
            String item = available ? items.get(i) : null;
            int count = available && i < charges.size() ? charges.get(i) : 1;
            boolean automatic = "damage_boost".equals(item) || "shield".equals(item);
            slots[i].setText((i + 1) + "  " + (locked ? "잠김" : item == null ? "빈 슬롯"
                    : ItemSpec.categoryOf(item) + " " + itemShortName(item)
                    + (count > 1 ? " ×" + count : "")));
            automaticSlots[i] = automatic;
            slots[i].setBackground(available ? automatic ? UniversityPixelTheme.MINT
                    : UniversityPixelTheme.GOLD : UniversityPixelTheme.PANEL_LIGHT);
            slots[i].setToolTipText(locked ? "유틸형 캐릭터가 4번 슬롯을 사용할 수 있습니다."
                    : item == null ? "비어 있는 슬롯"
                    : itemName(item) + " [" + ItemSpec.categoryOf(item) + "] · " + count + "회 · "
                    + (automatic ? "조건이 맞으면 자동 사용" : "클릭 또는 " + (i + 1) + " 키"));
            slots[i].setEnabled(item != null
                    && state.getStatus() == BattleState.Status.RUNNING);
        }
        clock.setText(String.format("%02d:%02d", state.getElapsedMillis() / 60000,
                state.getElapsedMillis() / 1000 % 60));
        if (System.currentTimeMillis() >= feedbackUntil) {
            status.setForeground(UniversityPixelTheme.TEXT_SUB);
            status.setText(state.getStatus() == BattleState.Status.PAUSED ? "일시정지 · P 키로 계속"
                    : "줄을 지우면 공격 · 여러 줄을 한 번에, 연속으로 지울수록 강해집니다 · ESC 포기");
        }
    }
    private void onlineEffects(ParticipantState previous, ParticipantState current, Board board, boolean monster) {
        if (previous == null) return;
        int lines = current.getGameState().getLinesCleared() - previous.getGameState().getLinesCleared();
        if (lines > 0) {
            board.showLineClear(lines, current.getGameState().getCombo(), false);
            arena.showClear(lines, current.getGameState().getCombo(), false, monster);
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
    private static String itemShortName(String id) {
        switch (id) {
            case "damage_boost": return "피해↑";
            case "garbage_bomb": return "폭탄";
            case "heal": return "회복";
            case "shield": return "방어";
            case "line_cleaner": return "클리너";
            case "fever_charge": return "피버";
            case "time_warp": return "왜곡";
            case "nullify": return "무효화";
            default: return id;
        }
    }
}

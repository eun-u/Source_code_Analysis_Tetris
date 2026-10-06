package kr.ac.jbnu.se.tetris.ui.seongeun;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.core.*;

/** 엔진 스냅샷을 픽셀 격자에 그리는 화면 전용 보드. */
public class Board extends JPanel {
    private static final int COLUMNS = 10, ROWS = 22;
    private static final Color EMPTY = new Color(11, 11, 46);
    private static final Color GRID = new Color(43, 33, 102);
    private static final Color[] COLORS = {
        EMPTY, new Color(255, 139, 113), new Color(112, 217, 155),
        new Color(114, 157, 255), new Color(255, 221, 115),
        new Color(201, 139, 255), new Color(104, 221, 235),
        new Color(253, 183, 97), new Color(132, 146, 164)
    };
    private final JLabel statusbar;
    private GameState state;
    private Consumer<GameAction.Type> inputHandler;
    private Runnable pauseHandler;
    private Consumer<Integer> itemHandler;
    private String overlayText;
    private final boolean keyboardEnabled;
    private final Map<Integer, Runnable> keyActions = new LinkedHashMap<Integer, Runnable>();
    private final KeyEventDispatcher keyDispatcher = event -> {
        if (event.getID() != KeyEvent.KEY_PRESSED || event.getModifiersEx() != 0
                || !isShowing() || state == null ||
                (state.getStatus() != GameState.Status.RUNNING
                        && state.getStatus() != GameState.Status.PAUSED)) return false;
        Runnable action = keyActions.get(event.getKeyCode());
        if (action == null || MenuSelectionManager.defaultManager().getSelectedPath().length != 0)
            return false;
        Window gameWindow = SwingUtilities.getWindowAncestor(this);
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (gameWindow == null || focusOwner == null
                || SwingUtilities.getWindowAncestor(focusOwner) != gameWindow) return false;
        action.run();
        return true;
    };

    public Board(JLabel statusbar, boolean keyboardEnable) {
        this.statusbar = statusbar;
        keyboardEnabled = keyboardEnable;
        setBackground(EMPTY);
        setPreferredSize(new Dimension(260, 572));
        setMinimumSize(new Dimension(120, 264));
        setFocusable(true);
        if (keyboardEnable) bindKeys();
    }
    @Override public void addNotify() {
        super.addNotify();
        if (keyboardEnabled)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
    }
    @Override public void removeNotify() {
        if (keyboardEnabled)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        super.removeNotify();
    }
    public void start() { requestFocusInWindow(); }
    public void setState(GameState next) {
        state = next;
        if (statusbar != null && next != null) {
            if (next.getStatus() == GameState.Status.PAUSED) statusbar.setText("일시정지");
            else if (next.getStatus() == GameState.Status.GAME_OVER) statusbar.setText("게임 종료");
            else statusbar.setText("LINES  " + next.getLinesCleared() + "    COMBO  " + Math.max(0, next.getCombo()));
        }
        repaint();
    }
    public void setInputHandlers(Consumer<GameAction.Type> input, Runnable pause) {
        inputHandler = input;
        pauseHandler = pause;
    }
    public void setItemHandler(Consumer<Integer> handler) { itemHandler = handler; }
    public void setOverlayText(String text) { overlayText = text; repaint(); }
    private void bindKeys() {
        bind(KeyEvent.VK_LEFT, "left", () -> submit(GameAction.Type.MOVE_LEFT));
        bind(KeyEvent.VK_RIGHT, "right", () -> submit(GameAction.Type.MOVE_RIGHT));
        bind(KeyEvent.VK_UP, "rotate-left", () -> submit(GameAction.Type.ROTATE_LEFT));
        bind(KeyEvent.VK_DOWN, "rotate-right", () -> submit(GameAction.Type.ROTATE_RIGHT));
        bind(KeyEvent.VK_SPACE, "drop", () -> submit(GameAction.Type.HARD_DROP));
        bind(KeyEvent.VK_D, "soft-drop", () -> submit(GameAction.Type.SOFT_DROP));
        bind(KeyEvent.VK_C, "hold", () -> submit(GameAction.Type.HOLD));
        bind(KeyEvent.VK_P, "pause", () -> {
            if (state != null && (state.getStatus() == GameState.Status.RUNNING
                    || state.getStatus() == GameState.Status.PAUSED) && pauseHandler != null) pauseHandler.run();
        });
        for (int index = 0; index < 4; index++) {
            final int slot = index;
            bind(KeyEvent.VK_1 + index, "item-" + index, () -> {
                if (state != null && state.getStatus() == GameState.Status.RUNNING && itemHandler != null)
                    itemHandler.accept(slot);
            });
        }
    }
    private void bind(int key, String name, Runnable action) {
        keyActions.put(key, action);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { if (isShowing()) action.run(); }
        });
    }
    private void submit(GameAction.Type action) {
        if (state != null && state.getStatus() == GameState.Status.RUNNING
                && state.getActivePiece() != null && inputHandler != null) inputHandler.accept(action);
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int cell = Math.max(1, Math.min(getWidth() / COLUMNS, getHeight() / ROWS));
            int width = cell * COLUMNS, height = cell * ROWS;
            int left = (getWidth() - width) / 2, top = (getHeight() - height) / 2;
            g.setColor(EMPTY);
            g.fillRect(left, top, width, height);
            g.setColor(GRID);
            for (int x = 0; x <= COLUMNS; x++) g.drawLine(left + x * cell, top, left + x * cell, top + height);
            for (int y = 0; y <= ROWS; y++) g.drawLine(left, top + y * cell, left + width, top + y * cell);
            if (state != null && state.getBoard() != null) {
                BoardState board = state.getBoard();
                Map<Long, int[]> badges = new LinkedHashMap<Long, int[]>();
                for (int y = 0; y < ROWS; y++) for (int x = 0; x < COLUMNS; x++) {
                    PieceType type = board.getCell(x, y);
                    if (type != PieceType.EMPTY) {
                        drawCell(g, left, top, cell, x, y, type, false, null);
                        String itemId = board.getItemId(x, y);
                        if (itemId != null) {
                            long origin = board.getOriginId(x, y);
                            if (origin == 0) origin = -1L - itemId.hashCode();
                            int[] center = badges.get(origin);
                            if (center == null) { center = new int[3]; badges.put(origin, center); }
                            center[0] += left + x * cell + cell / 2;
                            center[1] += top + (ROWS - 1 - y) * cell + cell / 2;
                            center[2]++;
                        }
                    }
                }
                for (int[] center : badges.values())
                    drawBadge(g, center[0] / center[2], center[1] / center[2], cell);
                Piece piece = state.getActivePiece();
                if (piece != null) {
                    for (int i = 0; i < 4; i++) {
                        int x = state.getPieceX() + piece.x(i), y = state.getGhostY() - piece.y(i);
                        if (x >= 0 && x < COLUMNS && y >= 0 && y < ROWS && board.getCell(x, y) == PieceType.EMPTY)
                            drawCell(g, left, top, cell, x, y, piece.getType(), true, null);
                    }
                    int badgeX = 0, badgeY = 0, visible = 0;
                    for (int i = 0; i < 4; i++) {
                        int x = state.getPieceX() + piece.x(i), y = state.getPieceY() - piece.y(i);
                        if (x >= 0 && x < COLUMNS && y >= 0 && y < ROWS) {
                            drawCell(g, left, top, cell, x, y, piece.getType(), false, null);
                            badgeX += left + x * cell + cell / 2;
                            badgeY += top + (ROWS - 1 - y) * cell + cell / 2;
                            visible++;
                        }
                    }
                    if (piece.getItemId() != null && visible > 0)
                        drawBadge(g, badgeX / visible, badgeY / visible, cell);
                }
            }
            g.setColor(new Color(102, 128, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawRect(left, top, width, height);
            if (overlayText != null || state != null && (state.getStatus() == GameState.Status.PAUSED
                    || state.getStatus() == GameState.Status.GAME_OVER)) {
                g.setColor(new Color(8, 14, 27, 198));
                g.fillRect(left, top, width, height);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(14, cell)));
                String label = overlayText != null ? overlayText
                        : state.getStatus() == GameState.Status.PAUSED ? "PAUSED" : "GAME OVER";
                g.drawString(label, left + (width - g.getFontMetrics().stringWidth(label)) / 2, top + height / 2);
            }
        } finally { g.dispose(); }
    }
    private static void drawCell(Graphics2D g, int left, int top, int size, int x, int y,
                                 PieceType type, boolean ghost, String itemId) {
        int px = left + x * size, py = top + (ROWS - 1 - y) * size;
        Color color = COLORS[type.ordinal()];
        if (ghost) {
            g.setColor(color.darker());
            g.drawRect(px + 2, py + 2, Math.max(1, size - 5), Math.max(1, size - 5));
            return;
        }
        g.setColor(color.darker());
        g.fillRect(px + 1, py + 1, size - 2, size - 2);
        g.setColor(color);
        g.fillRect(px + 2, py + 2, Math.max(1, size - 5), Math.max(1, size - 5));
        g.setColor(color.brighter());
        g.fillRect(px + 3, py + 3, Math.max(1, size - 8), Math.max(2, size / 5));
    }
    private static void drawBadge(Graphics2D g, int x, int y, int size) {
        int badge = Math.max(7, size / 2);
        g.setColor(new Color(27, 32, 48));
        g.fillRect(x - badge / 2 - 1, y - badge / 2 - 1, badge + 2, badge + 2);
        g.setColor(new Color(255, 233, 144));
        g.fillRect(x - badge / 2, y - badge / 2, badge, badge);
        g.setColor(new Color(148, 84, 62));
        g.fillRect(x - 1, y - badge / 3, 2, badge * 2 / 3);
        g.fillRect(x - badge / 3, y - 1, badge * 2 / 3, 2);
    }
}

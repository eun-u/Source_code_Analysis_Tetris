package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * 게임 타이틀식 세로 메뉴. 마우스를 올리거나 ↑↓ 키로 고르고, 클릭이나 Enter로 실행한다.
 * 선택된 항목은 금색 화살표와 밝은 띠로 표시된다.
 */
public final class GameMenu extends JPanel {
    private final List<Item> items = new ArrayList<Item>();
    private int selected;
    private final Timer pulse = new Timer(60, event -> repaint());

    public GameMenu() {
        setOpaque(false);
        setLayout(new GridLayout(0, 1, 0, 4));
        pulse.setCoalesce(true);
        bind(KeyEvent.VK_UP, "menu-up", () -> move(-1));
        bind(KeyEvent.VK_DOWN, "menu-down", () -> move(1));
        bind(KeyEvent.VK_W, "menu-up-w", () -> move(-1));
        bind(KeyEvent.VK_S, "menu-down-s", () -> move(1));
        bind(KeyEvent.VK_ENTER, "menu-enter", () -> { if (!items.isEmpty()) items.get(selected).doClick(); });
    }

    public Item add(String title, String description, Color accent) {
        Item item = new Item(title, description, accent);
        final int index = items.size();
        items.add(item);
        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { select(index); }
        });
        add(item);
        return item;
    }

    private void bind(int key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            // 숨겨진 메뉴는 키를 소비하지 않아 다른 화면의 조작을 방해하지 않는다.
            @Override public boolean isEnabled() { return GameMenu.this.isShowing(); }
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private void move(int delta) {
        if (items.isEmpty()) return;
        select(Math.floorMod(selected + delta, items.size()));
    }

    public void select(int index) {
        if (index < 0 || index >= items.size()) return;
        selected = index;
        for (Component component : getComponents()) component.repaint();
    }

    @Override public void addNotify() { super.addNotify(); pulse.start(); }
    @Override public void removeNotify() { pulse.stop(); super.removeNotify(); }

    public final class Item extends JButton {
        private final Color accent;
        private String description;
        private String badge;
        private Color badgeColor;

        Item(String title, String description, Color accent) {
            super(title);
            this.accent = accent;
            this.description = description;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(300, 52));
        }

        public void setDescription(String description) { this.description = description; repaint(); }

        public void setBadge(String badge, Color color) { this.badge = badge; this.badgeColor = color; repaint(); }

        private boolean isSelectedItem() { return items.indexOf(this) == selected; }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                boolean on = isSelectedItem();
                boolean pressed = getModel().isPressed();
                long now = System.currentTimeMillis();
                if (on) {
                    // 선택 띠: 왼쪽이 진하고 오른쪽으로 사라지는 그라데이션.
                    g.setPaint(new GradientPaint(0, 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150),
                            w, 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)));
                    g.fillRect(pressed ? 2 : 0, 0, w, h);
                    g.setColor(accent);
                    g.fillRect(0, 0, 5, h);
                    int bob = (int) Math.round(Math.sin(now / 140.0) * 3);
                    int ax = 14 + bob, ay = h / 2;
                    g.setColor(UniversityPixelTheme.BLACK);
                    g.fillPolygon(new int[] { ax + 1, ax + 13, ax + 1 }, new int[] { ay - 8, ay + 1, ay + 10 }, 3);
                    g.setColor(UniversityPixelTheme.GOLD);
                    g.fillPolygon(new int[] { ax, ax + 12, ax }, new int[] { ay - 9, ay, ay + 9 }, 3);
                } else {
                    g.setColor(new Color(12, 28, 33, 188));
                    g.fillRect(0, 0, w, h);
                    g.setColor(new Color(98, 131, 134, 150));
                    g.fillRect(0, 0, 3, h);
                }
                int textX = on ? 38 : 24;
                g.setFont(UniversityPixelTheme.font(on ? 19 : 17, Font.BOLD));
                FontMetrics title = g.getFontMetrics();
                Font small = UniversityPixelTheme.font(11, Font.PLAIN);
                FontMetrics smallMetrics = g.getFontMetrics(small);
                int blockHeight = title.getAscent() + (description == null ? 0 : smallMetrics.getAscent() + 3);
                int top = (h - blockHeight) / 2;
                g.setColor(UniversityPixelTheme.BLACK);
                g.drawString(getText(), textX + 2, top + title.getAscent() + 2);
                g.setColor(on ? UniversityPixelTheme.TEXT : UniversityPixelTheme.TEXT_SUB);
                g.drawString(getText(), textX, top + title.getAscent());
                if (description != null) {
                    g.setFont(small);
                    g.setColor(on ? UniversityPixelTheme.TEXT : UniversityPixelTheme.TEXT_SUB);
                    g.drawString(description, textX, top + title.getAscent() + 3 + smallMetrics.getAscent());
                }
                if (badge != null) {
                    g.setFont(UniversityPixelTheme.font(10, Font.BOLD));
                    FontMetrics m = g.getFontMetrics();
                    int bw = m.stringWidth(badge) + 12, bx = w - bw - 10, by = (h - 18) / 2;
                    g.setColor(UniversityPixelTheme.BLACK);
                    g.fillRect(bx + 2, by + 2, bw, 18);
                    g.setColor(badgeColor);
                    g.fillRect(bx, by, bw, 18);
                    g.setColor(UniversityPixelTheme.readableOn(badgeColor));
                    g.drawString(badge, bx + 6, by + 13);
                }
            } finally { g.dispose(); }
        }
    }
}

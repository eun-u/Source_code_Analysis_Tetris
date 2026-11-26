package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import kr.ac.jbnu.se.tetris.item.ItemIds;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** PvE 하단 아이템 버튼: 아이콘 읽기, 버튼 생성, 프레임·횟수·단축키 배지 그리기. */
final class PveItemButtons {
    // UI 리소스는 최초 한 번만 로드하고 전투 갱신 시 재사용한다.
    private static final Map<String, ImageIcon> ITEM_ICONS = loadItemIcons();

    private PveItemButtons() { }

    /** 아이템 ID에 해당하는 아이콘. 없으면 null. */
    static ImageIcon icon(String itemId) {
        return ITEM_ICONS.get(itemId);
    }

    private static Map<String, ImageIcon> loadItemIcons() {
        Map<String, ImageIcon> icons = new HashMap<>();
        icons.put(ItemIds.DAMAGE_BOOST, loadItemIcon("item_damage.png"));
        icons.put(ItemIds.GARBAGE_BOMB, loadItemIcon("item_bomb.png"));
        icons.put(ItemIds.HEAL, loadItemIcon("item_heal.png"));
        icons.put(ItemIds.SHIELD, loadItemIcon("item_shield.png"));
        icons.put(ItemIds.LINE_CLEANER, loadItemIcon("item_line_cleaner.png"));
        icons.put(ItemIds.FEVER_CHARGE, loadItemIcon("item_fever.png"));
        icons.put(ItemIds.TIME_WARP, loadItemIcon("item_time.png"));
        icons.put(ItemIds.NULLIFY, loadItemIcon("item_nullify.png"));
        return icons;
    }

    private static ImageIcon loadItemIcon(String filename) {
        String path = "/ui/campus-rpg/icons/" + filename;
        try (InputStream stream = PveItemButtons.class.getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("아이템 아이콘 파일을 찾지 못했습니다: " + path);
                return null;
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) return null;
            return new ImageIcon(image.getScaledInstance(48, 48, Image.SCALE_SMOOTH));
        } catch (IOException e) {
            System.err.println("아이템 아이콘 로딩 실패: " + path + " / " + e.getMessage());
            return null;
        }
    }

    // PvE 아이템 버튼: 중앙 아이콘, 우상단 실제 남은 횟수, 하단 1~4번 단축키
    static JButton create() {
        JButton button = new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                paintPveItemButton(this, graphics);
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setFocusable(false);
        return button;
    }

    private static void paintPveItemButton(JButton button, Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = button.getWidth(), h = button.getHeight();
            if (w < 15 || h < 15) return;
            drawPveButtonFrame(g, w, h, button.isEnabled(),
                    button.getModel().isPressed(), button.getModel().isRollover());

            // 아이콘은 실제 아이템 ID로 갱신된 기존 ImageIcon을 그대로 사용
            Icon icon = button.getIcon();
            if (icon != null) {
                int iconX = (w - icon.getIconWidth()) / 2;
                int iconY = Math.max(4, (h - icon.getIconHeight()) / 2 - 5);
                icon.paintIcon(button, g, iconX, iconY);
            }

            // 우상단: 실제 아이템 남은 사용 횟수. 0개/빈 슬롯에는 숨김
            Object countValue = button.getClientProperty("hudCharges");
            int count = countValue instanceof Number ? ((Number) countValue).intValue() : 0;
            if (icon != null && count > 0) drawChargeBadge(g, count, w);

            // 하단 중앙: 1~4번 단축키 배지(보유 횟수와 무관)
            Object hotkeyValue = button.getClientProperty("hudHotkey");
            if (hotkeyValue instanceof Number) drawHotkeyBadge(g, ((Number) hotkeyValue).intValue(), w, h);
        } finally {
            g.dispose();
        }
    }

    /** 두꺼운 둥근 어두운 프레임 + 안쪽 패널 */
    private static void drawPveButtonFrame(Graphics2D g, int w, int h,
                                           boolean active, boolean pressed, boolean hover) {
        g.setColor(new Color(0x101714));
        g.fillRoundRect(0, 0, w, h - 2, 18, 18);
        g.setColor(new Color(0x929E8B));
        g.fillRoundRect(3, 3, w - 6, h - 8, 16, 16);
        g.setColor(pressed ? new Color(0x253529)
                : hover ? new Color(0x425B4C) : new Color(0x34443C));
        g.fillRoundRect(6, 6, w - 12, h - 14, 13, 13);
        g.setColor(new Color(0x233128));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(6, 6, w - 13, h - 15, 13, 13);
        if (!active) {
            g.setColor(new Color(0, 0, 0, 48));
            g.fillRoundRect(6, 6, w - 12, h - 14, 13, 13);
        }
    }

    private static void drawChargeBadge(Graphics2D g, int count, int w) {
        int bw = 27, bh = 26, bx = w - bw - 2, by = 2;
        g.setColor(new Color(0x1B100B));
        g.fillRoundRect(bx - 2, 0, bw + 4, bh + 4, 12, 12);
        g.setColor(new Color(0xC69362));
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(new Color(0x765036));
        g.fillRoundRect(bx + 3, by + 4, bw - 6, bh - 7, 8, 8);
        drawBadgeNumber(g, String.valueOf(count), bx, by, bw, bh, 17);
    }

    private static void drawHotkeyBadge(Graphics2D g, int key, int w, int h) {
        int bw = 26, bh = 24, bx = (w - bw) / 2, by = h - bh - 1;
        g.setColor(new Color(0x102923));
        g.fillRoundRect(bx - 2, by - 2, bw + 4, bh + 4, 10, 10);
        g.setColor(new Color(0x78DCC0));
        g.fillRoundRect(bx, by, bw, bh, 8, 8);
        g.setColor(new Color(0x43B99A));
        g.fillRoundRect(bx + 3, by + 3, bw - 6, bh - 6, 6, 6);
        drawBadgeNumber(g, String.valueOf(key), bx, by, bw, bh, 16);
    }

    private static void drawBadgeNumber(Graphics2D g, String text,
                                        int x, int y, int w, int h, int size) {
        g.setFont(UniversityPixelTheme.font(size, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(new Color(0x18231E));
        g.drawString(text, tx + 1, ty + 2);
        g.setColor(new Color(0xFFF8E9));
        g.drawString(text, tx, ty);
    }
}

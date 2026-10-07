package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.character.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** 획득한 재화로 전투 캐릭터를 구입하고 선택하는 로컬 화면. */
public final class CharacterShopPanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {
    private final CharacterCatalog catalog = CharacterCatalog.loadDefault();
    private final JLabel wallet = new JLabel();
    private final PixelButton back = new PixelButton("로비로");
    private final Map<String, JPanel> cards = new LinkedHashMap<String, JPanel>();
    private final Map<String, JLabel> badges = new LinkedHashMap<String, JLabel>();
    private final Map<String, JButton> actions = new LinkedHashMap<String, JButton>();
    private Consumer<String> selection;

    public CharacterShopPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        back.secondary();
        back.setPreferredSize(new Dimension(96, 38));
        wallet.setOpaque(true);
        UniversityPixelTheme.setChip(wallet, "0 COINS", UniversityPixelTheme.GOLD);
        wallet.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        add(UniversityPixelTheme.screenHeader("CHARACTER SHOP", "캐릭터 / 상점", wallet, back), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setOpaque(false);
        for (CharacterSpec spec : catalog.all()) cards.add(card(spec));
        // 카드가 화면 높이만큼 늘어나지 않도록 위쪽에 붙여 내용 크기만 쓰게 한다.
        JPanel holder = new JPanel(new BorderLayout()); holder.setOpaque(false);
        holder.add(cards, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        add(UniversityPixelTheme.label("코인은 스토리 전투를 처음 이길 때 받습니다 · 일반 30 · 엘리트 50 · 보스 80",
                12, Font.PLAIN, UniversityPixelTheme.TEXT_SUB), BorderLayout.SOUTH);
    }

    private JPanel card(CharacterSpec spec) {
        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setBackground(UniversityPixelTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel crest = new JLabel(new ImageIcon(portrait(spec.getId())), SwingConstants.CENTER);
        crest.setOpaque(true); crest.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        crest.setPreferredSize(new Dimension(92, 92));
        crest.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.GOLD, 3));
        JPanel crestSpace = new JPanel(new GridBagLayout()); crestSpace.setOpaque(false);
        crestSpace.setPreferredSize(new Dimension(92, 100)); crestSpace.add(crest);
        card.add(crestSpace, BorderLayout.WEST);

        JPanel details = new JPanel(); details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(spec.getName());
        name.setFont(UniversityPixelTheme.font(17, Font.BOLD));
        name.setForeground(UniversityPixelTheme.TEXT);
        String extra = spec.getDamageBuff() > 0 ? "피해 +" + (int) (spec.getDamageBuff() * 100) + "%"
                : spec.getMaxHp() > 100 ? "최대 HP +" + (spec.getMaxHp() - 100)
                : spec.getItemSlots() > 3 ? "아이템 슬롯 " + spec.getItemSlots() + "칸" : "기본 능력치";
        JLabel stats = new JLabel("HP " + spec.getMaxHp() + "  ·  " + extra);
        stats.setForeground(UniversityPixelTheme.TEXT_SUB);
        stats.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        JLabel badge = UniversityPixelTheme.chip(" ", UniversityPixelTheme.PANEL_LIGHT);
        badges.put(spec.getId(), badge);
        details.add(badge); details.add(Box.createVerticalStrut(6));
        JLabel role = new JLabel(role(spec.getId()));
        role.setForeground(UniversityPixelTheme.TEXT);
        role.setFont(UniversityPixelTheme.font(12, Font.PLAIN));
        stats.setForeground(UniversityPixelTheme.GOLD);
        stats.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        details.add(name); details.add(Box.createVerticalStrut(5)); details.add(role);
        details.add(Box.createVerticalStrut(8)); details.add(stats);
        card.add(details, BorderLayout.CENTER);
        JButton action = new PixelButton("");
        action.setPreferredSize(new Dimension(130, 40));
        action.addActionListener(event -> { if (selection != null) selection.accept(spec.getId()); });
        actions.put(spec.getId(), action);
        cards.put(spec.getId(), card);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    /** 픽셀 경계를 유지해 캐릭터 원화를 상점 카드 크기로 표시한다. */
    private static BufferedImage portrait(String id) {
        String path = "/ui/characters/" + id + ".png";
        try (InputStream stream = CharacterShopPanel.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing character portrait: " + path);
            BufferedImage original = ImageIO.read(stream);
            if (original == null) throw new IllegalStateException("Unreadable character portrait: " + path);
            BufferedImage scaled = new BufferedImage(86, 86, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(original, 0, 0, 86, 86, null);
            } finally { graphics.dispose(); }
            return scaled;
        } catch (IOException failed) {
            throw new IllegalStateException("Cannot load character portrait: " + path, failed);
        }
    }

    private static String role(String id) {
        if ("attacker".equals(id)) return "줄을 지울 때 주는 피해가 커지는 공격형";
        if ("defender".equals(id)) return "체력이 높아 오래 버티는 방어형";
        if ("utility".equals(id)) return "아이템을 한 칸 더 보관하는 유틸형";
        return "균형 잡힌 기본 캐릭터 · PvP 기본형";
    }

    public void setData(int coins, Set<String> owned, String selected, Map<String, Integer> prices) {
        wallet.setText(coins + " COINS");
        for (Map.Entry<String, JButton> entry : actions.entrySet()) {
            String id = entry.getKey();
            JButton action = entry.getValue();
            PixelButton button = (PixelButton) action;
            boolean equipped = id.equals(selected), mine = owned.contains(id);
            if (equipped) { action.setText("장착 중"); action.setEnabled(false); }
            else if (mine) { action.setText("장착하기"); action.setEnabled(true); button.positive(); }
            else { int cost = prices.get(id);
                   action.setText(coins >= cost ? "구입  " + cost + " COINS" : cost + " COINS · 코인 부족");
                   action.setEnabled(coins >= cost); button.primary(); }
            UniversityPixelTheme.setChip(badges.get(id), equipped ? "장착 중" : mine ? "보유" : "미보유",
                    equipped ? UniversityPixelTheme.MINT : mine ? UniversityPixelTheme.LINE : UniversityPixelTheme.PANEL_LIGHT);
            cards.get(id).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(equipped ? UniversityPixelTheme.MINT : UniversityPixelTheme.LINE,
                            equipped ? 3 : 2), new EmptyBorder(equipped ? 9 : 10, 12, equipped ? 9 : 10, 12)));
        }
    }
    public void setSelectionAction(Consumer<String> listener) { selection = listener; }
    public void setBackAction(java.awt.event.ActionListener listener) { back.addActionListener(listener); }
}

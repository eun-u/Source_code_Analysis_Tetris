package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.character.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

/** 획득한 재화로 전투 캐릭터를 구입하고 선택하는 로컬 화면. */
public final class CharacterShopPanel extends JPanel {
    private final CharacterCatalog catalog = CharacterCatalog.loadDefault();
    private final JLabel wallet = new JLabel();
    private final JButton back = new PixelButton("로비로");
    private final Map<String, JButton> actions = new LinkedHashMap<String, JButton>();
    private Consumer<String> selection;

    public CharacterShopPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel top = new JPanel(new BorderLayout(12, 0)); top.setOpaque(false);
        JPanel heading = new JPanel(); heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CAMPUS QUEST  /  캐릭터 상점");
        title.setFont(UniversityPixelTheme.font(23, Font.BOLD));
        title.setForeground(UniversityPixelTheme.GOLD);
        JLabel subtitle = new JLabel("획득한 코인으로 전투 스타일을 선택하세요");
        subtitle.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        subtitle.setForeground(UniversityPixelTheme.TEXT_SUB);
        heading.add(title); heading.add(Box.createVerticalStrut(4)); heading.add(subtitle);
        top.add(heading, BorderLayout.WEST);
        back.setPreferredSize(new Dimension(100, 38));
        top.add(back, BorderLayout.EAST); add(top, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setBackground(UniversityPixelTheme.BG);
        for (CharacterSpec spec : catalog.all()) cards.add(card(spec));
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(null); scroll.getViewport().setBackground(UniversityPixelTheme.BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel walletPlate = new JPanel(new BorderLayout());
        walletPlate.setBackground(UniversityPixelTheme.PANEL);
        walletPlate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(8, 14, 8, 14)));
        wallet.setForeground(UniversityPixelTheme.GOLD);
        wallet.setFont(UniversityPixelTheme.font(14, Font.BOLD));
        walletPlate.add(wallet); add(walletPlate, BorderLayout.SOUTH);
    }

    private JPanel card(CharacterSpec spec) {
        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setBackground(UniversityPixelTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel crest = new JLabel(spec.getName().substring(0, 1), SwingConstants.CENTER);
        crest.setOpaque(true); crest.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        crest.setForeground(UniversityPixelTheme.GOLD);
        crest.setFont(UniversityPixelTheme.font(26, Font.BOLD));
        crest.setPreferredSize(new Dimension(70, 72));
        crest.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.GOLD, 2));
        JPanel crestSpace = new JPanel(new GridBagLayout()); crestSpace.setOpaque(false);
        crestSpace.setPreferredSize(new Dimension(70, 80)); crestSpace.add(crest);
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
        details.add(name); details.add(Box.createVerticalStrut(7)); details.add(stats);
        card.add(details, BorderLayout.CENTER);
        JButton action = new PixelButton("");
        action.setPreferredSize(new Dimension(130, 34));
        action.addActionListener(event -> { if (selection != null) selection.accept(spec.getId()); });
        actions.put(spec.getId(), action);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    public void setData(int coins, Set<String> owned, String selected, Map<String, Integer> prices) {
        wallet.setText("COINS  " + coins + "     ·     스토리 첫 승리로 획득");
        for (Map.Entry<String, JButton> entry : actions.entrySet()) {
            String id = entry.getKey();
            JButton action = entry.getValue();
            if (id.equals(selected)) { action.setText("장착 중"); action.setEnabled(false); }
            else if (owned.contains(id)) { action.setText("장착"); action.setEnabled(true); }
            else { int cost = prices.get(id); action.setText("구입  " + cost + " COINS");
                   action.setEnabled(coins >= cost); }
        }
    }
    public void setSelectionAction(Consumer<String> listener) { selection = listener; }
    public void setBackAction(java.awt.event.ActionListener listener) { back.addActionListener(listener); }
}

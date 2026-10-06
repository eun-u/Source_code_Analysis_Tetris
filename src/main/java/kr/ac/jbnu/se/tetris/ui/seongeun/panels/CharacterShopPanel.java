package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.character.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelButton;

/** 획득한 재화로 전투 캐릭터를 구입하고 선택하는 로컬 화면. */
public final class CharacterShopPanel extends JPanel {
    private final CharacterCatalog catalog = CharacterCatalog.loadDefault();
    private final JLabel wallet = new JLabel();
    private final JButton back = new PixelButton("로비로");
    private final Map<String, JButton> actions = new LinkedHashMap<String, JButton>();
    private Consumer<String> selection;

    public CharacterShopPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(13, 23, 40));
        setBorder(new EmptyBorder(22, 28, 22, 28));
        JLabel title = new JLabel("CHARACTER  /  SHOP");
        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(back, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(2, 2, 14, 14));
        cards.setOpaque(false);
        for (CharacterSpec spec : catalog.all()) {
            JPanel card = new JPanel(new BorderLayout(5, 10));
            card.setBackground(new Color(29, 47, 68));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(92, 133, 160), 2),
                    new EmptyBorder(18, 20, 18, 20)));
            JLabel name = new JLabel(spec.getName());
            name.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
            name.setForeground(new Color(244, 245, 240));
            card.add(name, BorderLayout.NORTH);
            String extra = spec.getDamageBuff() > 0 ? "피해 +" + (int) (spec.getDamageBuff() * 100) + "%"
                    : spec.getMaxHp() > 100 ? "최대 HP +" + (spec.getMaxHp() - 100)
                    : spec.getItemSlots() > 3 ? "아이템 슬롯 " + spec.getItemSlots() + "칸" : "기본형";
            JLabel stats = new JLabel("<html>HP " + spec.getMaxHp() + "<br>" + extra + "</html>");
            stats.setForeground(new Color(171, 221, 225));
            stats.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
            card.add(stats, BorderLayout.CENTER);
            JButton action = new PixelButton("");
            action.addActionListener(event -> { if (selection != null) selection.accept(spec.getId()); });
            actions.put(spec.getId(), action);
            card.add(action, BorderLayout.SOUTH);
            cards.add(card);
        }
        add(cards, BorderLayout.CENTER);
        wallet.setForeground(new Color(255, 224, 137));
        wallet.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        add(wallet, BorderLayout.SOUTH);
    }
    public void setData(int coins, Set<String> owned, String selected, Map<String, Integer> prices) {
        wallet.setText("COINS  " + coins + "    ·    스토리 첫 승리로 획득");
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

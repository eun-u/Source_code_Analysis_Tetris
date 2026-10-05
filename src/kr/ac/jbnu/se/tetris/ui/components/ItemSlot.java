package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.*;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.ui.model.ItemData;

public class ItemSlot extends JPanel {

    private JLabel keyLabel;
    private JLabel itemNameLabel;

    public ItemSlot(String key, ItemData item) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(70, 70));
        setMinimumSize(new Dimension(70, 70));
        setMaximumSize(new Dimension(70, 70));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 슬롯 사용 키
        keyLabel = new JLabel(key);

        // 슬롯에 아이템이 없는 경우
        if (item == null) {
            itemNameLabel = new JLabel("EMPTY");
        } 
        else {
            itemNameLabel = new JLabel(item.getItemName());
        }

        keyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        itemNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(keyLabel);
        add(Box.createVerticalStrut(8));
        add(itemNameLabel);
        add(Box.createVerticalGlue());
    }
}

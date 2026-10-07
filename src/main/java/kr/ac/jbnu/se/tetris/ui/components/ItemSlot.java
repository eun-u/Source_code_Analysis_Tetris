package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 성은 브랜치 ItemSlot의 70×70 키·EMPTY 슬롯 구성 */
public final class ItemSlot extends JPanel {
    public ItemSlot() {
        this("—");
    }

    public ItemSlot(String keyText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Dimension size = new Dimension(70, 70);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel key = new JLabel(keyText);
        JLabel item = new JLabel("EMPTY");
        key.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(key);
        add(Box.createVerticalStrut(8));
        add(item);
        add(Box.createVerticalGlue());
        setToolTipText("아이템 기능 준비 중");
    }
}

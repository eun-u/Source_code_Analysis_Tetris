package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 성은 브랜치 ItemSlot의 슬롯 테두리 구성 이식; 미지원 기능 표시 전용 */
public final class ItemSlot extends JPanel {
    public ItemSlot() {
        super(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(48, 30));
        JLabel label = new JLabel("—", JLabel.CENTER);
        label.setForeground(Color.GRAY);
        add(label, BorderLayout.CENTER);
        setToolTipText("아이템 기능 준비 중");
    }
}

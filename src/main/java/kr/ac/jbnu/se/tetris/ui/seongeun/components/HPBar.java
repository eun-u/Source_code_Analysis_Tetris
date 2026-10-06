package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.*;
import javax.swing.*;

public class HPBar extends JPanel {
    private JLabel hpLabel;
    private JProgressBar hpProgressBar;

    public HPBar(int currentHP, int maxHP) {
        // 컴포넌트 사이 가로 간격
        setLayout(new BorderLayout(5, 0));
        
        // HP 글자 
        hpLabel = new JLabel("HP");

        // 실제 게이지
        hpProgressBar = new JProgressBar(0, maxHP);
        // 현재 HP 삽입
        hpProgressBar.setValue(currentHP);

        // HP 숫자 표시
        hpProgressBar.setStringPainted(true);
        hpProgressBar.setString(currentHP + " /" + maxHP);
        
        add(hpLabel, BorderLayout.WEST);
        add(hpProgressBar, BorderLayout.CENTER);
    }

    // HP 변경을 위한 클래스
    public void setHP(int currentHP, int maxHP) {
        hpProgressBar.setMaximum(maxHP);
        hpProgressBar.setValue(currentHP);
        hpProgressBar.setString(currentHP + " / " + maxHP);
    } 

}

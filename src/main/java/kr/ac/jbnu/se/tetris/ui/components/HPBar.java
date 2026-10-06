package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/** 성은 브랜치 HPBar의 수치 표시 구성 이식 */
public final class HPBar extends JPanel {
    private final JProgressBar progress = new JProgressBar();

    public HPBar() {
        super(new BorderLayout(5, 0));
        add(new JLabel("HP"), BorderLayout.WEST);
        progress.setStringPainted(true);
        add(progress, BorderLayout.CENTER);
    }

    public void setHP(int current, int maximum) {
        progress.setMaximum(Math.max(1, maximum));
        progress.setValue(Math.max(0, current));
        progress.setString(current + " / " + maximum);
    }
}

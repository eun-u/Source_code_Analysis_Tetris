package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/** 성은 브랜치 FeverBar의 게이지 자리; 현재 규칙에 없는 FEVER 비활성 표시 */
public final class FeverBar extends JPanel {
    private final JProgressBar progress = new JProgressBar(0, 100);

    public FeverBar() {
        super(new BorderLayout(5, 0));
        add(new JLabel("FEVER"), BorderLayout.WEST);
        progress.setValue(0);
        progress.setStringPainted(true);
        progress.setString("미지원");
        progress.setEnabled(false);
        add(progress, BorderLayout.CENTER);
    }
}

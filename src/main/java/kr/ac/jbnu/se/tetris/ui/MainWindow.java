package kr.ac.jbnu.se.tetris.ui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** 화면 라우터를 담는 실제 창이며 닫을 때 세션/타이머 정리를 앱에 위임 */
public final class MainWindow extends JFrame {
    public MainWindow(ScreenRouter router, Runnable onClose) {
        super("Tetris");
        ScreenRouter.requireEdt();
        setContentPane(router.getContainer());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 600));
        setSize(800, 600);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                onClose.run();
                dispose();
            }
        });
    }
}

package kr.ac.jbnu.se.tetris.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import kr.ac.jbnu.se.tetris.core.GameAction;

/** 기존 키 매핑 유지 및 버튼 포커스 이동 시에도 창 단위 입력 수신 */
public final class GameKeyBindings {
    private GameKeyBindings() { }

    public static void install(JComponent component, Consumer<GameAction.Type> submit,
                               Runnable pause, Runnable home) {
        bind(component, KeyEvent.VK_LEFT, () -> submit.accept(GameAction.Type.MOVE_LEFT));
        bind(component, KeyEvent.VK_RIGHT, () -> submit.accept(GameAction.Type.MOVE_RIGHT));
        bind(component, KeyEvent.VK_UP, () -> submit.accept(GameAction.Type.ROTATE_LEFT));
        bind(component, KeyEvent.VK_DOWN, () -> submit.accept(GameAction.Type.ROTATE_RIGHT));
        bind(component, KeyEvent.VK_D, () -> submit.accept(GameAction.Type.SOFT_DROP));
        bind(component, KeyEvent.VK_SPACE, () -> submit.accept(GameAction.Type.HARD_DROP));
        bind(component, KeyEvent.VK_C, () -> submit.accept(GameAction.Type.HOLD));
        bind(component, KeyEvent.VK_P, pause);
        bind(component, KeyEvent.VK_ESCAPE, home);
    }

    private static void bind(JComponent component, int key, Runnable action) {
        String id = "game.key." + key;
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), id);
        component.getActionMap().put(id, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }
}

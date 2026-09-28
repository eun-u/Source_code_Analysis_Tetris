package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** 진행 서비스의 잠금 상태 표시와 Stage ID 기반 시작 요청 */
public final class StageSelectPanel extends JPanel implements Screen {
    private final StageCatalog catalog;
    private final Predicate<String> canEnter;
    private final JComboBox<String> choices = new JComboBox<String>();
    private final JButton begin = new JButton("선택한 Stage 시작");

    public StageSelectPanel(StageCatalog catalog, Predicate<String> canEnter,
            Consumer<String> start, Runnable home) {
        super(new BorderLayout(12, 12));
        this.catalog = catalog; this.canEnter = canEnter;
        setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        add(new JLabel("Story PvE · 일반 → 엘리트 → 보스", JLabel.CENTER), BorderLayout.NORTH);
        JPanel content = new JPanel(new GridLayout(0, 1, 8, 12));
        choices.setName("stageSelection");
        choices.addActionListener(event -> refreshButton());
        content.add(new JLabel("도전할 Stage")); content.add(choices);
        content.add(new JLabel("<html>일반·엘리트·보스를 차례로 클리어하면 다음 Stage가 열립니다.<br>"
                + "전투에서 승리하면 다음 상대에 도전할 수 있습니다.<br>"
                + "진행 기록은 현재 앱 실행 중 유지됩니다.</html>"));
        begin.setName("beginStory");
        begin.addActionListener(event -> {
            int index = choices.getSelectedIndex();
            if (index >= 0) {
                String id = catalog.getStages().get(index).getId();
                if (canEnter.test(id)) start.accept(id);
            }
        });
        content.add(begin); add(content, BorderLayout.CENTER);
        JButton back = new JButton("홈으로"); back.setName("stageHome");
        back.addActionListener(event -> home.run()); add(back, BorderLayout.SOUTH);
        refresh();
    }
    private void refreshButton() {
        int index = choices.getSelectedIndex();
        begin.setEnabled(index >= 0 && canEnter.test(catalog.getStages().get(index).getId()));
    }
    private void refresh() {
        int selected = Math.max(0, choices.getSelectedIndex());
        choices.removeAllItems();
        for (Stage stage : catalog.getStages()) {
            choices.addItem((canEnter.test(stage.getId()) ? "" : "[잠김] ") + stage.getName());
        }
        if (choices.getItemCount() > 0) choices.setSelectedIndex(Math.min(selected, choices.getItemCount() - 1));
        refreshButton();
    }
    @Override public String getId() { return "stages"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { refresh(); }
}

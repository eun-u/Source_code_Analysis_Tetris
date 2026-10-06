package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 StoryStageSelectPanel의 Stage 행과 진행 서비스의 잠금 판정 연결 */
public final class StageSelectPanel extends JPanel implements Screen {
    private final StageCatalog catalog;
    private final Predicate<String> canEnter;
    private final JComboBox<String> choices = new JComboBox<String>();
    private final JButton begin = new GameButton("선택한 Stage 시작");
    private final List<JButton> stageButtons = new ArrayList<JButton>();
    private final List<JLabel> stageStates = new ArrayList<JLabel>();

    public StageSelectPanel(StageCatalog catalog, Predicate<String> canEnter,
            Consumer<String> start, Runnable home) {
        super(new BorderLayout(12, 12));
        this.catalog = catalog;
        this.canEnter = canEnter;
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel heading = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("STORY · 대학 도전");
        title.setFont(title.getFont().deriveFont(23f));
        heading.add(title, BorderLayout.WEST);
        JButton back = new GameButton("홈으로");
        back.setName("stageHome");
        back.addActionListener(event -> home.run());
        heading.add(back, BorderLayout.EAST);
        heading.add(new JLabel("각 Stage는 일반 → 엘리트 → 보스 순서로 진행됩니다."), BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (int index = 0; index < catalog.getStages().size(); index++) {
            Stage stage = catalog.getStages().get(index);
            JPanel row = new JPanel(new BorderLayout(12, 4));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            JLabel stageName = new JLabel(stage.getName());
            stageName.setFont(stageName.getFont().deriveFont(16f));
            row.add(stageName, BorderLayout.WEST);

            JPanel encounterOrder = new JPanel(new GridLayout(1, 3, 8, 0));
            for (String tier : new String[] {"일반", "엘리트", "보스"}) {
                JLabel tierLabel = new JLabel(tier, JLabel.CENTER);
                tierLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                encounterOrder.add(tierLabel);
            }
            row.add(encounterOrder, BorderLayout.CENTER);

            JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            JLabel lock = new JLabel();
            stageStates.add(lock);
            action.add(lock);
            JButton choose = new GameButton("선택");
            choose.setName("stage-" + stage.getId());
            final int selected = index;
            choose.addActionListener(event -> choices.setSelectedIndex(selected));
            stageButtons.add(choose);
            action.add(choose);
            row.add(action, BorderLayout.EAST);
            rows.add(row);
            rows.add(Box.createVerticalStrut(8));
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel launch = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        choices.setName("stageSelection");
        choices.addActionListener(event -> refreshButton());
        launch.add(new JLabel("도전할 Stage"));
        launch.add(choices);
        begin.setName("beginStory");
        begin.setPreferredSize(new Dimension(180, 35));
        begin.addActionListener(event -> {
            int index = choices.getSelectedIndex();
            if (index >= 0) {
                String id = catalog.getStages().get(index).getId();
                if (canEnter.test(id)) start.accept(id);
            }
        });
        launch.add(begin);
        add(launch, BorderLayout.SOUTH);
        refresh();
    }

    private void refreshButton() {
        int index = choices.getSelectedIndex();
        begin.setEnabled(index >= 0 && canEnter.test(catalog.getStages().get(index).getId()));
    }

    private void refresh() {
        int selected = Math.max(0, choices.getSelectedIndex());
        choices.removeAllItems();
        for (int index = 0; index < catalog.getStages().size(); index++) {
            Stage stage = catalog.getStages().get(index);
            boolean unlocked = canEnter.test(stage.getId());
            choices.addItem((unlocked ? "" : "[잠김] ") + stage.getName());
            stageButtons.get(index).setEnabled(unlocked);
            stageStates.get(index).setText(unlocked ? "입장 가능" : "잠김");
        }
        if (choices.getItemCount() > 0) choices.setSelectedIndex(Math.min(selected, choices.getItemCount() - 1));
        refreshButton();
    }

    @Override public String getId() { return "stages"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { refresh(); }
}

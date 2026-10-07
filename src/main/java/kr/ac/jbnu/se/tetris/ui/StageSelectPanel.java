package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 과정별 실제 전투를 고유 ID로 표시한다. 같은 Elite 패턴도 독립 버튼이다. */
public final class StageSelectPanel extends JPanel implements Screen {
    private final StageCatalog catalog;
    private final BiPredicate<String, String> canEnter;
    private final Map<String, JButton> buttons = new LinkedHashMap<String, JButton>();

    /** Stage 단위 구 호출자의 시작 의미를 첫 전투에 연결하는 호환 생성자. */
    public StageSelectPanel(StageCatalog catalog, Predicate<String> canEnter,
            Consumer<String> start, Runnable home) {
        this(catalog, (id, encounterId) ->
                        catalog.getStage(id).getEncounters().get(0).getId().equals(encounterId)
                                && canEnter.test(id),
                (id, encounterId) -> start.accept(id), home);
    }

    public StageSelectPanel(StageCatalog catalog, BiPredicate<String, String> canEnter,
            BiConsumer<String, String> start, Runnable home) {
        super(new BorderLayout());
        if (catalog == null || canEnter == null || start == null || home == null)
            throw new IllegalArgumentException("Story selector requires catalog and actions");
        this.catalog = catalog;
        this.canEnter = canEnter;

        JLabel title = new JLabel("Story");
        JButton back = new GameButton("Back");
        back.setName("stageHome");
        back.addActionListener(event -> home.run());
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(10, 10, 10, 10));
        top.add(title, BorderLayout.WEST);
        top.add(back, BorderLayout.EAST);
        JPanel header = new JPanel(new BorderLayout());
        header.add(top, BorderLayout.CENTER);
        header.add(new JSeparator(), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel stageContainer = new JPanel();
        stageContainer.setLayout(new BoxLayout(stageContainer, BoxLayout.Y_AXIS));
        stageContainer.setBorder(new EmptyBorder(10, 20, 10, 20));
        int number = 1;
        for (Stage stage : catalog.getStages()) {
            if (number > 1) stageContainer.add(Box.createVerticalStrut(15));
            stageContainer.add(createStagePanel(stage, number++, start));
        }
        JScrollPane scroll = new JScrollPane(stageContainer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    private JPanel createStagePanel(Stage stage, int number,
            BiConsumer<String, String> start) {
        JLabel label = new JLabel(number + ". " + stage.getName());
        JPanel buttonPanel = new JPanel(new GridLayout(1, stage.getEncounters().size(), 12, 0));
        for (MonsterSpec monster : stage.getEncounters()) {
            String encounterId = monster.getId();
            JButton button = new GameButton(monster.getName());
            button.setName(key(stage.getId(), encounterId));
            button.addActionListener(event -> {
                if (canEnter.test(stage.getId(), encounterId))
                    start.accept(stage.getId(), encounterId);
            });
            buttonPanel.add(button);
            buttons.put(key(stage.getId(), encounterId), button);
        }
        JPanel row = new JPanel(new BorderLayout(0, 5));
        row.add(label, BorderLayout.NORTH);
        row.add(buttonPanel, BorderLayout.CENTER);
        int naturalWidth = row.getPreferredSize().width;
        row.setPreferredSize(new Dimension(naturalWidth, 162));
        row.setMinimumSize(new Dimension(0, 162));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 162));
        return row;
    }
    private static String key(String stageId, String encounterId) {
        return "stage-" + stageId + "-" + encounterId;
    }
    private static String tierName(MonsterTier tier) {
        return tier == MonsterTier.NORMAL ? "일반" : tier == MonsterTier.ELITE ? "엘리트" : "보스";
    }
    private void refresh() {
        for (Stage stage : catalog.getStages()) {
            for (MonsterSpec monster : stage.getEncounters()) {
                JButton button = buttons.get(key(stage.getId(), monster.getId()));
                boolean unlocked = canEnter.test(stage.getId(), monster.getId());
                button.setEnabled(unlocked);
                button.setText(unlocked ? monster.getName() + " · " + tierName(monster.getTier())
                        : monster.getName() + " · 잠김");
            }
        }
    }
    @Override public String getId() { return "stages"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onEnter() { refresh(); }
}

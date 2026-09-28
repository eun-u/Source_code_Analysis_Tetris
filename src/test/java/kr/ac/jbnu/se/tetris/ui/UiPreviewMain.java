package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.battle.BattleManager;
import kr.ac.jbnu.se.tetris.battle.ParticipantSpec;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.story.StageCatalog;
import kr.ac.jbnu.se.tetris.story.StoryProgressService;
import kr.ac.jbnu.se.tetris.support.SampleSnapshots;

/** 실제 서버 없이 참가자·잠금·방·결과 화면을 확인하는 테스트 전용 실행기 */
public final class UiPreviewMain {
    private UiPreviewMain() { }
    public static JPanel createPanel() {
        ScreenRouter.requireEdt();
        JPanel root = new JPanel(new BorderLayout());
        JLabel log = new JLabel("개발용 샘플 상태 · 실제 네트워크 연결 없음");
        JTabbedPane tabs = new JTabbedPane(); root.add(tabs, BorderLayout.CENTER); root.add(log, BorderLayout.SOUTH);
        StageCatalog catalog = StageCatalog.loadDefault();
        StoryProgressService progress = new StoryProgressService(catalog);
        tabs.addTab("Stage 잠금", new StageSelectPanel(catalog,
                id -> progress.getCampaignProgress().isStageUnlocked(id), id -> log.setText("시작 의도: " + id), () -> { }));
        for (int count = 2; count <= 4; count++) {
            List<ParticipantSpec> specs = new ArrayList<ParticipantSpec>();
            for (int i = 0; i < count; i++) specs.add(new ParticipantSpec("student-" + i, "학생 " + (i + 1), 100));
            BattleManager battle = new BattleManager(specs, 37); battle.start();
            BattlePanel panel = new BattlePanel(new AssetManager(), action -> log.setText("입력 의도: " + action),
                    () -> log.setText("일시정지 의도"), () -> { }, () -> { }, () -> { });
            panel.setEncounter("참가자 " + count + "명 샘플", "NORMAL");
            panel.setState(battle.getState(), "student-1", false, false);
            tabs.addTab(count + "인 표시", panel);
        }
        Map<String, Boolean> ready = new LinkedHashMap<String, Boolean>(); ready.put("student-a", false); ready.put("student-b", true);
        RoomPanel room = new RoomPanel(command -> log.setText("방 요청: " + command.getType()));
        room.setState(new RoomState("preview-room", 0, RoomState.Phase.WAITING, "student-a", ready));
        tabs.addTab("방 준비", room);
        for (boolean won : new boolean[] {true, false}) {
            ResultPanel result = new ResultPanel(() -> log.setText("재도전 의도"), () -> { });
            result.setBattleResult(SampleSnapshots.finished("student-a", "student-b", won ? "student-a" : "student-b"), "student-a");
            tabs.addTab(won ? "승리" : "패배", result);
        }
        ResultPanel failed = new ResultPanel(() -> { }, () -> { }); failed.setFailure("연결 종료 샘플 · 로비 복귀 필요");
        tabs.addTab("연결 실패", failed);
        JButton inspect = new JButton("선택 화면 확인"); inspect.addActionListener(event -> log.setText("샘플: " + tabs.getTitleAt(tabs.getSelectedIndex())));
        root.add(inspect, BorderLayout.NORTH);
        return root;
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("G0 UI 미리보기 · 개발용 fixture");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); frame.setContentPane(createPanel());
            frame.setSize(1060, 820); frame.setLocationByPlatform(true); frame.setVisible(true);
        });
    }
}

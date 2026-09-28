package kr.ac.jbnu.se.tetris.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.battle.BattleManager;
import kr.ac.jbnu.se.tetris.battle.ParticipantSpec;
import kr.ac.jbnu.se.tetris.resource.AssetManager;

/** 인원별 View 구성과 로컬 참가자 교체 및 독립 미리보기 생성 검증 */
public final class G0UiContractTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattlePanel panel = new BattlePanel(new AssetManager(), action -> { }, () -> { }, () -> { }, () -> { }, () -> { });
            for (int count = 2; count <= 4; count++) {
                List<ParticipantSpec> specs = new ArrayList<ParticipantSpec>();
                for (int i = 0; i < count; i++) specs.add(new ParticipantSpec("id-" + i, "학생 " + i, 100));
                BattleManager battle = new BattleManager(specs, 37); battle.start();
                panel.setState(battle.getState(), "id-1", false, false);
                check(panel.getParticipantViewCount() == count, "participant count");
                panel.setState(battle.getState(), "id-0", false, false);
                check(panel.getLocalParticipantId().equals("id-0"), "local participant injection");
            }
            JPanel preview = UiPreviewMain.createPanel();
            check(preview.getComponentCount() == 3, "server-free fixture screen");
        });
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}

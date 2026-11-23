package kr.ac.jbnu.se.tetris.app;

import java.awt.Component;
import java.awt.Container;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.support.FakeNetworkClient;

/** 승인되지 않은 온라인 메시지와 방 이탈 결과가 화면에 미치는 영향을 검증한다. */
public final class OnlineRoomStateFlowTest {
    private OnlineRoomStateFlowTest() { }

    public static void main(String[] args) throws Exception {
        FakeNetworkClient network = new FakeNetworkClient();
        ConnectionOptions options = new ConnectionOptions(URI.create("ws://127.0.0.1:9876/ws"), "fixture");
        try (EdtApplicationFixture fixture = EdtApplicationFixture.open(new Random(77), null)) {
            SeongeunApplication app = fixture.get();
            onEdt(() -> app.openOnline(options, network));

            RoomState waiting = room("room", 1, RoomState.Phase.WAITING);
            network.emit(NetworkUpdate.connected());
            network.emit(NetworkUpdate.roomState(waiting));
            network.drain();
            flushEdt();
            check("WAITING_ROOM".equals(app.getCurrentScreen()) && app.getRoomState() == waiting,
                    "승인한 방 상태를 대기 화면에 표시");

            network.emit(NetworkUpdate.matchStarted("room", 2, "match-a", "p"));
            network.emit(NetworkUpdate.rankedSaveStatus("room", "match-a", "SAVED"));
            network.drain();
            flushEdt();
            check(hasText(app.getScreens(), "전적이 저장되었습니다"), "수락한 경기의 저장 상태 표시");

            network.emit(NetworkUpdate.roomState(room("room", 0, RoomState.Phase.WAITING)));
            network.emit(NetworkUpdate.roomState(room("other", 3, RoomState.Phase.WAITING)));
            network.emit(NetworkUpdate.matchStarted("room", 2, "match-a", "p"));
            network.emit(NetworkUpdate.matchStarted("other", 3, "other-match", "p"));
            network.drain();
            flushEdt();
            check(app.getRoomState() == waiting && "WAITING_ROOM".equals(app.getCurrentScreen())
                    && hasText(app.getScreens(), "전적이 저장되었습니다"),
                    "오래된 방·다른 방·중복 경기 메시지가 화면을 되돌리지 않음");

            onEdt(() -> button(app.getScreens(), "방 나가기").doClick());
            check("ROOM_LIST".equals(app.getCurrentScreen()), "방 이탈 중에는 목록 표시");
            RoomState updatedWhileLeaving = room("room", 2, RoomState.Phase.WAITING);
            network.emit(NetworkUpdate.roomState(updatedWhileLeaving));
            network.drain();
            flushEdt();
            check("ROOM_LIST".equals(app.getCurrentScreen())
                    && app.getRoomState() == updatedWhileLeaving,
                    "이탈 확정 전 방 갱신은 저장하되 대기 화면은 다시 열지 않음");
            network.complete(network.getLastRequestId(), false, "LEAVE_DENIED", "room", "match-a", null);
            network.drain();
            flushEdt();
            check("WAITING_ROOM".equals(app.getCurrentScreen()) && app.getRoomState() == updatedWhileLeaving,
                    "이탈 거절 시 승인된 방으로 복귀");

            onEdt(() -> button(app.getScreens(), "방 나가기").doClick());
            network.complete(network.getLastRequestId(), true, null, "room", "match-a", null);
            network.emit(NetworkUpdate.roomState(room("room", 2, RoomState.Phase.WAITING)));
            network.drain();
            flushEdt();
            check("ROOM_LIST".equals(app.getCurrentScreen()), "이탈 수락 뒤 닫힘 통지까지 목록 유지");
            network.emit(NetworkUpdate.roomState(room("room", 3, RoomState.Phase.CLOSED)));
            network.drain();
            flushEdt();
            check("ROOM_LIST".equals(app.getCurrentScreen()) && app.getRoomState() == null,
                    "확정된 방 종료를 목록에 반영");

            RoomState rejoined = room("other", 4, RoomState.Phase.WAITING);
            network.emit(NetworkUpdate.roomState(rejoined));
            network.drain();
            flushEdt();
            check("WAITING_ROOM".equals(app.getCurrentScreen()) && app.getRoomState() == rejoined,
                    "새 방 재입장을 반영");

            onEdt(() -> button(app.getScreens(), "방 나가기").doClick());
            network.emit(NetworkUpdate.connectionFailed("CONNECTION_LOST"));
            network.emit(NetworkUpdate.roomState(room("other", 5, RoomState.Phase.WAITING)));
            network.emit(NetworkUpdate.matchStarted("other", 6, "late-match", "p"));
            network.drain();
            flushEdt();
            check("ROOM_LIST".equals(app.getCurrentScreen()) && app.getRoomState() == rejoined,
                    "연결 실패 뒤 지연 수신으로 방 화면이 다시 열리지 않음");
        }
        System.out.println("PASS OnlineRoomStateFlowTest");
    }

    private static RoomState room(String id, long version, RoomState.Phase phase) {
        Map<String, Boolean> ready = new LinkedHashMap<String, Boolean>();
        ready.put("p", false);
        ready.put("m", false);
        return new RoomState(id, version, phase, "p", ready);
    }

    private static AbstractButton button(Container parent, String text) {
        AbstractButton found = findButton(parent, text);
        if (found == null) throw new AssertionError("Button not found: " + text);
        return found;
    }

    private static AbstractButton findButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof AbstractButton && text.equals(((AbstractButton) component).getText()))
                return (AbstractButton) component;
            if (component instanceof Container) {
                AbstractButton found = findButton((Container) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasText(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel && ((JLabel) component).getText() != null
                    && ((JLabel) component).getText().contains(text)) return true;
            if (component instanceof Container && hasText((Container) component, text)) return true;
        }
        return false;
    }

    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static void flushEdt() throws Exception { onEdt(() -> { }); onEdt(() -> { }); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

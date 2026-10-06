package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 브랜치 온라인 방 화면을 로컬 서버의 실제 접속·입장·준비 상태에 연결 */
public final class OnlineLobbyPanel extends JPanel implements Screen {
    private final JTextField port = new JTextField("28080", 6);
    private final JTextField roomId = new JTextField(12);
    private final JButton connect = new GameButton("접속");
    private final JButton create = new GameButton("방 만들기");
    private final JButton join = new GameButton("입장");
    private final JLabel status = new JLabel("로컬 서버를 실행한 뒤 접속하세요.");
    private final RoomPanel room;
    private boolean connected;

    public OnlineLobbyPanel(Consumer<ConnectionOptions> connectAction,
            Consumer<RoomCommand> send, Runnable home) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel top = new JPanel(new GridLayout(4, 1, 0, 5));
        JLabel title = new JLabel("ONLINE BATTLE · 로컬 서버");
        title.setFont(title.getFont().deriveFont(22f));
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connection.add(new JLabel("서버 127.0.0.1   포트")); connection.add(port); connection.add(connect);
        JPanel rooms = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rooms.add(create); rooms.add(new JLabel("방 번호")); rooms.add(roomId); rooms.add(join);
        top.add(title); top.add(connection); top.add(rooms); top.add(status); add(top, BorderLayout.NORTH);
        room = new RoomPanel(send); add(room, BorderLayout.CENTER);
        JButton back = new GameButton("접속 종료 · 홈으로");
        back.setPreferredSize(new java.awt.Dimension(170, 35));
        back.setName("onlineHome");
        back.addActionListener(event -> home.run()); add(back, BorderLayout.SOUTH);
        port.setName("serverPort"); roomId.setName("roomId");
        status.setName("onlineStatus");
        connect.setName("connectServer"); create.setName("createRoom"); join.setName("joinRoom");
        connect.addActionListener(event -> {
            try {
                ConnectionOptions options = new ConnectionOptions("127.0.0.1", Integer.parseInt(port.getText().trim()));
                connect.setEnabled(false); setMessage("접속 중..."); connectAction.accept(options);
            } catch (IllegalArgumentException invalid) { setMessage("포트 번호를 확인하세요. (1~65535)"); connect.setEnabled(true); }
        });
        create.addActionListener(event -> send.accept(RoomCommand.createRoom(2)));
        join.addActionListener(event -> {
            String id = roomId.getText().trim();
            if (id.isEmpty()) setMessage("입장할 방 번호를 입력하세요.");
            else send.accept(RoomCommand.joinRoom(id));
        });
        reset();
    }

    public void reset() {
        ScreenRouter.requireEdt(); connected = false; room.setState(null);
        connect.setEnabled(true); port.setEnabled(true); create.setEnabled(false); join.setEnabled(false);
        setMessage("로컬 서버를 실행한 뒤 접속하세요.");
    }
    public void setConnected() {
        ScreenRouter.requireEdt(); connected = true; connect.setEnabled(false); port.setEnabled(false);
        create.setEnabled(true); join.setEnabled(true); setMessage("접속 완료 · 방을 만들거나 방 번호로 입장하세요.");
    }
    public void setConnecting() {
        ScreenRouter.requireEdt(); connect.setEnabled(false); port.setEnabled(false); setMessage("접속 중...");
    }
    public void setRoom(RoomState state) {
        ScreenRouter.requireEdt();
        boolean joined = state != null && state.getPhase() != RoomState.Phase.CLOSED;
        room.setState(joined ? state : null);
        create.setEnabled(connected && !joined); join.setEnabled(connected && !joined);
        if (joined) { roomId.setText(state.getRoomId()); setMessage("두 참가자가 모두 준비하면 대전이 시작됩니다."); }
        else setMessage("방에서 나왔습니다. 새 방을 만들거나 입장하세요.");
    }
    public void setMessage(String message) { ScreenRouter.requireEdt(); status.setText(message); }
    @Override public String getId() { return "online"; }
    @Override public JPanel getPanel() { return this; }
}

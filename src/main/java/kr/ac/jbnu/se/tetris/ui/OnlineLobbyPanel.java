package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 원본의 방 목록 배치를 실제 TCP 방 입장·대기 상태와 연결한다. */
public final class OnlineLobbyPanel extends JPanel implements Screen {
    private static final String ROOM_LIST = "room-list";
    private static final String WAITING_ROOM = "waiting-room";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JTextField port = new JTextField("28080", 6);
    private final JTextField roomId = new JTextField(12);
    private final GameButton connect = new GameButton("접속");
    private final GameButton create = new GameButton("방 만들기");
    private final GameButton join = new GameButton("입장");
    private final JLabel status = new JLabel("로컬 서버를 실행한 뒤 접속하세요.");
    private final RoomPanel room;
    private boolean connected;
    private boolean rankedMatch;
    private boolean matchFinished;
    private String rankedSaveStatus;

    public OnlineLobbyPanel(Consumer<ConnectionOptions> connectAction,
            Consumer<RoomCommand> send, Runnable home) {
        this(connectAction, send, home, () -> { }, () -> { }, false);
    }

    public OnlineLobbyPanel(Consumer<ConnectionOptions> connectAction,
            Consumer<RoomCommand> send, Runnable home, Runnable account, Runnable cloudConnect,
            boolean configured) {
        super(new BorderLayout());
        if (connectAction == null || send == null || home == null || account == null || cloudConnect == null) {
            throw new IllegalArgumentException("Actions are required");
        }

        JPanel listPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Online Battle - Room List", SwingConstants.CENTER);
        GameButton back = new GameButton("Back");
        back.setName("onlineHome");
        back.addActionListener(event -> home.run());
        GameButton ranked = new GameButton("온라인 랭킹 접속");
        ranked.setName("connectRanked");
        ranked.setEnabled(configured);
        ranked.addActionListener(event -> cloudConnect.run());
        GameButton accounts = new GameButton("계정 · 랭킹");
        accounts.setName("openAccount");
        accounts.addActionListener(event -> account.run());
        JPanel topButtonPanel = new JPanel();
        topButtonPanel.add(ranked);
        topButtonPanel.add(accounts);
        topButtonPanel.add(create);
        topButtonPanel.add(back);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(topButtonPanel, BorderLayout.EAST);
        listPanel.add(topPanel, BorderLayout.NORTH);

        JPanel headerPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        headerPanel.add(new JLabel("방 이름"));
        headerPanel.add(new JLabel("인원"));
        headerPanel.add(new JLabel("상태"));
        headerPanel.add(new JLabel("입장"));
        JPanel roomContainer = new JPanel();
        roomContainer.setLayout(new javax.swing.BoxLayout(roomContainer, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(roomContainer);
        JPanel listArea = new JPanel(new BorderLayout());
        listArea.add(headerPanel, BorderLayout.NORTH);
        listArea.add(scrollPane, BorderLayout.CENTER);
        listPanel.add(listArea, BorderLayout.CENTER);

        JLabel listNote = new JLabel("방 목록 조회 준비 중 · 방 번호로 입장할 수 있습니다.");
        listNote.setFont(listNote.getFont().deriveFont(11f));
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        connection.add(new JLabel("서버 127.0.0.1   포트"));
        connection.add(port);
        connection.add(connect);
        connection.add(new JLabel("방 번호"));
        connection.add(roomId);
        connection.add(join);
        JPanel support = new JPanel(new BorderLayout());
        support.add(listNote, BorderLayout.NORTH);
        support.add(connection, BorderLayout.CENTER);
        listPanel.add(support, BorderLayout.SOUTH);

        room = new RoomPanel(command -> {
            if (isRankedSavePending() && command.getType() == RoomCommand.Type.SET_READY) {
                setMessage("대전 결과 저장을 확인할 때까지 재대전할 수 없습니다.");
                return;
            }
            send.accept(command);
        });
        cards.add(listPanel, ROOM_LIST);
        cards.add(room, WAITING_ROOM);
        add(cards, BorderLayout.CENTER);
        status.setName("onlineStatus");
        status.setFont(status.getFont().deriveFont(11f));
        add(status, BorderLayout.SOUTH);

        port.setName("serverPort");
        roomId.setName("roomId");
        connect.setName("connectServer");
        create.setName("createRoom");
        join.setName("joinRoom");
        connect.addActionListener(event -> {
            try {
                ConnectionOptions options = new ConnectionOptions("127.0.0.1", Integer.parseInt(port.getText().trim()));
                connect.setEnabled(false);
                setMessage("접속 중...");
                connectAction.accept(options);
            } catch (IllegalArgumentException invalid) {
                setMessage("포트 번호를 확인하세요. (1~65535)");
                connect.setEnabled(true);
            }
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
        ScreenRouter.requireEdt();
        connected = false;
        rankedMatch = false;
        matchFinished = false;
        rankedSaveStatus = null;
        room.setState(null);
        room.setMatchFinished(false);
        cardLayout.show(cards, ROOM_LIST);
        connect.setEnabled(true);
        port.setEnabled(true);
        create.setEnabled(false);
        join.setEnabled(false);
        setMessage("로컬 서버를 실행한 뒤 접속하세요.");
    }

    public void setConnected() {
        ScreenRouter.requireEdt();
        connected = true;
        connect.setEnabled(false);
        port.setEnabled(false);
        create.setEnabled(true);
        join.setEnabled(true);
        setMessage("접속 완료 · 방을 만들거나 방 번호로 입장하세요.");
    }

    public void setConnecting() {
        ScreenRouter.requireEdt();
        connect.setEnabled(false);
        port.setEnabled(false);
        setMessage("접속 중...");
    }

    public void setRoom(RoomState state) {
        ScreenRouter.requireEdt();
        boolean joined = state != null && state.getPhase() != RoomState.Phase.CLOSED;
        room.setState(joined ? state : null);
        refreshReadyGate();
        create.setEnabled(connected && !joined);
        join.setEnabled(connected && !joined);
        if (joined) {
            roomId.setText(state.getRoomId());
            cardLayout.show(cards, WAITING_ROOM);
            setMessage(state.getPhase() == RoomState.Phase.WAITING
                    ? "두 참가자가 모두 준비하면 대전이 시작됩니다." : "대전 중");
        } else {
            cardLayout.show(cards, ROOM_LIST);
            setMessage("방에서 나왔습니다. 새 방을 만들거나 입장하세요.");
        }
    }

    public void setMatchFinished(boolean finished) {
        ScreenRouter.requireEdt();
        matchFinished = finished;
        refreshReadyGate();
    }

    public void setRankedMatch(boolean ranked) {
        ScreenRouter.requireEdt();
        rankedMatch = ranked;
        matchFinished = false;
        rankedSaveStatus = null;
        refreshReadyGate();
    }

    public void setRankedSaveStatus(String status) {
        ScreenRouter.requireEdt();
        rankedSaveStatus = status;
        refreshReadyGate();
    }

    private boolean isRankedSavePending() {
        return rankedMatch && matchFinished && !"SAVED".equals(rankedSaveStatus)
                && !"VOIDED".equals(rankedSaveStatus);
    }

    private void refreshReadyGate() {
        room.setMatchFinished(matchFinished && !isRankedSavePending());
        if (isRankedSavePending()) {
            JButton readyButton = findReadyButton(room);
            if (readyButton != null) readyButton.setEnabled(false);
        }
    }

    private static JButton findReadyButton(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton && "roomReady".equals(component.getName()))
                return (JButton) component;
            if (component instanceof Container) {
                JButton found = findReadyButton((Container) component);
                if (found != null) return found;
            }
        }
        return null;
    }

    public void setMessage(String message) {
        ScreenRouter.requireEdt();
        status.setText(message == null ? "" : message);
    }

    @Override public String getId() { return "online"; }
    @Override public JPanel getPanel() { return this; }
}

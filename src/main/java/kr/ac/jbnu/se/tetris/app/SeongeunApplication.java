package kr.ac.jbnu.se.tetris.app;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import kr.ac.jbnu.se.tetris.ai.AIProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.PlacementLog;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.app.session.LocalMatchSession;
import kr.ac.jbnu.se.tetris.app.session.MatchSession;
import kr.ac.jbnu.se.tetris.app.session.OnlineMatchSession;
import kr.ac.jbnu.se.tetris.app.session.SessionPhase;
import kr.ac.jbnu.se.tetris.app.session.SessionSnapshot;
import kr.ac.jbnu.se.tetris.app.session.Subscription;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.network.ConnectionOptions;
import kr.ac.jbnu.se.tetris.network.NetworkSubscription;
import kr.ac.jbnu.se.tetris.network.NetworkUpdate;
import kr.ac.jbnu.se.tetris.network.RoomCommand;
import kr.ac.jbnu.se.tetris.network.RoomState;
import kr.ac.jbnu.se.tetris.network.TcpNetworkClient;
import kr.ac.jbnu.se.tetris.story.CampaignProgress;
import kr.ac.jbnu.se.tetris.story.EncounterRun;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;
import kr.ac.jbnu.se.tetris.story.StoryProgressService;
import kr.ac.jbnu.se.tetris.ui.ScreenRouter;
import kr.ac.jbnu.se.tetris.ui.SessionUiBinding;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.RoomData;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.BattlePanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.LocalGamePanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.LocalModePanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.LoginPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.MainLobbyPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.ResultPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.RoomListPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.SignUpPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.StoryStageSelectPanel;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.WaitingRoomPanel;

/** 성은 원본 화면과 이동 버튼에 현재 게임 세션을 붙이는 실행 조립부. */
public final class SeongeunApplication implements AutoCloseable {
    private static final String LOGIN = "LOGIN";
    private static final String SIGN_UP = "SIGN_UP";
    private static final String LOBBY = "LOBBY";
    private static final String STORY_STAGE = "STORY_STAGE";
    private static final String ROOM_LIST = "ROOM_LIST";
    private static final String WAITING_ROOM = "WAITING_ROOM";
    private static final String BATTLE = "BATTLE";
    private static final String RESULT = "RESULT";
    private static final String LOCAL_MODE = "LOCAL_MODE";
    private static final String LOCAL_GAME = "LOCAL_GAME";

    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final JMenuBar menu = new JMenuBar();
    private final Random seeds;
    private final StageCatalog stages = StageCatalog.loadDefault();
    private final StoryProgressService progress = new StoryProgressService(stages);
    private final AIProfileCatalog aiProfiles = AIProfileCatalog.loadDefault();
    private final LoginPanel login = new LoginPanel();
    private final SignUpPanel signUp = new SignUpPanel();
    private final MainLobbyPanel lobby = new MainLobbyPanel();
    private final StoryStageSelectPanel storySelect = new StoryStageSelectPanel();
    private final RoomListPanel roomList = new RoomListPanel();
    private final WaitingRoomPanel waitingRoom = new WaitingRoomPanel();
    private final BattlePanel battle = new BattlePanel();
    private final ResultPanel result = new ResultPanel();
    private final LocalModePanel localMode = new LocalModePanel();
    private final LocalGamePanel localPanel = new LocalGamePanel();
    private final Timer localGravity;
    private JFrame frame;
    private String currentScreen;
    private String lastMessage;
    private LocalGameSession localGame;
    private TutorialSession tutorial;
    private MatchSession match;
    private Subscription matchSubscription;
    private TcpNetworkClient network;
    private NetworkSubscription networkSubscription;
    private RoomState roomState;
    private String requestedRoomName;
    private boolean onlineConnected;
    private boolean onlineResultReturned;
    private boolean storyBattle;
    private String displayedBattleMatchId;
    private boolean closed;

    public SeongeunApplication() { this(new Random()); }

    SeongeunApplication(Random seeds) {
        ScreenRouter.requireEdt();
        if (seeds == null) throw new IllegalArgumentException("Seed generator is required");
        this.seeds = seeds;
        for (Stage stage : stages.getStages()) {
            for (MonsterSpec monster : stage.getEncounters()) aiProfiles.get(monster.getAiProfileId());
        }
        localGravity = new Timer(400, event -> advanceGravity());
        localGravity.setCoalesce(true);
        addScreens();
        bindActions();
        buildMenu();
        show(LOGIN);
    }

    private void addScreens() {
        screens.add(login, LOGIN);
        screens.add(signUp, SIGN_UP);
        screens.add(lobby, LOBBY);
        screens.add(storySelect, STORY_STAGE);
        screens.add(roomList, ROOM_LIST);
        screens.add(waitingRoom, WAITING_ROOM);
        screens.add(battle, BATTLE);
        screens.add(result, RESULT);
        screens.add(localMode, LOCAL_MODE);
        screens.add(localPanel, LOCAL_GAME);
    }

    private void bindActions() {
        // 원본의 로그인 버튼은 계정 확인 기능이 아니라 로컬 로비 진입이었다.
        login.setLoginAction(event -> { clearSecrets(login); show(LOBBY); });
        login.setSignUpAction(event -> show(SIGN_UP));
        signUp.setBackAction(event -> { clearSecrets(signUp); show(LOGIN); });
        signUp.setRegisterAction(event -> {
            clearSecrets(signUp);
            message("회원가입 서비스는 아직 연결되지 않았습니다.");
            show(LOGIN);
        });
        lobby.setStoryAction(event -> showStories());
        lobby.setOnlineBattleAction(event -> openOnline());
        lobby.setLocalModeAction(event -> show(LOCAL_MODE));
        storySelect.setBackAction(event -> show(LOBBY));
        storySelect.setStage1NormalAction(event -> startStory(0, MonsterTier.NORMAL));
        storySelect.setStage1EliteAction(event -> startStory(0, MonsterTier.ELITE));
        storySelect.setStage1BossAction(event -> startStory(0, MonsterTier.BOSS));
        storySelect.setStage2NormalAction(event -> startStory(1, MonsterTier.NORMAL));
        storySelect.setStage2EliteAction(event -> startStory(1, MonsterTier.ELITE));
        storySelect.setStage2BossAction(event -> startStory(1, MonsterTier.BOSS));
        storySelect.setStage3NormalAction(event -> startStory(2, MonsterTier.NORMAL));
        storySelect.setStage3EliteAction(event -> startStory(2, MonsterTier.ELITE));
        storySelect.setStage3BossAction(event -> startStory(2, MonsterTier.BOSS));
        roomList.setBackAction(event -> { closeSession(); show(LOBBY); });
        roomList.setRooms(Collections.<RoomData>emptyList(), room -> joinRoom(room.getRoomName()));
        roomList.setCreateRoomAction(event -> createRoom());
        waitingRoom.setBackAction(event -> leaveRoom());
        waitingRoom.setReadyAction(event -> toggleReady());
        battle.setResultAction(event -> {
            if (match != null && match.getSnapshot().getPhase() == SessionPhase.FINISHED) renderMatch();
            else message("전투가 끝나면 실제 결과가 자동으로 표시됩니다.");
        });
        result.setReturnAction(event -> returnFromResult());
        result.setLobbyAction(event -> { closeSession(); show(LOBBY); });
        localMode.setBackAction(event -> show(LOBBY));
        localMode.setInfiniteAction(event -> startLocal(LocalGameSession.Mode.INFINITE));
        localMode.setSprintAction(event -> startLocal(LocalGameSession.Mode.SPRINT));
        localPanel.setBackAction(event -> { closeSession(); show(LOCAL_MODE); });
        localPanel.getPlayerBoard().setInputHandlers(this::submit, this::togglePause);
        battle.getPlayerBoard().setInputHandlers(this::submit, this::togglePause);
    }

    private void buildMenu() {
        // 원본 화면에 없던 기능은 패널 좌표와 기존 버튼을 바꾸지 않고 메뉴로 연다.
        JMenu functions = new JMenu("기능");
        addMenuItem(functions, "튜토리얼", () -> startTutorial());
        addMenuItem(functions, "HOLD", () -> submit(GameAction.Type.HOLD));
        addMenuItem(functions, "현재 게임 안내", this::showGameInformation);
        addMenuItem(functions, "일시정지 / 계속", this::togglePause);
        addMenuItem(functions, "대전 포기", this::forfeit);
        JMenu extraStages = new JMenu("추가 스테이지");
        for (int index = 3; index < stages.getStages().size(); index++) {
            final int stageIndex = index;
            JMenu stageMenu = new JMenu("Stage " + (index + 1));
            for (MonsterTier tier : MonsterTier.values()) {
                final MonsterTier selectedTier = tier;
                addMenuItem(stageMenu, tierName(tier), () -> startStory(stageIndex, selectedTier));
            }
            extraStages.add(stageMenu);
        }
        if (extraStages.getItemCount() > 0) functions.add(extraStages);
        menu.add(functions);
        JMenu online = new JMenu("온라인");
        addMenuItem(online, "서버 연결...", this::promptConnection);
        addMenuItem(online, "방 번호로 입장...", this::promptJoin);
        addMenuItem(online, "방 번호 보기", this::showRoomId);
        menu.add(online);
    }

    private static String tierName(MonsterTier tier) {
        switch (tier) {
            case NORMAL: return "일반";
            case ELITE: return "엘리트";
            default: return "보스";
        }
    }

    private void addMenuItem(JMenu parent, String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(event -> action.run());
        parent.add(item);
    }

    private void show(String id) {
        ScreenRouter.requireEdt();
        if (closed) return;
        cards.show(screens, id);
        currentScreen = id;
        if (LOCAL_GAME.equals(id)) SwingUtilities.invokeLater(() -> localPanel.getPlayerBoard().requestFocusInWindow());
        if (BATTLE.equals(id)) SwingUtilities.invokeLater(() -> battle.getPlayerBoard().requestFocusInWindow());
    }

    private void showStories() {
        storySelect.updateProgress(progress.getCampaignProgress(), stages);
        show(STORY_STAGE);
    }

    private void startStory(int stageIndex, MonsterTier tier) {
        ScreenRouter.requireEdt();
        if (closed || stageIndex >= stages.getStages().size()) return;
        String stageId = stages.getStages().get(stageIndex).getId();
        CampaignProgress campaign = progress.getCampaignProgress();
        if (!campaign.isEncounterUnlocked(stageId, tier)) {
            message("아직 잠긴 전투입니다.");
            return;
        }
        closeSession();
        EncounterRun run = progress.startEncounter(stageId, tier, newRunId(), "local", "monster");
        MonsterSpec spec = run.getMonster();
        PlayerProfile profile = new PlayerProfile();
        PlacementLog placements = new PlacementLog();
        MonsterSession engine = new MonsterSession(seeds.nextLong(), spec.getName(), spec.getHp(),
                aiProfiles.get(spec.getAiProfileId()).getDelayMillis(), MonsterStrategies.create(spec),
                profile, placements, run.getRunId(), run.getLocalParticipantId(),
                run.getMonsterParticipantId(), System::nanoTime);
        storyBattle = true;
        installMatch(new LocalMatchSession(engine));
        result.setReturnButtonText("Story로");
        show(BATTLE);
        ((LocalMatchSession) match).startClock();
        renderMatch();
    }

    private void startLocal(LocalGameSession.Mode mode) {
        ScreenRouter.requireEdt();
        closeSession();
        localGame = new LocalGameSession(mode, seeds.nextLong());
        localPanel.startMode(mode == LocalGameSession.Mode.INFINITE ? "Infinite" : "Sprint");
        show(LOCAL_GAME);
        renderLocal();
    }

    private void startTutorial() {
        ScreenRouter.requireEdt();
        closeSession();
        tutorial = new TutorialSession(seeds.nextLong());
        localPanel.startMode("Tutorial");
        show(LOCAL_GAME);
        renderLocal();
        message(tutorial.getInstruction());
    }

    void submit(GameAction.Type action) {
        ScreenRouter.requireEdt();
        if (closed || (!LOCAL_GAME.equals(currentScreen) && !BATTLE.equals(currentScreen))) return;
        if (localGame != null) { localGame.submit(action); renderLocal(); }
        else if (tutorial != null) { tutorial.submit(action); renderLocal(); }
        else if (match != null && match.getSnapshot().getCapabilities().canSubmit()) {
            match.submit(new PlayerIntent(action));
        }
    }

    private void advanceGravity() {
        ScreenRouter.requireEdt();
        if (closed || !LOCAL_GAME.equals(currentScreen)) return;
        if (localGame != null && !localGame.isPaused()) { localGame.tick(); renderLocal(); }
        else if (tutorial != null && !tutorial.isPaused()) { tutorial.tick(); renderLocal(); }
    }

    private void renderLocal() {
        GameState state = localGame != null ? localGame.getPlayerState()
                : tutorial != null ? tutorial.getPlayerState() : null;
        if (state == null) return;
        localPanel.setState(state);
        boolean finished = localGame != null ? localGame.isFinished() : tutorial.isFinished();
        if (finished) {
            localGravity.stop();
            if (localGame != null) {
                if (localGame.isCompleted()) localPanel.setStatusText("COMPLETE");
                return;
            }
            result.setResultDetails(tutorial.isCompleted() ? "COMPLETE" : "GAME OVER", "Player",
                    Integer.toString(state.getLinesCleared()), "—", "—", "—");
            result.setReturnButtonText("로컬 모드로");
            show(RESULT);
        } else if (state.getStatus() == GameState.Status.PAUSED) localGravity.stop();
        else localGravity.start();
    }

    private void installMatch(MatchSession session) {
        match = session;
        matchSubscription = SessionUiBinding.bind(session, update -> {
            if (closed || session != match) return;
            if (storyBattle && session instanceof LocalMatchSession) {
                LocalMatchSession local = (LocalMatchSession) session;
                progress.recordBattleResult(update.getSnapshot().getMatchId(), local.getLastBattleResult());
            }
            renderMatch();
        });
    }

    private void renderMatch() {
        if (match == null) return;
        SessionSnapshot snapshot = match.getSnapshot();
        if (snapshot.getPhase() == SessionPhase.FAILED) {
            if (match instanceof OnlineMatchSession && displayedBattleMatchId == null) {
                show(ROOM_LIST);
                return;
            }
            result.setResultDetails("CONNECTION LOST", "Player", "—", "—", "—", "—");
            result.setReturnButtonText("대기방으로");
            show(RESULT);
            return;
        }
        BattleState state = snapshot.getBattleState();
        if (state == null) return;
        if (!snapshot.getMatchId().equals(displayedBattleMatchId)) {
            String localId = snapshot.getLocalParticipantId();
            String opponentId = null;
            for (String id : state.getParticipants().keySet()) {
                if (!id.equals(localId)) { opponentId = id; break; }
            }
            if (opponentId != null) {
                battle.setPlayers(new PlayerData(state.getParticipant(localId).getName(), 0, "—", false),
                        new PlayerData(state.getParticipant(opponentId).getName(), 0, "—", false));
                battle.setItems(new ItemData[0], new ItemData[0]);
            }
            displayedBattleMatchId = snapshot.getMatchId();
        }
        battle.setState(state, snapshot.getLocalParticipantId());
        if (snapshot.getPhase() == SessionPhase.FINISHED) {
            if (match instanceof OnlineMatchSession && onlineResultReturned) {
                show(WAITING_ROOM);
                return;
            }
            boolean won = snapshot.getLocalParticipantId().equals(state.getWinnerId());
            int lines = state.getParticipant(snapshot.getLocalParticipantId()).getGameState().getLinesCleared();
            String outcome = state.getWinnerId() == null ? "DRAW" : won ? "VICTORY" : "DEFEAT";
            result.setResultDetails(outcome, "Player", Integer.toString(lines),
                    "—", "—", "—");
            result.setReturnButtonText(storyBattle ? "Story로" : "대기방으로");
            show(RESULT);
        } else if (match instanceof OnlineMatchSession) show(BATTLE);
    }

    private void togglePause() {
        ScreenRouter.requireEdt();
        if (localGame != null && LOCAL_GAME.equals(currentScreen)) {
            if (localGame.isPaused()) localGame.resume(); else localGame.pause();
            renderLocal();
        } else if (tutorial != null && LOCAL_GAME.equals(currentScreen)) {
            if (tutorial.isPaused()) tutorial.resume(); else tutorial.pause();
            renderLocal();
        } else if (match != null && BATTLE.equals(currentScreen)
                && match.getSnapshot().getCapabilities().canPause()) {
            match.requestPause(match.getSnapshot().getPhase() != SessionPhase.PAUSED);
        }
    }

    private void forfeit() {
        ScreenRouter.requireEdt();
        if (match != null && BATTLE.equals(currentScreen) && match.getSnapshot().getCapabilities().canLeave()) {
            match.leave();
        }
    }

    private void showGameInformation() {
        if (tutorial != null) message(tutorial.getInstruction());
        else if (localGame != null) message(localGame.getInstruction());
        else if (match != null && match.getSnapshot().getBattleState() != null) {
            BattleState state = match.getSnapshot().getBattleState();
            String localId = match.getSnapshot().getLocalParticipantId();
            GameState local = state.getParticipant(localId).getGameState();
            message("지운 줄: " + local.getLinesCleared() + " · HOLD: " + local.getHoldPiece()
                    + " · 다음: " + local.getNextPieces());
        } else message("진행 중인 게임이 없습니다.");
    }

    private void returnFromResult() {
        if (storyBattle) {
            closeSession();
            showStories();
        } else if (match instanceof OnlineMatchSession) {
            if (match.getSnapshot().getPhase() == SessionPhase.FAILED) {
                closeSession();
                show(ROOM_LIST);
                return;
            }
            onlineResultReturned = true;
            if (roomState != null) show(WAITING_ROOM);
            else show(ROOM_LIST);
            sendRoomCommand(RoomCommand.getRoomState());
        } else {
            closeSession();
            show(LOCAL_MODE);
        }
    }

    private void openOnline() {
        openOnline(new ConnectionOptions("127.0.0.1", 28080));
    }

    void openOnline(ConnectionOptions options) {
        closeSession();
        roomList.setRooms(Collections.<RoomData>emptyList(), room -> joinRoom(room.getRoomName()));
        show(ROOM_LIST);
        connectOnline(options);
    }

    private void promptConnection() {
        String address = JOptionPane.showInputDialog(frame, "서버 주소 (host:port)", "127.0.0.1:28080");
        if (address == null) return;
        int separator = address.lastIndexOf(':');
        try {
            if (separator <= 0) throw new IllegalArgumentException("Missing port");
            ConnectionOptions options = new ConnectionOptions(address.substring(0, separator).trim(),
                    Integer.parseInt(address.substring(separator + 1).trim()));
            closeSession();
            show(ROOM_LIST);
            connectOnline(options);
        } catch (IllegalArgumentException invalid) {
            message("주소를 host:port 형식으로 입력하세요.");
        }
    }

    void connectOnline(ConnectionOptions options) {
        ScreenRouter.requireEdt();
        if (closed) return;
        if (match != null || network != null) closeSession();
        final TcpNetworkClient client = new TcpNetworkClient();
        network = client;
        onlineConnected = false;
        onlineResultReturned = false;
        roomState = null;
        installMatch(new OnlineMatchSession(newRunId(), client));
        networkSubscription = client.subscribe(update -> SwingUtilities.invokeLater(() -> {
            if (closed || network != client) return;
            onNetworkUpdate(update);
        }));
        client.connect(options);
    }

    private void onNetworkUpdate(NetworkUpdate update) {
        switch (update.getType()) {
            case CONNECTED:
                onlineConnected = true;
                break;
            case ROOM_STATE:
                roomState = update.getRoomState();
                if (roomState.getPhase() == RoomState.Phase.CLOSED) {
                    roomState = null;
                    requestedRoomName = null;
                    show(ROOM_LIST);
                } else {
                    renderRoom();
                    if (roomState.getPhase() == RoomState.Phase.WAITING
                            && (WAITING_ROOM.equals(currentScreen) || ROOM_LIST.equals(currentScreen))) {
                        show(WAITING_ROOM);
                    }
                }
                break;
            case MATCH_STARTED:
                onlineResultReturned = false;
                break;
            case CONNECTION_FAILED:
            case ERROR:
            case CLOSED:
                onlineConnected = false;
                message("서버 연결을 확인하세요: " + update.getReasonCode());
                break;
            case REQUEST_OUTCOME:
                if (!update.getRequestOutcome().isAccepted()) {
                    message("요청 거절: " + update.getRequestOutcome().getReasonCode());
                }
                break;
            default: break;
        }
    }

    private void renderRoom() {
        if (roomState == null) return;
        Map<String, Boolean> ready = roomState.getReadyByParticipantId();
        String localId = roomState.getLocalParticipantId();
        PlayerData local = new PlayerData(localId, 1, "Basic Character", ready.get(localId));
        PlayerData opponent = null;
        for (Map.Entry<String, Boolean> entry : ready.entrySet()) {
            if (!entry.getKey().equals(localId)) {
                opponent = new PlayerData(entry.getKey(), 1, "Basic Character", entry.getValue());
                break;
            }
        }
        waitingRoom.setRoom(new RoomData(requestedRoomName == null ? roomState.getRoomId() : requestedRoomName,
                ready.size(), 2,
                roomState.getPhase() == RoomState.Phase.WAITING ? "Waiting" : "Playing"));
        waitingRoom.setPlayers(local, opponent);
    }

    private void createRoom() {
        String name = JOptionPane.showInputDialog(frame, "방 이름을 입력하세요.",
                "방 만들기", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        createRoomWithName(name);
    }

    void createRoomWithName(String name) {
        ScreenRouter.requireEdt();
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) { message("방 이름을 입력해주세요."); return; }
        if (!onlineConnected || network == null) { message("서버에 먼저 연결하세요."); return; }
        // 서버가 방 이름을 저장하지 않으므로 이 이름은 이 창의 제목에만 사용한다.
        requestedRoomName = name;
        sendRoomCommand(RoomCommand.createRoom(2));
    }

    private void promptJoin() {
        String roomId = JOptionPane.showInputDialog(frame, "입장할 방 번호를 입력하세요.");
        if (roomId != null) joinRoom(roomId);
    }

    void joinRoom(String roomId) {
        if (!onlineConnected || network == null) { message("서버에 먼저 연결하세요."); return; }
        if (roomId.trim().isEmpty()) { message("방 번호를 입력하세요."); return; }
        requestedRoomName = null;
        sendRoomCommand(RoomCommand.joinRoom(roomId.trim()));
    }

    private void showRoomId() {
        message(roomState == null ? "아직 참가한 방이 없습니다." : "방 번호: " + roomState.getRoomId());
    }

    private void toggleReady() {
        if (roomState == null || network == null) return;
        if (roomState.getPhase() != RoomState.Phase.WAITING
                && (match == null || match.getSnapshot().getPhase() != SessionPhase.FINISHED)) return;
        boolean ready = roomState.getReadyByParticipantId().get(roomState.getLocalParticipantId());
        sendRoomCommand(RoomCommand.setReady(!ready));
    }

    private void leaveRoom() {
        if (roomState != null && network != null) sendRoomCommand(RoomCommand.leaveRoom());
        roomState = null;
        requestedRoomName = null;
        show(ROOM_LIST);
    }

    private void sendRoomCommand(RoomCommand command) {
        if (network == null) return;
        try { network.send(command); }
        catch (IllegalStateException error) { message("서버 접속 상태를 확인하세요."); }
    }

    private void message(String text) {
        lastMessage = text;
        if (frame != null) JOptionPane.showMessageDialog(frame, text);
    }

    private static void clearSecrets(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JPasswordField) ((JPasswordField) component).setText("");
            else if (component instanceof Container) clearSecrets((Container) component);
        }
    }

    private String newRunId() { return UUID.randomUUID().toString(); }

    private void closeSession() {
        localGravity.stop();
        if (matchSubscription != null) { matchSubscription.close(); matchSubscription = null; }
        if (networkSubscription != null) { networkSubscription.close(); networkSubscription = null; }
        if (match != null) { match.close(); match = null; }
        if (network != null) { network.close(); network = null; }
        if (localGame != null) { localGame.close(); localGame = null; }
        if (tutorial != null) { tutorial.close(); tutorial = null; }
        roomState = null;
        requestedRoomName = null;
        onlineConnected = false;
        onlineResultReturned = false;
        storyBattle = false;
        displayedBattleMatchId = null;
    }

    public void openWindow() {
        ScreenRouter.requireEdt();
        if (frame != null) throw new IllegalStateException("Window is already open");
        frame = new JFrame("Tetris UI Preview");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setJMenuBar(menu);
        frame.add(screens);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { close(); }
        });
        frame.setVisible(true);
    }

    @Override public void close() {
        ScreenRouter.requireEdt();
        if (closed) return;
        closeSession();
        clearSecrets(login);
        clearSecrets(signUp);
        closed = true;
        if (frame != null && frame.isDisplayable()) frame.dispose();
    }

    String getCurrentScreen() { return currentScreen; }
    String getLastMessage() { return lastMessage; }
    GameState getLocalState() { return localGame != null ? localGame.getPlayerState()
            : tutorial != null ? tutorial.getPlayerState() : null; }
    SessionSnapshot getMatchSnapshot() { return match == null ? null : match.getSnapshot(); }
    RoomState getRoomState() { return roomState; }
    boolean isOnlineConnected() { return onlineConnected; }
    boolean isLocalGravityRunning() { return localGravity.isRunning(); }
    boolean isStoryClockRunning() { return match instanceof LocalMatchSession
            && ((LocalMatchSession) match).isGravityRunning(); }
    CampaignProgress getCampaignProgress() { return progress.getCampaignProgress(); }
    JPanel getScreens() { return screens; }
    JMenuBar getMenu() { return menu; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SeongeunApplication().openWindow());
    }
}

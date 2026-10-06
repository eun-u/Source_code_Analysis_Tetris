package kr.ac.jbnu.se.tetris.app;

import java.util.Random;
import java.util.UUID;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import kr.ac.jbnu.se.tetris.ai.AIProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.DifficultyProfileCatalog;
import kr.ac.jbnu.se.tetris.app.session.*;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.character.CharacterSpec;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PlayerIntent;
import kr.ac.jbnu.se.tetris.resource.AssetManager;
import kr.ac.jbnu.se.tetris.network.*;
import kr.ac.jbnu.se.tetris.story.*;
import kr.ac.jbnu.se.tetris.ui.*;

/** 화면 조립·진행 서비스·대전 세션의 연결 및 모드별 실행 책임 위임 */
public final class TetrisApplication {
    private final ScreenRouter router = new ScreenRouter();
    private final ModeManager modes = new ModeManager();
    private final Random seeds;
    private final StageCatalog stages = StageCatalog.loadDefault();
    private final StoryProgressService progress = new StoryProgressService(stages);
    private final AIProfileCatalog aiProfiles = AIProfileCatalog.loadDefault();
    private final HomePanel home;
    private final LoginPanel login;
    private final SignUpPanel signUp;
    private final LocalModePanel localModes;
    private final GamePanel game;
    private final BattlePanel battlePanel;
    private final ResultPanel result;
    private final OnlineLobbyPanel onlineLobby;
    private final Timer tutorialGravity;
    private TutorialSession tutorial;
    private LocalGameSession localGame;
    private Subscription matchSubscription;
    private TcpNetworkClient onlineClient;
    private NetworkSubscription onlineSubscription;
    private boolean rematchRequested;
    private boolean onlineResultReturned;
    private long rematchRequestId;
    private boolean storyActive;
    private boolean lastGameWasBattle;
    private boolean closed;

    public TetrisApplication() {
        this(new Random());
    }
    /** 결정적 스토리 회귀 검증을 위한 시드 생성기 주입 */
    TetrisApplication(Random seeds) {
        ScreenRouter.requireEdt();
        if (seeds == null) throw new IllegalArgumentException("Seed generator is required");
        this.seeds = seeds;
        for (Stage stage : stages.getStages()) for (MonsterSpec monster : stage.getEncounters()) {
            aiProfiles.get(monster.getAiProfileId());
        }
        tutorialGravity = new Timer(400, event -> advanceGravity());
        tutorialGravity.setCoalesce(true);
        AssetManager assets = new AssetManager();
        login = new LoginPanel(this::enterLocalLobby,
                () -> router.show("signup"));
        signUp = new SignUpPanel(this::requestRegistration, this::returnToLogin);
        localModes = new LocalModePanel(() -> startLocalGame(LocalGameSession.Mode.INFINITE),
                () -> startLocalGame(LocalGameSession.Mode.SPRINT), this::showHome);
        localModes.setTutorialAction(this::startNewGame);
        home = new HomePanel(assets, this::showLocalModes, this::showStages, this::showOnline, this::continueGame, this::refreshHome);
        onlineLobby = new OnlineLobbyPanel(this::connectOnline, this::sendRoomCommand, this::showHome);
        game = new GamePanel(this::submit, this::togglePause, this::returnFromGame, this::enterGame, this::leaveGame);
        battlePanel = new BattlePanel(assets, this::submit, this::togglePause, this::leaveToLobby,
                this::enterGame, this::leaveGame);
        result = new ResultPanel(this::returnFromResult, this::leaveToLobby, this::nextEncounter);
        router.register(login); router.register(signUp); router.register(localModes);
        router.register(home); router.register(game); router.register(battlePanel); router.register(result);
        router.register(onlineLobby);
        router.register(new StageSelectPanel(stages,
                (id, tier) -> progress.getCampaignProgress().isEncounterUnlocked(id, tier),
                this::startStory, this::leaveToLobby));
        router.show("login");
    }
    private void enterLocalLobby() { login.clearSecrets(); router.show("home"); }
    private void requestRegistration() {
        signUp.clearSecrets(); signUp.setMessage("회원가입 서비스 연결 준비 중입니다.");
    }
    private void returnToLogin() { signUp.clearSecrets(); router.show("login"); }
    public void startNewGame() { startNewGame(seeds.nextLong()); }
    public void startBattle() { startBattle(seeds.nextLong()); }
    public void startNewGame(long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false; lastGameWasBattle = false;
        tutorial = new TutorialSession(seed); game.setMode("Tutorial"); router.show("game"); render();
    }
    public void showLocalModes() { ScreenRouter.requireEdt(); if (!closed) router.show("local-mode"); }
    public void startLocalGame(LocalGameSession.Mode mode) { startLocalGame(mode, seeds.nextLong()); }
    public void startLocalGame(LocalGameSession.Mode mode, long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        if (mode == null) throw new IllegalArgumentException("Local mode is required");
        prepareSessionChange(); storyActive = false; lastGameWasBattle = false;
        localGame = new LocalGameSession(mode, seed);
        game.setMode(mode == LocalGameSession.Mode.INFINITE ? "Infinite" : "Sprint");
        router.show("game"); render();
    }
    private void returnFromGame() {
        if (closed) return;
        prepareSessionChange(); storyActive = false; router.show("local-mode");
    }
    private void leaveToLobby() {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false;
    }
    private void returnFromResult() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (modes.getCurrent() instanceof OnlineMatchSession) {
            if (modes.getCurrent().getSnapshot().getPhase() == SessionPhase.FAILED) { showOnline(); return; }
            onlineResultReturned = true;
            onlineLobby.setMatchFinished(true);
            router.show("online");
            sendRoomCommand(RoomCommand.getRoomState());
        } else if (storyActive) {
            prepareSessionChange(); storyActive = false; showStages();
        } else if (localGame != null) {
            prepareSessionChange(); showLocalModes();
        } else if (tutorial != null) {
            prepareSessionChange(); showLocalModes();
        } else leaveToLobby();
    }
    /** 개발용 기본 전투 시작 및 제품 홈의 Story 진입과 분리 */
    public void startBattle(long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false;
        battlePanel.setEncounter("기본 휴리스틱 대전", "NORMAL");
        installMatch(new LocalMatchSession(new MonsterSession(seed))); router.show("battle");
    }
    public void showStages() { ScreenRouter.requireEdt(); if (!closed) router.show("stages"); }
    public void showOnline() {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false; onlineLobby.reset(); router.show("online");
    }
    public void connectOnline(ConnectionOptions options) {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false; rematchRequested = false; rematchRequestId = 0; onlineResultReturned = false;
        onlineLobby.reset(); onlineLobby.setConnecting();
        TcpNetworkClient client = new TcpNetworkClient(); onlineClient = client;
        installMatch(new OnlineMatchSession(newRunId(), client));
        onlineSubscription = client.subscribe(update -> SwingUtilities.invokeLater(() -> {
            if (closed || onlineClient != client) return;
            switch (update.getType()) {
                case CONNECTED: onlineLobby.setConnected(); break;
                case ROOM_STATE:
                    onlineLobby.setRoom(update.getRoomState());
                    if (update.getRoomState().getPhase() == RoomState.Phase.WAITING) {
                        rematchRequested = false; rematchRequestId = 0;
                    }
                    break;
                case MATCH_STARTED:
                    rematchRequested = false; rematchRequestId = 0; onlineResultReturned = false;
                    onlineLobby.setMatchFinished(false); break;
                case REQUEST_OUTCOME:
                    if (!update.getRequestOutcome().isAccepted()) {
                        if (update.getRequestOutcome().getRequestId() == rematchRequestId) {
                            rematchRequested = false; rematchRequestId = 0;
                        }
                        onlineLobby.setMessage("요청 거절: " + update.getRequestOutcome().getReasonCode());
                        render();
                    }
                    break;
                default: break;
            }
        }));
        router.show("online"); client.connect(options);
    }
    public void sendRoomCommand(RoomCommand command) {
        ScreenRouter.requireEdt(); if (closed || onlineClient == null) return;
        try { onlineClient.send(command); }
        catch (IllegalStateException error) { onlineLobby.setMessage("접속 상태를 확인하고 다시 시도하세요."); }
    }
    public void startStory(String stageId) { startStory(stageId, seeds.nextLong()); }
    public void startStory(String stageId, long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        EncounterRun run = progress.startStage(stageId, newRunId(), "local", "monster");
        prepareSessionChange(); storyActive = true;
        startStoryEncounter(run, seed);
    }
    public void startStory(int stageIndex, long seed) {
        if (stageIndex < 0 || stageIndex >= stages.getStages().size()) throw new IllegalArgumentException("Unknown stage");
        startStory(stages.getStages().get(stageIndex).getId(), seed);
    }
    public void startStory(String stageId, MonsterTier tier) { startStory(stageId, tier, seeds.nextLong()); }
    public void startStory(String stageId, MonsterTier tier, long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        EncounterRun run = progress.startEncounter(stageId, tier, newRunId(), "local", "monster");
        prepareSessionChange(); storyActive = true;
        startStoryEncounter(run, seed);
    }
    public void startStory(String stageId, String encounterId) {
        startStory(stageId, encounterId, seeds.nextLong());
    }
    public void startStory(String stageId, String encounterId, long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        EncounterRun run = progress.startEncounter(stageId, encounterId, newRunId(),
                "local", "monster");
        prepareSessionChange(); storyActive = true;
        startStoryEncounter(run, seed);
    }
    private void startStoryEncounter(EncounterRun run, long seed) {
        MonsterSpec spec = run.getMonster();
        MonsterSession engine = new MonsterSession(seed, spec.getName(),
                DifficultyProfileCatalog.forEncounter(spec), CharacterSpec.DEFAULT,
                run.getRunId(), run.getLocalParticipantId(), run.getMonsterParticipantId());
        battlePanel.setEncounter(stages.getStage(run.getStageId()).getName(), spec.getTier().name());
        installMatch(new LocalMatchSession(engine)); router.show("battle");
    }
    private void installMatch(MatchSession session) {
        modes.install(session); lastGameWasBattle = true;
        long generation = modes.getGeneration();
        matchSubscription = SessionUiBinding.bind(session, update -> {
            if (closed || generation != modes.getGeneration()) return;
            if (storyActive && session instanceof LocalMatchSession) {
                LocalMatchSession local = (LocalMatchSession) session;
                progress.recordBattleResult(update.getSnapshot().getMatchId(), local.getLastBattleResult());
            }
            render();
        });
    }
    private void restart() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (modes.getCurrent() instanceof OnlineMatchSession) {
            if (modes.getCurrent().getSnapshot().getPhase() == SessionPhase.FAILED) { showOnline(); return; }
            if (!rematchRequested && onlineClient != null) {
                try { rematchRequestId = onlineClient.send(RoomCommand.requestRematch()); rematchRequested = true; render(); }
                catch (IllegalStateException error) { showOnline(); }
            }
        } else if (storyActive && progress.getActiveRun() != null) {
            EncounterRun run = progress.restartActive(newRunId());
            prepareSessionChange(); storyActive = true;
            startStoryEncounter(run, seeds.nextLong());
        } else if (lastGameWasBattle) startBattle(); else startNewGame();
    }
    public void nextEncounter() {
        ScreenRouter.requireEdt();
        EncounterRun active = progress.getActiveRun();
        if (closed || !storyActive || active == null || !active.isWon() || !"result".equals(router.getCurrentId())) return;
        EncounterRun next = progress.nextEncounter(newRunId());
        prepareSessionChange();
        if (next == null) { storyActive = false; showStages(); }
        else { storyActive = true; startStoryEncounter(next, seeds.nextLong()); }
    }
    private String newRunId() { return UUID.randomUUID().toString(); }
    private void prepareSessionChange() {
        router.show("home"); tutorialGravity.stop();
        if (onlineSubscription != null) { onlineSubscription.close(); onlineSubscription = null; }
        onlineClient = null;
        if (matchSubscription != null) { matchSubscription.close(); matchSubscription = null; }
        modes.clear();
        if (tutorial != null) { tutorial.close(); tutorial = null; }
        if (localGame != null) { localGame.close(); localGame = null; }
        onlineResultReturned = false;
    }
    public void submit(GameAction.Type type) {
        ScreenRouter.requireEdt(); if (!canPlay()) return;
        if (localGame != null) { localGame.submit(type); render(); }
        else if (tutorial != null) { tutorial.submit(type); render(); }
        else if (modes.getCurrent() != null && isUserAction(type)) modes.getCurrent().submit(new PlayerIntent(type));
    }
    private static boolean isUserAction(GameAction.Type type) {
        return type == GameAction.Type.MOVE_LEFT || type == GameAction.Type.MOVE_RIGHT
                || type == GameAction.Type.ROTATE_LEFT || type == GameAction.Type.ROTATE_RIGHT
                || type == GameAction.Type.SOFT_DROP || type == GameAction.Type.HARD_DROP || type == GameAction.Type.HOLD;
    }
    /** 튜토리얼 및 로컬 대전만 공유하는 시간 진행 검증 경계 */
    void advanceGravity() {
        ScreenRouter.requireEdt(); if (!canPlay()) return;
        if (localGame != null) { localGame.tick(); render(); }
        else if (tutorial != null) { tutorial.tick(); render(); }
        else if (modes.getCurrent() instanceof LocalMatchSession) ((LocalMatchSession) modes.getCurrent()).advanceGravity();
    }
    private boolean canPlay() {
        return !closed && ("game".equals(router.getCurrentId()) || "battle".equals(router.getCurrentId()));
    }
    private void render() {
        if (localGame != null) {
            game.setState(localGame.getPlayerState()); game.setInstruction(localGame.getInstruction());
            if (localGame.isFinished()) {
                tutorialGravity.stop(); result.setLocalResult(localGame.getPlayerState(), localGame.isCompleted());
                router.show("result");
            } else if (localGame.isPaused()) tutorialGravity.stop();
            else if (canPlay()) tutorialGravity.start();
            return;
        }
        if (tutorial != null) {
            game.setState(tutorial.getPlayerState()); game.setInstruction(tutorial.getInstruction());
            if (tutorial.isFinished()) {
                tutorialGravity.stop(); result.setTutorialResult(tutorial.isCompleted(), tutorial.getStep()); router.show("result");
            } else if (tutorial.isPaused()) tutorialGravity.stop();
            else if (canPlay()) tutorialGravity.start();
            return;
        }
        MatchSession match = modes.getCurrent(); if (match == null) return;
        SessionSnapshot snapshot = match.getSnapshot();
        BattleState state = snapshot.getBattleState();
        if (snapshot.getPhase() == SessionPhase.FAILED) {
            result.setFailure(match instanceof LocalMatchSession ? ((LocalMatchSession) match).getFailure() : "연결이 종료되었습니다.");
            if (match instanceof OnlineMatchSession) result.setOnlineRetry(false, true);
            router.show("result"); return;
        }
        if (state == null) {
            if (match instanceof OnlineMatchSession && snapshot.getPhase() == SessionPhase.WAITING) router.show("online");
            return;
        }
        if (match instanceof OnlineMatchSession) {
            battlePanel.setOnlineEncounter();
            onlineLobby.setMatchFinished(snapshot.getPhase() == SessionPhase.FINISHED);
        }
        battlePanel.setState(state, snapshot.getLocalParticipantId(), snapshot.getCapabilities().canPause(), isAiThinking());
        if (snapshot.getPhase() == SessionPhase.FINISHED) {
            if (match instanceof OnlineMatchSession && onlineResultReturned) { router.show("online"); return; }
            result.setBattleResult(state, snapshot.getLocalParticipantId());
            if (storyActive) {
                EncounterRun run = progress.getActiveRun();
                Stage lastStage = stages.getStages().get(stages.getStages().size() - 1);
                boolean last = run.getStageId().equals(lastStage.getId())
                        && run.getEncounterId().equals(lastStage.getEncounters()
                                .get(lastStage.getEncounters().size() - 1).getId());
                result.setStoryContinuation(run.isWon(), last);
            }
            if (match instanceof OnlineMatchSession) result.setOnlineRetry(rematchRequested, false);
            router.show("result");
        } else if (match instanceof OnlineMatchSession) {
            onlineResultReturned = false;
            router.show("battle");
        }
    }
    public void togglePause() {
        ScreenRouter.requireEdt(); if (!canPlay()) return;
        if (localGame != null) {
            if (localGame.isFinished()) return;
            if (localGame.isPaused()) localGame.resume(); else localGame.pause(); render();
        } else if (tutorial != null) {
            if (tutorial.isFinished()) return;
            if (tutorial.isPaused()) tutorial.resume(); else tutorial.pause(); render();
        } else if (modes.getCurrent() != null) {
            MatchSession match = modes.getCurrent();
            if (match.getSnapshot().getCapabilities().canPause()) match.requestPause(match.getSnapshot().getPhase() != SessionPhase.PAUSED);
        }
    }
    public void showHome() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (modes.getCurrent() instanceof OnlineMatchSession) prepareSessionChange();
        else router.show("home");
    }
    public void continueGame() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (hasStoryResult()) { render(); return; }
        if (localGame != null && localGame.isPaused() && !localGame.isFinished()) {
            router.show("game"); localGame.resume(); render();
        } else if (tutorial != null && tutorial.isPaused() && !tutorial.isFinished()) {
            router.show("game"); tutorial.resume(); render();
        } else if (modes.getCurrent() != null && modes.getCurrent().getSnapshot().getPhase() == SessionPhase.PAUSED) {
            router.show("battle"); modes.getCurrent().requestPause(false);
        }
    }
    private void enterGame() {
        render();
        if (tutorial != null && !tutorial.isPaused() && !tutorial.isFinished()) tutorialGravity.start();
        if (localGame != null && !localGame.isPaused() && !localGame.isFinished()) tutorialGravity.start();
        if (modes.getCurrent() instanceof LocalMatchSession) ((LocalMatchSession) modes.getCurrent()).startClock();
    }
    private void leaveGame() {
        tutorialGravity.stop();
        if (tutorial != null && !tutorial.isFinished()) tutorial.pause();
        if (localGame != null && !localGame.isFinished()) localGame.pause();
        MatchSession match = modes.getCurrent();
        if (match != null) {
            SessionSnapshot snapshot = match.getSnapshot();
            if (snapshot.getPhase() == SessionPhase.RUNNING && snapshot.getCapabilities().canPause()) match.requestPause(true);
        }
    }
    private void refreshHome() {
        boolean paused = tutorial != null && tutorial.isPaused() && !tutorial.isFinished()
                || modes.getCurrent() != null && modes.getCurrent().getSnapshot().getPhase() == SessionPhase.PAUSED;
        home.setCanContinue(hasStoryResult() || paused);
        home.setContinueText(hasStoryResult() ? "스토리 결과로 돌아가기" : "계속하기");
    }
    private boolean hasStoryResult() {
        return storyActive && modes.getCurrent() != null && modes.getCurrent().getSnapshot().getPhase() == SessionPhase.FINISHED;
    }
    public ScreenRouter getRouter() { return router; }
    public CampaignProgress getCampaignProgress() { return progress.getCampaignProgress(); }
    public GameState getState() {
        if (localGame != null) return localGame.getPlayerState();
        if (tutorial != null) return tutorial.getPlayerState();
        MatchSession match = modes.getCurrent();
        if (match == null || match.getSnapshot().getBattleState() == null) return null;
        SessionSnapshot snapshot = match.getSnapshot();
        return snapshot.getBattleState().getParticipant(snapshot.getLocalParticipantId()).getGameState();
    }
    public BattleState getBattleState() { return modes.getCurrent() == null ? null : modes.getCurrent().getSnapshot().getBattleState(); }
    public boolean isGravityRunning() { return tutorialGravity.isRunning() || modes.getCurrent() instanceof LocalMatchSession
            && ((LocalMatchSession) modes.getCurrent()).isGravityRunning(); }
    public boolean isAiTimerRunning() { return modes.getCurrent() instanceof LocalMatchSession
            && ((LocalMatchSession) modes.getCurrent()).isAiTimerRunning(); }
    public boolean isAiThinking() { return modes.getCurrent() instanceof LocalMatchSession
            && ((LocalMatchSession) modes.getCurrent()).isThinking(); }
    public void close() {
        ScreenRouter.requireEdt(); if (closed) return;
        closed = true; tutorialGravity.stop();
        if (onlineSubscription != null) { onlineSubscription.close(); onlineSubscription = null; }
        onlineClient = null;
        if (matchSubscription != null) { matchSubscription.close(); matchSubscription = null; }
        modes.close(); if (tutorial != null) tutorial.close(); if (localGame != null) localGame.close();
        login.clearSecrets(); signUp.clearSecrets(); router.close();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TetrisApplication app = new TetrisApplication();
            MainWindow window = new MainWindow(app.getRouter(), app::close); window.setVisible(true);
        });
    }
}

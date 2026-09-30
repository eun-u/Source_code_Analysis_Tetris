package kr.ac.jbnu.se.tetris.app;

import java.util.Random;
import java.util.UUID;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import kr.ac.jbnu.se.tetris.ai.AIProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.ai.PlacementLog;
import kr.ac.jbnu.se.tetris.app.session.*;
import kr.ac.jbnu.se.tetris.battle.BattleState;
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
    private final GamePanel game;
    private final BattlePanel battlePanel;
    private final ResultPanel result;
    private final OnlineLobbyPanel onlineLobby;
    private final OnlineAccountController accounts;
    private final Timer tutorialGravity;
    private TutorialSession tutorial;
    private Subscription matchSubscription;
    private NetworkClient onlineClient;
    private String rankedSaveState;
    private String onlineFailureReason;
    private long onlineConnectGeneration;
    private NetworkSubscription onlineSubscription;
    private boolean rematchRequested;
    private long rematchRequestId;
    private PlayerProfile profile = new PlayerProfile();
    private PlacementLog placementLog = new PlacementLog();
    private boolean storyActive;
    private boolean lastGameWasBattle;
    private boolean closed;

    public TetrisApplication() {
        this(new Random());
    }
    /** 결정적 스토리 회귀 검증을 위한 시드 생성기 주입 */
    TetrisApplication(Random seeds) {
        this(seeds, loadOnlineConfig());
    }
    TetrisApplication(Random seeds, OnlineClientConfig onlineConfig) {
        ScreenRouter.requireEdt();
        if (seeds == null) throw new IllegalArgumentException("Seed generator is required");
        this.seeds = seeds;
        for (Stage stage : stages.getStages()) for (MonsterSpec monster : stage.getEncounters()) {
            aiProfiles.get(monster.getAiProfileId());
        }
        tutorialGravity = new Timer(400, event -> advanceGravity());
        tutorialGravity.setCoalesce(true);
        AssetManager assets = new AssetManager();
        home = new HomePanel(assets, this::startNewGame, this::showStages, this::showOnline, this::continueGame, this::refreshHome);
        accounts = new OnlineAccountController(onlineConfig, () -> router.show("online"), () -> {
            prepareSessionChange(); router.show("account");
        });
        onlineLobby = new OnlineLobbyPanel(this::connectOnline, this::sendRoomCommand, this::showHome,
                this::showAccount, this::connectRanked, accounts.isConfigured());
        accounts.setTokenListener(token -> {
            if (onlineClient instanceof WebSocketNetworkClient) {
                try { onlineClient.refreshAuthentication(token); }
                catch (IllegalStateException disconnected) { onlineLobby.setMessage("접속이 종료되었습니다. 다시 접속하세요."); }
            }
        });
        game = new GamePanel(this::submit, this::togglePause, this::showHome, this::enterGame, this::leaveGame);
        battlePanel = new BattlePanel(assets, this::submit, this::togglePause, this::showHome,
                this::enterGame, this::leaveGame);
        result = new ResultPanel(this::restart, this::showHome, this::nextEncounter);
        result.setLeaderboardAction(this::showAccount);
        router.register(home); router.register(game); router.register(battlePanel); router.register(result);
        router.register(onlineLobby);
        router.register(accounts.getPanel());
        router.register(new StageSelectPanel(stages,
                id -> progress.getCampaignProgress().isStageUnlocked(id), this::startStory, this::showHome));
        router.show("home");
    }
    public void startNewGame() { startNewGame(seeds.nextLong()); }
    public void startBattle() { startBattle(seeds.nextLong()); }
    public void startNewGame(long seed) {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false; lastGameWasBattle = false;
        tutorial = new TutorialSession(seed); router.show("game"); render();
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
    public void showAccount() {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); onlineLobby.reset(); storyActive = false; router.show("account");
        if (accounts.isSignedIn()) accounts.refreshLeaderboard();
    }
    public void connectRanked() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (!accounts.isSignedIn()) { showAccount(); return; }
        long attempt = ++onlineConnectGeneration;
        onlineLobby.setMessage("로그인 상태를 확인하고 서버에 접속하는 중...");
        accounts.connect(options -> {
            if (!closed && attempt == onlineConnectGeneration && "online".equals(router.getCurrentId())) connectOnline(options);
        });
    }
    private static OnlineClientConfig loadOnlineConfig() {
        try { return OnlineClientConfig.load(); }
        catch (java.io.IOException | IllegalArgumentException | IllegalStateException failure) { return null; }
    }
    public void connectOnline(ConnectionOptions options) {
        ScreenRouter.requireEdt(); if (closed) return;
        prepareSessionChange(); storyActive = false; rematchRequested = false; rematchRequestId = 0;
        onlineLobby.reset(); onlineLobby.setConnecting();
        NetworkClient client = options.isWebSocket() ? new WebSocketNetworkClient() : new TcpNetworkClient(); onlineClient = client;
        rankedSaveState = options.isWebSocket() ? "SAVE_PENDING" : null;
        onlineFailureReason = null;
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
                    rematchRequested = false; rematchRequestId = 0;
                    rankedSaveState = options.isWebSocket() ? "SAVE_PENDING" : null;
                    break;
                case RANKED_SAVE_STATUS:
                    MatchSession active = modes.getCurrent();
                    if (active != null && update.getMatchId().equals(active.getSnapshot().getMatchId())) {
                        rankedSaveState = update.getReasonCode(); render();
                    }
                    break;
                case ERROR:
                case CLOSED:
                case CONNECTION_FAILED:
                    onlineFailureReason = update.getReasonCode();
                    if ("SAVE_PENDING".equals(rankedSaveState)) rankedSaveState = "SAVE_FAILED";
                    render();
                    break;
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
        profile = new PlayerProfile(); placementLog = new PlacementLog();
        startStoryEncounter(run, seed);
    }
    public void startStory(int stageIndex, long seed) {
        if (stageIndex < 0 || stageIndex >= stages.getStages().size()) throw new IllegalArgumentException("Unknown stage");
        startStory(stages.getStages().get(stageIndex).getId(), seed);
    }
    private void startStoryEncounter(EncounterRun run, long seed) {
        MonsterSpec spec = run.getMonster();
        MonsterSession engine = new MonsterSession(seed, spec.getName(), spec.getHp(),
                aiProfiles.get(spec.getAiProfileId()).getDelayMillis(), MonsterStrategies.create(spec),
                profile, placementLog, run.getRunId(), run.getLocalParticipantId(),
                run.getMonsterParticipantId(), System::nanoTime);
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
            if (rankedSaveState != null && !"SAVED".equals(rankedSaveState) && !"VOIDED".equals(rankedSaveState)) return;
            if (!rematchRequested && onlineClient != null) {
                try { rematchRequestId = onlineClient.send(RoomCommand.requestRematch()); rematchRequested = true; render(); }
                catch (IllegalStateException error) { showOnline(); }
            }
        } else if (storyActive && progress.getActiveRun() != null) {
            EncounterRun run = progress.restartActive(newRunId());
            prepareSessionChange(); storyActive = true;
            profile = new PlayerProfile(); placementLog = new PlacementLog();
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
        onlineConnectGeneration++;
        router.show("home"); tutorialGravity.stop();
        if (onlineSubscription != null) { onlineSubscription.close(); onlineSubscription = null; }
        onlineClient = null;
        rankedSaveState = null;
        onlineFailureReason = null;
        if (matchSubscription != null) { matchSubscription.close(); matchSubscription = null; }
        modes.clear();
        if (tutorial != null) { tutorial.close(); tutorial = null; }
    }
    public void submit(GameAction.Type type) {
        ScreenRouter.requireEdt(); if (!canPlay()) return;
        if (tutorial != null) { tutorial.submit(type); render(); }
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
        if (tutorial != null) { tutorial.tick(); render(); }
        else if (modes.getCurrent() instanceof LocalMatchSession) ((LocalMatchSession) modes.getCurrent()).advanceGravity();
    }
    private boolean canPlay() {
        return !closed && ("game".equals(router.getCurrentId()) || "battle".equals(router.getCurrentId()));
    }
    private void render() {
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
            result.setFailure(match instanceof LocalMatchSession ? ((LocalMatchSession) match).getFailure() : onlineFailureMessage());
            if (match instanceof OnlineMatchSession) {
                if (snapshot.getMatchId() != null) result.setRankedSaveStatus(rankedSaveState);
                // 실패 화면의 다시 접속은 재대결이 아니므로 저장 대기와 별도로 제공
                result.setOnlineRetry(false, true);
            }
            router.show("result"); return;
        }
        if (state == null) {
            if (match instanceof OnlineMatchSession && snapshot.getPhase() == SessionPhase.WAITING) router.show("online");
            return;
        }
        if (match instanceof OnlineMatchSession) battlePanel.setOnlineEncounter();
        battlePanel.setState(state, snapshot.getLocalParticipantId(), snapshot.getCapabilities().canPause(), isAiThinking());
        if (snapshot.getPhase() == SessionPhase.FINISHED) {
            result.setBattleResult(state, snapshot.getLocalParticipantId());
            if (storyActive) {
                EncounterRun run = progress.getActiveRun();
                boolean last = run.getStageId().equals(stages.getStages().get(stages.getStages().size() - 1).getId())
                        && run.getMonster().getTier() == MonsterTier.BOSS;
                result.setStoryContinuation(run.isWon(), last);
            }
            if (match instanceof OnlineMatchSession) {
                result.setOnlineRetry(rematchRequested, false); result.setRankedSaveStatus(rankedSaveState);
            }
            router.show("result");
        } else if (match instanceof OnlineMatchSession) {
            router.show("battle");
        }
    }
    public void togglePause() {
        ScreenRouter.requireEdt(); if (!canPlay()) return;
        if (tutorial != null) {
            if (tutorial.isFinished()) return;
            if (tutorial.isPaused()) tutorial.resume(); else tutorial.pause(); render();
        } else if (modes.getCurrent() != null) {
            MatchSession match = modes.getCurrent();
            if (match.getSnapshot().getCapabilities().canPause()) match.requestPause(match.getSnapshot().getPhase() != SessionPhase.PAUSED);
        }
    }
    private String onlineFailureMessage() {
        if ("VOIDED".equals(rankedSaveState)) return "경기를 완료할 수 없어 무효 처리했습니다. 다시 접속하세요.";
        if (onlineFailureReason != null && onlineFailureReason.contains("AUTH")) return "로그인 확인에 실패했습니다. 로그인 상태를 확인한 뒤 다시 접속하세요.";
        if (onlineFailureReason != null && (onlineFailureReason.contains("DRAIN") || onlineFailureReason.contains("MAINTENANCE"))) return "서버 점검 중입니다. 잠시 후 다시 접속하세요.";
        return "서버 연결이 종료되었습니다. 다시 접속할 수 있습니다.";
    }
    public void showHome() {
        ScreenRouter.requireEdt(); if (closed) return;
        onlineConnectGeneration++;
        if (modes.getCurrent() instanceof OnlineMatchSession) prepareSessionChange();
        else router.show("home");
    }
    public void continueGame() {
        ScreenRouter.requireEdt(); if (closed) return;
        if (hasStoryResult()) { render(); return; }
        if (tutorial != null && tutorial.isPaused() && !tutorial.isFinished()) {
            router.show("game"); tutorial.resume(); render();
        } else if (modes.getCurrent() != null && modes.getCurrent().getSnapshot().getPhase() == SessionPhase.PAUSED) {
            router.show("battle"); modes.getCurrent().requestPause(false);
        }
    }
    private void enterGame() {
        render();
        if (tutorial != null && !tutorial.isPaused() && !tutorial.isFinished()) tutorialGravity.start();
        if (modes.getCurrent() instanceof LocalMatchSession) ((LocalMatchSession) modes.getCurrent()).startClock();
    }
    private void leaveGame() {
        tutorialGravity.stop();
        if (tutorial != null && !tutorial.isFinished()) tutorial.pause();
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
        closed = true; tutorialGravity.stop(); accounts.close();
        if (onlineSubscription != null) { onlineSubscription.close(); onlineSubscription = null; }
        onlineClient = null;
        if (matchSubscription != null) { matchSubscription.close(); matchSubscription = null; }
        modes.close(); if (tutorial != null) tutorial.close(); router.close();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TetrisApplication app = new TetrisApplication();
            MainWindow window = new MainWindow(app.getRouter(), app::close); window.setVisible(true);
        });
    }
}

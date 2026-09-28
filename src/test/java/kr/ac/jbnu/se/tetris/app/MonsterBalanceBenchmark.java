package kr.ac.jbnu.se.tetris.app;

import java.util.Locale;
import kr.ac.jbnu.se.tetris.ai.AIPlan;
import kr.ac.jbnu.se.tetris.ai.AIProfile;
import kr.ac.jbnu.se.tetris.ai.AIProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.HeuristicStrategy;
import kr.ac.jbnu.se.tetris.ai.HeuristicWeights;
import kr.ac.jbnu.se.tetris.ai.PlacementLog;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;
import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.story.Stage;
import kr.ac.jbnu.se.tetris.story.StageCatalog;

/** 실제 전투 엔진의 몬스터 밸런스 비교용 수동 도구 및 자동 Test 묶음에서 제외 */
public final class MonsterBalanceBenchmark {
    private static final int[] STAGE_INDEXES = {0, 4};
    private static final long[] SEEDS = {1L, 7L};
    private static final int VIRTUAL_LIMIT_MILLIS = 120_000;
    private static final int QUANTUM_MILLIS = 100;
    private static final int GRAVITY_MILLIS = 400;
    private static final int HUMAN_CADENCE_MILLIS = 1_400;
    private static final long WALL_LIMIT_NANOS = 10_000_000_000L;

    private MonsterBalanceBenchmark() { }

    public static void main(String[] args) throws Exception {
        boolean smoke = args.length == 1 && "--smoke".equals(args[0]);
        if (args.length > 0 && !smoke) {
            throw new IllegalArgumentException("Usage: MonsterBalanceBenchmark [--smoke]");
        }
        StageCatalog catalog = StageCatalog.loadDefault();
        AIProfileCatalog profiles = AIProfileCatalog.loadDefault();
        int[][] summary = new int[MonsterTier.values().length][3];
        System.out.println("Synthetic benchmark: player = Heuristic SAFE surrogate, "
                + "one placement opportunity every 1400ms; not real human-play validation.");
        System.out.println("Rules: actual MonsterSession/BattleManager, gravity 400ms, "
                + "virtual limit 120s, wall limit 10s per duel. All monsters use fixed heuristic profiles.");
        System.out.println("| Stage | Tier | AI profile | Seed | Outcome | Virtual s | "
                + "HP player/monster | Placements player/monster | AI decisions | "
                + "Candidates total | AI mean/max ms | Synthetic log |");
        System.out.println("|---|---|---|---:|---|---:|---:|---:|---:|---:|---:|---:|");

        // 기본 실행 범위인 2개 장 × 3등급 × 2시드의 총 12회 대전
        for (int stageIndex : STAGE_INDEXES) {
            Stage stage = catalog.getStages().get(stageIndex);
            for (MonsterTier tier : MonsterTier.values()) {
                for (long seed : SEEDS) {
                    if (smoke && (stageIndex != STAGE_INDEXES[0]
                            || tier != MonsterTier.NORMAL || seed != SEEDS[0])) continue;
                    MonsterSpec spec = stage.getEncounters().get(tier.ordinal());
                    AIProfile aiProfile = profiles.get(spec.getAiProfileId());
                    Result result = run(spec, aiProfile, seed);
                    int[] counts = summary[tier.ordinal()];
                    if (result.outcome == Outcome.PLAYER) counts[0]++;
                    else if (result.outcome == Outcome.MONSTER) counts[1]++;
                    else counts[2]++;
                    System.out.println("| " + stage.getId() + " | " + tier + " | "
                            + aiProfile.getProfileId() + " | " + seed + " | " + result.outcome + " | "
                            + oneDecimal(result.virtualMillis / 1000.0) + " | "
                            + result.playerHp + "/" + result.monsterHp + " | "
                            + result.playerPlacements + "/" + result.monsterPlacements + " | "
                            + result.aiDecisions + " | " + result.aiCandidates + " | "
                            + oneDecimal(result.aiDecisions == 0 ? 0
                                    : result.aiTotalNanos / 1_000_000.0 / result.aiDecisions)
                            + "/" + oneDecimal(result.aiMaxNanos / 1_000_000.0) + " | "
                            + result.logSize + " |");
                }
            }
        }
        System.out.println("\n| Tier | Player wins | Monster wins | Unresolved |\n"
                + "|---|---:|---:|---:|");
        for (MonsterTier tier : MonsterTier.values()) {
            int[] counts = summary[tier.ordinal()];
            System.out.println("| " + tier + " | " + counts[0]
                    + " | " + counts[1] + " | " + counts[2] + " |");
        }
    }

    private static Result run(MonsterSpec spec, AIProfile aiProfile, long seed) throws InterruptedException {
        PlayerProfile profile = new PlayerProfile();
        PlacementLog log = new PlacementLog();
        MonsterSession session = new MonsterSession(seed, spec.getName(), spec.getHp(),
                aiProfile.getDelayMillis(), MonsterStrategies.create(spec),
                profile, log);
        HeuristicStrategy surrogate = new HeuristicStrategy(HeuristicWeights.SAFE,
                700, 40_000_000L);
        Result result = new Result();
        long wallDeadline = System.nanoTime() + WALL_LIMIT_NANOS;
        int nextHumanMillis = 0;
        try {
            for (int millis = 0; millis <= VIRTUAL_LIMIT_MILLIS;
                    millis += QUANTUM_MILLIS) {
                result.virtualMillis = millis;
                if (System.nanoTime() >= wallDeadline) {
                    result.outcome = Outcome.WALL_LIMIT;
                    break;
                }
                AIPlan previous = session.getLastPlan();
                long monsterVersion = session.getOpponentState().getVersion();
                long nowNanos = millis * 1_000_000L;
                session.pulse(nowNanos);

                // 작업자 종료까지 가상 시간을 멈춰 인위적인 계획 노후화 방지
                while (session.isThinking() && !session.isFinished()
                        && System.nanoTime() < wallDeadline) Thread.sleep(1);
                if (System.nanoTime() >= wallDeadline) {
                    result.outcome = Outcome.WALL_LIMIT;
                    break;
                }
                session.pulse(nowNanos);
                AIPlan ready = session.getLastPlan();
                if (ready != null && ready != previous) {
                    result.aiDecisions++;
                    result.aiCandidates += ready.getCandidateCount();
                    result.aiTotalNanos += ready.getElapsedNanos();
                    result.aiMaxNanos = Math.max(result.aiMaxNanos, ready.getElapsedNanos());
                    if (ready.getActions().contains(GameAction.Type.HARD_DROP)
                            && session.getOpponentState().getVersion() > monsterVersion) {
                        result.monsterPlacements++;
                    }
                }
                if (session.isFinished()) break;

                // 실제 공유 전투 엔진의 중력 입력을 양쪽 참가자에게 동일 간격으로 전달
                if (millis > 0 && millis % GRAVITY_MILLIS == 0) session.tick();
                if (session.isFinished()) break;

                if (millis >= nextHumanMillis) {
                    GameState state = session.getPlayerState();
                    if (state.getStatus() == GameState.Status.RUNNING
                            && state.getActivePiece() != null && !state.isAwaitingSpawn()) {
                        AIPlan plan = surrogate.plan(state);
                        for (GameAction.Type action : plan.getActions()) {
                            long beforeVersion = session.getPlayerState().getVersion();
                            session.submit(action);
                            if (action == GameAction.Type.HARD_DROP
                                    && session.getPlayerState().getVersion() > beforeVersion) {
                                result.playerPlacements++;
                            }
                            if (session.isFinished()) break;
                        }
                        nextHumanMillis = millis + HUMAN_CADENCE_MILLIS;
                    }
                }
                if (session.isFinished()) break;
            }
            BattleState battle = session.getBattleState();
            result.playerHp = battle.getParticipant(MonsterSession.PLAYER_ID).getHp();
            result.monsterHp = battle.getParticipant(MonsterSession.MONSTER_ID).getHp();
            result.logSize = log.size();
            if (battle.getStatus() == BattleState.Status.FINISHED) {
                String winner = battle.getWinnerId();
                result.outcome = MonsterSession.PLAYER_ID.equals(winner) ? Outcome.PLAYER
                        : MonsterSession.MONSTER_ID.equals(winner) ? Outcome.MONSTER : Outcome.DRAW;
            }
            return result;
        } finally {
            session.close();
        }
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private enum Outcome { PLAYER, MONSTER, DRAW, VIRTUAL_LIMIT, WALL_LIMIT }

    private static final class Result {
        private Outcome outcome = Outcome.VIRTUAL_LIMIT;
        private int virtualMillis;
        private int playerHp;
        private int monsterHp;
        private int playerPlacements;
        private int monsterPlacements;
        private int aiDecisions;
        private int aiCandidates;
        private int logSize;
        private long aiTotalNanos;
        private long aiMaxNanos;
    }
}

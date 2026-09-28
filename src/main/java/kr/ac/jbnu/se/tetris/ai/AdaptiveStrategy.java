package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.core.GameState;

/** 향후 WeightPolicy 구현 참고용 구버전 가중치 계산, 현재 전투 경로에서 미사용 */
@Deprecated
public final class AdaptiveStrategy implements AIStrategy {
    // 최소 6개 피스 배치 관측 전에는 기본 가중치 사용, 값 증가 시 적응 시작 지연
    private static final int MIN_OBSERVATIONS = 6;
    private final PlayerProfile profile;
    private final HeuristicWeights base;
    private final int maxStates;
    private final long budgetNanos;
    private volatile long lastProfileReadNanos;
    private volatile HeuristicWeights lastWeights;

    public AdaptiveStrategy(PlayerProfile profile) {
        this(profile, HeuristicWeights.SAFE, HeuristicStrategy.DEFAULT_MAX_STATES,
                HeuristicStrategy.DEFAULT_BUDGET_NANOS);
    }

    public AdaptiveStrategy(PlayerProfile profile, HeuristicWeights base,
                            int maxStates, long budgetNanos) {
        if (profile == null || base == null || maxStates < 1 || budgetNanos < 1) {
            throw new IllegalArgumentException("Invalid adaptive strategy settings");
        }
        this.profile = profile;
        this.base = base;
        this.maxStates = maxStates;
        this.budgetNanos = budgetNanos;
        this.lastWeights = base;
    }

    public AIPlan plan(GameState state) {
        if (state == null) throw new IllegalArgumentException("Game state is required");
        long started = System.nanoTime();
        PlayerProfile.Snapshot snapshot = profile.snapshot();
        HeuristicWeights weights = weightsFor(snapshot);
        lastProfileReadNanos = System.nanoTime() - started;
        lastWeights = weights;
        AIPlan plan = new HeuristicStrategy(weights, maxStates, budgetNanos).plan(state);
        return new AIPlan(plan.getSourceVersion(), plan.getActions(), plan.getCandidateCount(),
                System.nanoTime() - started, plan.getScore(), plan.isTimedOut());
    }

    public long getLastProfileReadNanos() { return lastProfileReadNanos; }
    public HeuristicWeights getLastWeights() { return lastWeights; }

    private HeuristicWeights weightsFor(PlayerProfile.Snapshot player) {
        if (player.getPlacements() < MIN_OBSERVATIONS) return base;
        // 테트리스 중심 상대의 빠른 라인 삭제와 높은 보드 상대의 빈번한 공격 우선
        // 최근 테트리스·공격·홀드·하드드롭·높이로 공격 압력을 0~1 범위로 제한
        // 각 계수 확대 시 해당 플레이 성향에 대한 줄 삭제 보상 증가
        double pressure = Math.min(1.0, player.getRecentTetrisRate() * 4
                + player.getRecentAttackRate() * 0.7 + player.getRecentHoldRate() * 0.25
                + player.getRecentHardDropRate() * 0.2
                + (player.getRecentAverageHeight() > 45 ? 0.25 : 0));
        // 최근 구멍과 콤보로 위험 회피 강도를 0~1 범위로 제한
        // 콤보 계수 0.4 증가 시 콤보 반영 강화, 구멍 분모 10.0 증가 시 구멍 반영 약화
        double caution = Math.min(1.0, player.getRecentAverageHoles() / 10.0
                + player.getRecentComboRate() * 0.4);
        // pressure는 줄 삭제 가중치, caution은 높이·구멍 가중치에만 적용
        return new HeuristicWeights(
                base.getLine() + 7 * pressure,
                base.getFourLineBonus() + 4 * player.getRecentComboRate(),
                base.getAggregateHeight() - 0.08 * caution,
                base.getMaximumHeight() - 0.12 * caution,
                base.getHoles() - 1.5 * caution,
                base.getBumpiness(),
                base.getWells());
    }
}

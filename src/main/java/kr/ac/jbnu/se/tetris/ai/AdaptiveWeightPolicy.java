package kr.ac.jbnu.se.tetris.ai;

/** 최근 플레이어 행동에 따라 공통 탐색의 평가 성향만 제한 범위에서 조정. */
public final class AdaptiveWeightPolicy implements WeightPolicy {
    @Override public PolicyDecision decide(AIContext context, AIProfile profile) {
        if (context == null || profile == null) throw new IllegalArgumentException("Context and profile required");
        HeuristicWeights base = profile.getBaseWeights();
        double limit = Math.min(.2, profile.getMaxWeightDeltaRatio());
        PlayerProfile.Snapshot seen = context.getObservation();
        HeuristicWeights weights = base;
        if (seen.getPlacements() >= 6) {
            double pressure = Math.min(1.0, Math.max(0, seen.getRecentAttackRate() * 1.5
                    + seen.getRecentTetrisRate() * 2 + seen.getRecentHardDropRate() * .15));
            double caution = Math.min(1.0, Math.max(0, seen.getRecentComboRate()
                    + seen.getRecentAverageHoles() / 10.0));
            weights = new HeuristicWeights(base.getLine() * (1 + limit * pressure),
                    base.getFourLineBonus() * (1 + limit * pressure),
                    base.getAggregateHeight() * (1 + limit * caution),
                    base.getMaximumHeight() * (1 + limit * caution),
                    base.getHoles() * (1 + limit * caution),
                    base.getBumpiness(), base.getWells());
        }
        return new PolicyDecision(profile.getPolicyId(), weights, "ADAPTIVE",
                context.getDecisionId(), context.getGameState().getVersion());
    }
}

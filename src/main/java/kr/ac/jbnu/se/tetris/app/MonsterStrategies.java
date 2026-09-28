package kr.ac.jbnu.se.tetris.app;

import kr.ac.jbnu.se.tetris.ai.AIStrategy;
import kr.ac.jbnu.se.tetris.ai.AIProfile;
import kr.ac.jbnu.se.tetris.ai.AIProfileCatalog;
import kr.ac.jbnu.se.tetris.ai.FixedWeightPolicy;
import kr.ac.jbnu.se.tetris.ai.PolicyDrivenStrategy;
import kr.ac.jbnu.se.tetris.ai.PlacementLog;
import kr.ac.jbnu.se.tetris.ai.PlayerProfile;
import kr.ac.jbnu.se.tetris.story.MonsterSpec;

/** 설정 데이터와 전략 구현의 조립 경계 및 UI·Core의 전략 분기 방지 */
public final class MonsterStrategies {
    private MonsterStrategies() { }
    public static AIStrategy create(MonsterSpec spec, PlayerProfile profile, PlacementLog log) {
        return create(spec);
    }

    /** 등급과 무관한 명시적 AI 프로필 조립 */
    public static AIStrategy create(MonsterSpec spec) {
        if (spec == null) throw new IllegalArgumentException("Monster specification is required");
        AIProfile selected = AIProfileCatalog.loadDefault().get(spec.getAiProfileId());
        if ("FIXED".equals(selected.getPolicyId())) {
            return new PolicyDrivenStrategy(selected, new FixedWeightPolicy());
        }
        throw new IllegalArgumentException("Unsupported AI policy: " + selected.getPolicyId());
    }
}

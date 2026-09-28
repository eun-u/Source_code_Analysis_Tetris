package kr.ac.jbnu.se.tetris.item;

/** 아이템 정의 계약 및 실제 효과·소비 규칙의 전투 담당 확장 경계 */
public final class ItemSpec {
    public enum Target { SELF, OPPONENT }
    private final String id;
    private final String effectId;
    private final Target target;
    public ItemSpec(String id, String effectId, Target target) {
        if (id == null || id.trim().isEmpty() || effectId == null || effectId.trim().isEmpty() || target == null) {
            throw new IllegalArgumentException("Item requires identity, effect, and target");
        }
        this.id = id; this.effectId = effectId; this.target = target;
    }
    public String getId() { return id; }
    public String getEffectId() { return effectId; }
    public Target getTarget() { return target; }
}

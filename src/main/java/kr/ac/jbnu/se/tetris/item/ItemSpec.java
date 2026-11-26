package kr.ac.jbnu.se.tetris.item;

/** 아이템 정의 계약 및 실제 효과·소비 규칙의 전투 담당 확장 경계 */
public final class ItemSpec {
    public enum Target { SELF, OPPONENT }
    public enum Category { H, L, G }
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

    /** 기획 9.3: H=HP, L=라인, G=기타. */
    public static Category categoryOf(String id) {
        if (ItemIds.DAMAGE_BOOST.equals(id) || ItemIds.HEAL.equals(id) || ItemIds.SHIELD.equals(id)) return Category.H;
        if (ItemIds.GARBAGE_BOMB.equals(id) || ItemIds.LINE_CLEANER.equals(id)) return Category.L;
        if (ItemIds.FEVER_CHARGE.equals(id) || ItemIds.TIME_WARP.equals(id)
                || ItemIds.NULLIFY.equals(id)) return Category.G;
        throw new IllegalArgumentException("Unknown item: " + id);
    }

    /** 획득 시 한 슬롯에 들어가는 사용 횟수. */
    public static int initialChargesOf(String id) {
        categoryOf(id);
        return ItemIds.GARBAGE_BOMB.equals(id) || ItemIds.HEAL.equals(id)
                || ItemIds.LINE_CLEANER.equals(id) ? 2 : 1;
    }

    /**
     * 조건이 맞으면 저절로 발동해서 직접 사용할 수 없는 아이템인지.
     * 전투 규칙, 몬스터 AI, 아이템 슬롯 표시가 모두 이 판정을 함께 쓴다.
     */
    public static boolean isAutomatic(String id) {
        return ItemIds.DAMAGE_BOOST.equals(id) || ItemIds.SHIELD.equals(id);
    }

    /** 아이템을 쓸 대상: 공격형(가비지 폭탄, 무효화)은 상대, 나머지는 자신. */
    public static Target targetOf(String id) {
        return ItemIds.GARBAGE_BOMB.equals(id) || ItemIds.NULLIFY.equals(id) ? Target.OPPONENT : Target.SELF;
    }
}

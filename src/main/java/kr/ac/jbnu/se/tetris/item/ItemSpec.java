package kr.ac.jbnu.se.tetris.item;

// 아이템 정의 계약 및 실제 효과·소비 규칙의 전투 담당 확장 경계
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

    // 아이템의 자동 사용 여부를 판단한다.
    // 공격 증폭과 보호막은 조건 충족 시 전투 로직에서 자동 발동한다.
    // @param id 아이템 ID / @return 자동 사용 아이템이면 true
    public static boolean isAutomatic(String id) {
        return ItemIds.DAMAGE_BOOST.equals(id) || ItemIds.SHIELD.equals(id);
    }

    // 아이템의 사용 대상을 반환한다.
    // @param id 아이템 ID / @return 자신 또는 상대방
    public static Target targetOf(String id) {

        // 등록되지 않은 아이템 ID인지 먼저 확인한다.
        categoryOf(id);

        // 방해 아이템은 상대방에게 사용한다.
        if (ItemIds.GARBAGE_BOMB.equals(id)
                || ItemIds.NULLIFY.equals(id)) {
            return Target.OPPONENT;
        }

        // 나머지 아이템은 자신에게 사용한다.
        return Target.SELF;
    }

    // 기획 9.3: H=HP, L=라인, G=기타.
    public static Category categoryOf(String id) {
        if ("damage_boost".equals(id) || "heal".equals(id) || "shield".equals(id)) return Category.H;
        if ("garbage_bomb".equals(id) || "line_cleaner".equals(id)) return Category.L;
        if ("fever_charge".equals(id) || "time_warp".equals(id)
                || "nullify".equals(id)) return Category.G;
        throw new IllegalArgumentException("Unknown item: " + id);
    }

    // 획득 시 한 슬롯에 들어가는 사용 횟수.
    public static int initialChargesOf(String id) {
        categoryOf(id);
        return "garbage_bomb".equals(id) || "heal".equals(id)
                || "line_cleaner".equals(id) ? 2 : 1;
    }
}

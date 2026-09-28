package kr.ac.jbnu.se.tetris.character;

/** 공통 전투에 주입할 기본 캐릭터 설정 및 특수 능력의 후속 확장 경계 */
public final class CharacterSpec {
    public static final CharacterSpec DEFAULT = new CharacterSpec("student", "대학생", 100);
    private final String id;
    private final String name;
    private final int maxHp;

    public CharacterSpec(String id, String name, int maxHp) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty() || maxHp <= 0) {
            throw new IllegalArgumentException("Character requires identity and positive HP");
        }
        this.id = id; this.name = name; this.maxHp = maxHp;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public int getMaxHp() { return maxHp; }
}

package kr.ac.jbnu.se.tetris.battle;

import kr.ac.jbnu.se.tetris.character.CharacterSpec;

/** 참가자 한 명의 ID·표시 이름·최대 HP를 담은 불변 전투 설정 */
public final class ParticipantSpec {
    private final String id;
    private final String name;
    private final int maxHp;

    public ParticipantSpec(String id, String name, int maxHp) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()
                || maxHp <= 0) {
            throw new IllegalArgumentException("Participant needs an ID, name, and positive max HP");
        }
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
    }

    public String getId() { return id; }

    public ParticipantSpec(String id, String name, CharacterSpec character) {
        this(id, name, requireCharacter(character).getMaxHp());
    }

    private static CharacterSpec requireCharacter(CharacterSpec character) {
        if (character == null) throw new IllegalArgumentException("Character is required");
        return character;
    }
    public String getName() { return name; }
    public int getMaxHp() { return maxHp; }
}

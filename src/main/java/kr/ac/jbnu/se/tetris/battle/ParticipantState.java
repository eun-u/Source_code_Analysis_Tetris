package kr.ac.jbnu.se.tetris.battle;

import kr.ac.jbnu.se.tetris.core.GameState;

/** 전투 스냅샷에 포함되는 참가자 한 명의 불변 상태 */
public final class ParticipantState {
    private final String id;
    private final String name;
    private final int hp;
    private final int maxHp;
    private final GameState gameState;
    private final boolean eliminated;

    ParticipantState(String id, String name, int hp, int maxHp, GameState gameState,
                     boolean eliminated) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.maxHp = maxHp;
        this.gameState = gameState;
        this.eliminated = eliminated;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public GameState getGameState() { return gameState; }
    public boolean isEliminated() { return eliminated; }
}

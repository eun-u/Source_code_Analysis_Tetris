package kr.ac.jbnu.se.tetris.core;

/** 기존 테트로미노 좌표계를 유지한 불변 블록 및 위쪽으로 증가하는 보드 y 좌표 */
public final class Piece {
    private static final int[][][] SHAPES = {
        {{0, 0}, {0, 0}, {0, 0}, {0, 0}},
        {{0, -1}, {0, 0}, {-1, 0}, {-1, 1}},
        {{0, -1}, {0, 0}, {1, 0}, {1, 1}},
        {{0, -1}, {0, 0}, {0, 1}, {0, 2}},
        {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},
        {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        {{-1, -1}, {0, -1}, {0, 0}, {0, 1}},
        {{1, -1}, {0, -1}, {0, 0}, {0, 1}}
    };

    private final PieceType type;
    private final int rotation;
    private final int[][] cells;
    private final long identity;
    private final String itemId;

    public Piece(PieceType type) {
        if (type == null || type == PieceType.EMPTY || type == PieceType.GARBAGE) {
            throw new IllegalArgumentException("A falling piece needs a non-empty type");
        }
        this.type = type;
        this.rotation = 0;
        this.cells = copy(SHAPES[type.ordinal()]);
        this.identity = 0;
        this.itemId = null;
    }

    Piece(PieceType type, long identity, String itemId) {
        this(type, 0, copy(SHAPES[type.ordinal()]), identity, itemId);
    }

    /** 네트워크 표시 스냅샷에서 아이템 미노를 복원한다. */
    public static Piece withItem(PieceType type, String itemId, long identity) {
        Piece base = new Piece(type);
        if (identity < 0) throw new IllegalArgumentException("Invalid piece identity");
        return new Piece(base.type, base.rotation, copy(base.cells), identity, itemId);
    }

    public Piece withItem(String itemId, long identity) {
        if (identity < 0) throw new IllegalArgumentException("Invalid piece identity");
        return new Piece(type, rotation, copy(cells), identity, itemId);
    }

    private Piece(PieceType type, int rotation, int[][] cells, long identity, String itemId) {
        this.type = type;
        this.rotation = rotation;
        this.cells = cells;
        this.identity = identity;
        this.itemId = itemId;
    }

    private static int[][] copy(int[][] source) {
        int[][] result = new int[4][2];
        for (int i = 0; i < 4; i++) {
            result[i][0] = source[i][0];
            result[i][1] = source[i][1];
        }
        return result;
    }

    public PieceType getType() { return type; }
    public int getRotation() { return rotation; }
    public long getIdentity() { return identity; }
    public String getItemId() { return itemId; }

    public int x(int index) { return cells[index][0]; }
    public int y(int index) { return cells[index][1]; }

    public int minY() {
        int minimum = cells[0][1];
        for (int i = 1; i < 4; i++) minimum = Math.min(minimum, cells[i][1]);
        return minimum;
    }

    public Piece rotateLeft() {
        if (type == PieceType.O) return this;
        int[][] rotated = new int[4][2];
        for (int i = 0; i < 4; i++) {
            rotated[i][0] = cells[i][1];
            rotated[i][1] = -cells[i][0];
        }
        return new Piece(type, (rotation + 3) % 4, rotated, identity, itemId);
    }

    public Piece rotateRight() {
        if (type == PieceType.O) return this;
        int[][] rotated = new int[4][2];
        for (int i = 0; i < 4; i++) {
            rotated[i][0] = -cells[i][1];
            rotated[i][1] = cells[i][0];
        }
        return new Piece(type, (rotation + 1) % 4, rotated, identity, itemId);
    }
}

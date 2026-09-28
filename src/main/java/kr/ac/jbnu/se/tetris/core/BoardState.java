package kr.ac.jbnu.se.tetris.core;

/** 호출자와 이후 엔진 동작으로부터 분리된 불변 보드 스냅샷 */
public final class BoardState {
    private final int width;
    private final int height;
    private final PieceType[] cells;

    BoardState(int width, int height, PieceType[] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells.clone();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public PieceType getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Cell outside board: " + x + "," + y);
        }
        return cells[y * width + x];
    }
}

package kr.ac.jbnu.se.tetris.core;

import java.util.Arrays;

/** 정착한 블록의 가변 셀 보관 및 GameEngine 전용 변경 권한 */
public final class Board {
    static final int WIDTH = 10;
    static final int HEIGHT = 22;
    private final PieceType[] cells = new PieceType[WIDTH * HEIGHT];

    Board() { Arrays.fill(cells, PieceType.EMPTY); }

    Board(BoardState snapshot) {
        if (snapshot == null || snapshot.getWidth() != WIDTH || snapshot.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Expected a 10 x 22 board snapshot");
        }
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                PieceType cell = snapshot.getCell(x, y);
                if (cell == null) throw new IllegalArgumentException("Snapshot contains a null cell");
                setCell(x, y, cell);
            }
        }
    }

    boolean canPlace(Piece piece, int originX, int originY) {
        for (int i = 0; i < 4; i++) {
            int x = originX + piece.x(i);
            int y = originY - piece.y(i);
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || getCell(x, y) != PieceType.EMPTY) return false;
        }
        return true;
    }

    PieceType getCell(int x, int y) { return cells[y * WIDTH + x]; }

    /** 가비지를 포함한 모든 칸이 비어 있는 상태의 확인, 퍼펙트 클리어 판정용 */
    boolean isEmpty() {
        for (PieceType cell : cells) {
            if (cell != PieceType.EMPTY) return false;
        }
        return true;
    }

    void setCell(int x, int y, PieceType type) { cells[y * WIDTH + x] = type; }

    void place(Piece piece, int originX, int originY) {
        if (!canPlace(piece, originX, originY)) throw new IllegalStateException("Invalid piece placement");
        for (int i = 0; i < 4; i++) setCell(originX + piece.x(i), originY - piece.y(i), piece.getType());
    }

    int landingY(Piece piece, int originX, int startY) {
        if (!canPlace(piece, originX, startY)) {
            throw new IllegalArgumentException("Initial piece placement is invalid");
        }
        int landing = startY;
        while (canPlace(piece, originX, landing - 1)) landing--;
        return landing;
    }

    void copyFrom(Board source) {
        System.arraycopy(source.cells, 0, cells, 0, cells.length);
    }

    int removeFullLines() {
        // 아래쪽부터 채워 쓰는 방식의 완성 줄 일괄 압축 및 최상단 행 비우기
        int destination = 0;
        for (int source = 0; source < HEIGHT; source++) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++) {
                if (getCell(x, source) == PieceType.EMPTY) { full = false; break; }
            }
            if (!full) {
                for (int x = 0; x < WIDTH; x++) setCell(x, destination, getCell(x, source));
                destination++;
            }
        }
        int removed = HEIGHT - destination;
        for (int y = destination; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) setCell(x, y, PieceType.EMPTY);
        }
        return removed;
    }

    /** 기존 정착 블록의 상향 이동 및 구멍 하나를 가진 바닥 가비지 행 삽입 */
    boolean addGarbageLine(int holeColumn) {
        if (holeColumn < 0 || holeColumn >= WIDTH) throw new IllegalArgumentException("Invalid hole column");
        boolean overflow = false;
        for (int x = 0; x < WIDTH; x++) overflow |= getCell(x, HEIGHT - 1) != PieceType.EMPTY;
        for (int y = HEIGHT - 1; y > 0; y--) {
            for (int x = 0; x < WIDTH; x++) setCell(x, y, getCell(x, y - 1));
        }
        for (int x = 0; x < WIDTH; x++) setCell(x, 0, x == holeColumn ? PieceType.EMPTY : PieceType.GARBAGE);
        return overflow;
    }

    BoardState snapshot() { return new BoardState(WIDTH, HEIGHT, cells); }
}

package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 이전·이후 보드를 비교해 새로 놓인 블록의 흔적을 찾는다. 출처 ID를 보내지 않는 구 서버 방식도 지원한다. */
final class BoardChanges {
    private BoardChanges() { }

    static long latestPlacedOrigin(BoardState board) {
        long latest = 0;
        for (int y = 0; y < board.getHeight(); y++)
            for (int x = 0; x < board.getWidth(); x++)
                if (board.getCell(x, y) != PieceType.EMPTY)
                    latest = Math.max(latest, board.getOriginId(x, y));
        return latest;
    }
    static boolean oldWirePlacement(BoardState before, BoardState after) {
        if (latestPlacedOrigin(before) != 0 || latestPlacedOrigin(after) != 0) return false;
        return countNewlyFilled(before, after) == 4
                && countCells(after, PieceType.GARBAGE) <= countCells(before, PieceType.GARBAGE);
    }
    private static int countCells(BoardState board, PieceType type) {
        int count = 0;
        for (int y = 0; y < board.getHeight(); y++)
            for (int x = 0; x < board.getWidth(); x++)
                if (board.getCell(x, y) == type) count++;
        return count;
    }
    /** 이전에 비어 있다가 가비지가 아닌 블록으로 채워진 칸의 수. */
    private static int countNewlyFilled(BoardState before, BoardState after) {
        int filled = 0;
        for (int y = 0; y < before.getHeight(); y++)
            for (int x = 0; x < before.getWidth(); x++) {
                PieceType newCell = after.getCell(x, y);
                if (before.getCell(x, y) == PieceType.EMPTY && newCell != PieceType.EMPTY
                        && newCell != PieceType.GARBAGE) filled++;
            }
        return filled;
    }
}

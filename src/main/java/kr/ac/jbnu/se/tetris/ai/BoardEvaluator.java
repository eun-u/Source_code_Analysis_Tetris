package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 시뮬레이션에서 피스 고정 후 보드 평가 */
public final class BoardEvaluator {
    private final HeuristicWeights weights;

    public BoardEvaluator(HeuristicWeights weights) {
        if (weights == null) throw new IllegalArgumentException("Weights are required");
        this.weights = weights;
    }

    public double score(BoardState board, int clearedLines) {
        if (board == null || clearedLines < 0) throw new IllegalArgumentException("Invalid board result");
        int[] heights = columnHeights(board);
        int holes = countHoles(board);
        int aggregate = 0;
        int maximum = 0;
        int bumpiness = 0;
        for (int x = 0; x < heights.length; x++) {
            aggregate += heights[x];
            maximum = Math.max(maximum, heights[x]);
            if (x > 0) bumpiness += Math.abs(heights[x] - heights[x - 1]);
        }
        int wells = countWells(heights, board.getHeight());
        return clearedLines * weights.getLine()
                + (clearedLines == 4 ? weights.getFourLineBonus() : 0)
                + aggregate * weights.getAggregateHeight()
                + maximum * weights.getMaximumHeight()
                + holes * weights.getHoles()
                + bumpiness * weights.getBumpiness()
                + wells * weights.getWells();
    }

    /** 각 열에서 가장 높이 쌓인 칸의 높이 */
    private static int[] columnHeights(BoardState board) {
        int[] heights = new int[board.getWidth()];
        for (int x = 0; x < heights.length; x++) {
            for (int y = board.getHeight() - 1; y >= 0; y--) {
                if (board.getCell(x, y) != PieceType.EMPTY) {
                    heights[x] = y + 1;
                    break;
                }
            }
        }
        return heights;
    }

    /** 블록 아래에 가려진 빈 칸의 수 */
    private static int countHoles(BoardState board) {
        int holes = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            boolean filledAbove = false;
            for (int y = board.getHeight() - 1; y >= 0; y--) {
                if (board.getCell(x, y) != PieceType.EMPTY) filledAbove = true;
                else if (filledAbove) holes++;
            }
        }
        return holes;
    }

    private static int countWells(int[] heights, int boardHeight) {
        int wells = 0;
        for (int x = 0; x < heights.length; x++) {
            int left = x == 0 ? boardHeight : heights[x - 1];
            int right = x == heights.length - 1 ? boardHeight : heights[x + 1];
            wells += Math.max(0, Math.min(left, right) - heights[x]);
        }
        return wells;
    }
}

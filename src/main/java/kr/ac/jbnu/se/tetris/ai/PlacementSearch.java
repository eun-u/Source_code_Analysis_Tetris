package kr.ac.jbnu.se.tetris.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.ac.jbnu.se.tetris.core.BoardState;
import kr.ac.jbnu.se.tetris.core.GameAction;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.Piece;
import kr.ac.jbnu.se.tetris.core.PieceType;
import kr.ac.jbnu.se.tetris.core.PlacementResult;
import kr.ac.jbnu.se.tetris.core.PlacementSimulator;

/** 실제 엔진이 허용하는 입력을 통한 착지 후보 탐색 */
final class PlacementSearch {
    private static final GameAction.Type[] MOVES = {
        GameAction.Type.MOVE_LEFT, GameAction.Type.MOVE_RIGHT,
        GameAction.Type.ROTATE_LEFT, GameAction.Type.ROTATE_RIGHT, GameAction.Type.SOFT_DROP
    };

    static final class Candidate {
        final Piece piece;
        final int x;
        final int y;
        final boolean held;
        final PlacementResult result;
        final List<GameAction.Type> actions;

        Candidate(Node node, PlacementResult result) {
            this.piece = node.piece;
            this.x = node.x;
            this.y = result.getY();
            this.held = node.held;
            this.result = result;
            this.actions = append(node.path, GameAction.Type.HARD_DROP);
        }
    }

    static final class Result {
        final List<Candidate> candidates;
        final boolean timedOut;

        Result(List<Candidate> candidates, boolean timedOut) {
            this.candidates = candidates;
            this.timedOut = timedOut;
        }
    }

    private static final class Node {
        final Piece piece;
        final int x;
        final int y;
        final boolean held;
        final List<GameAction.Type> path;

        Node(Piece piece, int x, int y, boolean held, List<GameAction.Type> path) {
            this.piece = piece;
            this.x = x;
            this.y = y;
            this.held = held;
            this.path = path;
        }

        String key() { return (held ? "H" : "A") + piece.getRotation() + ":" + x + ":" + y; }
    }

    private PlacementSearch() { }

    static Result search(GameState state, int maxStates, long budgetNanos, long started) {
        if (state.getStatus() != GameState.Status.RUNNING || state.getActivePiece() == null
                || state.isAwaitingSpawn()) {
            return new Result(Collections.<Candidate>emptyList(), false);
        }
        return search(state.getBoard(), state.getActivePiece(), state.getPieceX(), state.getPieceY(),
                state.canHold(), state.getHoldPiece(), state.getNextPieces(), maxStates,
                budgetNanos, started);
    }

    private static Result search(BoardState board, Piece activePiece, int pieceX, int pieceY,
                                 boolean canHold, PieceType holdPiece, List<PieceType> nextPieces,
                                 int maxStates, long budgetNanos, long started) {
        Node initial = new Node(activePiece, pieceX, pieceY,
                false, Collections.<GameAction.Type>emptyList());
        if (!PlacementSimulator.canPlace(board, initial.piece, initial.x, initial.y)) {
            return new Result(Collections.<Candidate>emptyList(), false);
        }
        Search run = new Search(board, maxStates, budgetNanos, started);
        run.enqueueRoots(initial, heldRoot(board, canHold, holdPiece, nextPieces));
        return run.execute();
    }

    /** 너비 우선 탐색 한 번의 진행 상태(대기열, 방문 기록, 후보, 시간 초과 여부) */
    private static final class Search {
        private final BoardState board;
        private final int maxStates;
        private final long budgetNanos;
        private final long started;
        private final ArrayDeque<Node> queue = new ArrayDeque<Node>();
        private final Set<String> visited = new HashSet<String>();
        private final Set<String> landings = new HashSet<String>();
        private final List<Candidate> candidates = new ArrayList<Candidate>();
        private boolean timedOut;

        Search(BoardState board, int maxStates, long budgetNanos, long started) {
            this.board = board;
            this.maxStates = maxStates;
            this.budgetNanos = budgetNanos;
            this.started = started;
        }

        void enqueueRoots(Node initial, Node held) {
            queue.add(initial);
            visited.add(initial.key());
            if (held != null && PlacementSimulator.canPlace(board, held.piece, held.x, held.y)
                    && visited.add(held.key())) queue.add(held);
        }

        Result execute() {
            int expanded = 0;
            while (!queue.isEmpty()) {
                if (limitReached(expanded)) {
                    timedOut = true;
                    break;
                }
                Node current = queue.removeFirst();
                expanded++;
                recordLanding(current);
                expandMoves(current);
                if (timedOut) break;
            }
            return new Result(candidates, timedOut);
        }

        /** 상태 수와 계산 시간 중 먼저 도달한 상한에서 탐색 중단 */
        private boolean limitReached(int expanded) {
            return expanded >= maxStates || (expanded > 0
                    && (System.nanoTime() - started >= budgetNanos
                        || Thread.currentThread().isInterrupted()));
        }

        private void recordLanding(Node current) {
            // 같은 X·회전에도 시작 높이에 따라 달라지는 틈새 착지 가능성
            PlacementResult result = PlacementSimulator.hardDrop(board, current.piece,
                    current.x, current.y);
            if (!result.isValid()) return;
            String key = (current.held ? "H" : "A") + current.piece.getType()
                    + ":" + current.piece.getRotation() + ":" + current.x + ":" + result.getY();
            if (landings.add(key)) candidates.add(new Candidate(current, result));
        }

        private void expandMoves(Node current) {
            for (GameAction.Type move : MOVES) {
                if (Thread.currentThread().isInterrupted()
                        || System.nanoTime() - started >= budgetNanos) {
                    timedOut = true;
                    break;
                }
                Node next = neighbor(current, move);
                if (next != null && visited.add(next.key())) queue.addLast(next);
            }
        }

        /** 이동 후 놓을 수 있으면 다음 노드를, 아니면 null을 돌려준다. */
        private Node neighbor(Node current, GameAction.Type move) {
            Piece piece = current.piece;
            int x = current.x;
            int y = current.y;
            switch (move) {
                case MOVE_LEFT: x--; break;
                case MOVE_RIGHT: x++; break;
                case ROTATE_LEFT: piece = piece.rotateLeft(); break;
                case ROTATE_RIGHT: piece = piece.rotateRight(); break;
                case SOFT_DROP: y--; break;
                default: throw new IllegalStateException("Unexpected search action");
            }
            if (!PlacementSimulator.canPlace(board, piece, x, y)) return null;
            return new Node(piece, x, y, current.held, append(current.path, move));
        }
    }

    private static Node heldRoot(BoardState board, boolean canHold, PieceType holdPiece,
                                 List<PieceType> nextPieces) {
        if (!canHold) return null;
        PieceType type = holdPiece;
        if (type == null || type == PieceType.EMPTY) {
            if (nextPieces.isEmpty()) return null;
            type = nextPieces.get(0);
        }
        if (type == null || type == PieceType.EMPTY || type == PieceType.GARBAGE) return null;
        Piece piece = new Piece(type);
        return new Node(piece, PlacementSimulator.spawnX(board),
                PlacementSimulator.spawnY(board, piece), true,
                Collections.singletonList(GameAction.Type.HOLD));
    }

    private static List<GameAction.Type> append(List<GameAction.Type> path, GameAction.Type action) {
        List<GameAction.Type> copy = new ArrayList<GameAction.Type>(path.size() + 1);
        copy.addAll(path);
        copy.add(action);
        return copy;
    }
}

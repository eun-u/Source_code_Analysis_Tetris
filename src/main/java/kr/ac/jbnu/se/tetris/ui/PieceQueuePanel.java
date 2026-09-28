package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import kr.ac.jbnu.se.tetris.core.GameState;
import kr.ac.jbnu.se.tetris.core.PieceType;

/** 불변 GameState의 HOLD 사용 가능 여부와 NEXT 순서를 표시 */
public final class PieceQueuePanel extends JPanel {
    private final PiecePreview hold = new PiecePreview();
    private final PiecePreview[] next = {new PiecePreview(), new PiecePreview(), new PiecePreview()};
    private final JLabel availability = new JLabel("HOLD · C", JLabel.CENTER);
    public PieceQueuePanel() {
        super(new GridLayout(1, 2, 8, 0));
        JPanel holdPanel = new JPanel(new BorderLayout());
        holdPanel.add(availability, BorderLayout.NORTH);
        holdPanel.add(hold, BorderLayout.CENTER);
        JPanel nextPanel = new JPanel(new BorderLayout());
        nextPanel.add(new JLabel("NEXT", JLabel.CENTER), BorderLayout.NORTH);
        JPanel pieces = new JPanel(new GridLayout(1, 3));
        for (PiecePreview piece : next) pieces.add(piece);
        nextPanel.add(pieces, BorderLayout.CENTER);
        add(holdPanel); add(nextPanel);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    }
    public void setState(GameState state) {
        hold.setPiece(state.getHoldPiece());
        availability.setText(state.getStatus() != GameState.Status.RUNNING ? "HOLD"
                : state.canHold() ? "HOLD · C" : "HOLD · 사용됨");
        List<PieceType> pieces = state.getNextPieces();
        for (int i = 0; i < next.length; i++) next[i].setPiece(i < pieces.size() ? pieces.get(i) : PieceType.EMPTY);
    }
}

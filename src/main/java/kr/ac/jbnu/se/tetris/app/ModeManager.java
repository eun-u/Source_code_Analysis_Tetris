package kr.ac.jbnu.se.tetris.app;

import kr.ac.jbnu.se.tetris.app.session.MatchSession;
import kr.ac.jbnu.se.tetris.ui.ScreenRouter;

/** 대전 세션 교체와 종료 소유권 및 이전 콜백 차단용 세대 관리 */
public final class ModeManager implements AutoCloseable {
    private MatchSession current;
    private long generation;
    public void install(MatchSession next) {
        ScreenRouter.requireEdt();
        if (next == null) throw new IllegalArgumentException("Match session required");
        clear(); current = next;
    }
    public MatchSession getCurrent() { return current; }
    public long getGeneration() { return generation; }
    public void clear() {
        ScreenRouter.requireEdt(); generation++;
        MatchSession previous = current; current = null;
        if (previous != null) previous.close();
    }
    @Override public void close() { clear(); }
}

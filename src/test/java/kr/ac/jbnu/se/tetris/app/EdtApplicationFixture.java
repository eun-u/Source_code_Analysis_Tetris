package kr.ac.jbnu.se.tetris.app;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** Owns a test application for its full lifetime on the Swing event thread. */
final class EdtApplicationFixture implements AutoCloseable {
    private final SeongeunApplication application;

    private EdtApplicationFixture(SeongeunApplication application) {
        this.application = application;
    }

    static EdtApplicationFixture open(Random random, PlayerSaveStore saveStore) throws Exception {
        AtomicReference<SeongeunApplication> created = new AtomicReference<SeongeunApplication>();
        SwingUtilities.invokeAndWait(() -> created.set(new SeongeunApplication(random, saveStore)));
        return new EdtApplicationFixture(created.get());
    }

    SeongeunApplication get() {
        return application;
    }

    @Override
    public void close() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            application.close();
        } else {
            SwingUtilities.invokeAndWait(application::close);
        }
    }
}

package kr.ac.jbnu.se.tetris.ui.seongeun.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** 광석이 깨진 실제 보드 칸에서 획득 슬롯까지 날아가는 비차단 화면 효과. */
public final class OreFlightLayer extends JComponent {
    private static final int DURATION = 780;
    private final List<Flight> flights = new ArrayList<Flight>();
    private final Timer timer = new Timer(16, event -> advance());

    public OreFlightLayer() {
        setOpaque(false);
        timer.setCoalesce(true);
    }

    /** 덮개가 버튼 입력을 가로채지 않는다. */
    @Override public boolean contains(int x, int y) { return false; }
    @Override public void removeNotify() { timer.stop(); flights.clear(); super.removeNotify(); }

    public void launch(Point source, Point target) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> launch(source, target));
            return;
        }
        if (source == null || target == null) return;
        if (flights.size() >= 8) flights.remove(0);
        flights.add(new Flight(new Point(source), new Point(target), System.currentTimeMillis()));
        if (!timer.isRunning()) timer.start();
        repaint();
    }

    public void reset() { flights.clear(); timer.stop(); repaint(); }

    private void advance() {
        long now = System.currentTimeMillis();
        for (Iterator<Flight> iterator = flights.iterator(); iterator.hasNext(); )
            if (now - iterator.next().started >= DURATION) iterator.remove();
        if (flights.isEmpty()) timer.stop();
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            long now = System.currentTimeMillis();
            for (Flight flight : flights) paintFlight(g, flight, now - flight.started);
        } finally { g.dispose(); }
    }

    private static void paintFlight(Graphics2D g, Flight flight, long age) {
        if (age < 0 || age > DURATION) return;
        int sx = flight.source.x, sy = flight.source.y;
        if (age < 220) {
            float fade = 1f - age / 220f;
            int radius = 5 + Math.round(age / 15f);
            g.setColor(new Color(255, 212, 105, Math.round(190 * fade)));
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8 + i * 0.15;
                int px = sx + (int) Math.round(Math.cos(angle) * radius);
                int py = sy + (int) Math.round(Math.sin(angle) * radius);
                g.fillRect(px, py, i % 3 == 0 ? 4 : 2, i % 3 == 0 ? 4 : 2);
            }
            g.setColor(new Color(255, 244, 188, Math.round(230 * fade)));
            g.drawLine(sx - radius / 2, sy - radius / 2, sx + 1, sy);
            g.drawLine(sx + 1, sy, sx + radius / 2, sy + radius / 2);
            g.drawLine(sx, sy, sx + radius / 2, sy - radius / 3);
        }
        if (age >= 70 && age < 560) {
            float t = Math.max(0f, Math.min(1f, (age - 70f) / 490f));
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            int x = Math.round(sx + (flight.target.x - sx) * eased);
            int y = Math.round(sy + (flight.target.y - sy) * eased
                    - (float) Math.sin(t * Math.PI) * Math.min(70, Math.abs(flight.target.x - sx) / 3 + 20));
            int trailX = Math.round(sx + (flight.target.x - sx) * Math.max(0f, eased - 0.07f));
            int trailY = Math.round(sy + (flight.target.y - sy) * Math.max(0f, eased - 0.07f)
                    - (float) Math.sin(Math.max(0f, t - 0.07f) * Math.PI) * 40);
            g.setColor(new Color(255, 180, 62, 105));
            g.drawLine(trailX, trailY, x, y);
            g.setColor(new Color(255, 205, 90, 95));
            g.fillOval(x - 11, y - 11, 22, 22);
            g.setColor(new Color(255, 248, 201));
            g.fillPolygon(new int[] {x, x + 5, x, x - 5}, new int[] {y - 7, y, y + 7, y}, 4);
            g.setColor(new Color(156, 91, 36));
            g.drawPolygon(new int[] {x, x + 5, x, x - 5}, new int[] {y - 7, y, y + 7, y}, 4);
        }
        if (age >= 500) {
            float t = Math.min(1f, (age - 500f) / 280f);
            int radius = 8 + Math.round(25 * t);
            g.setColor(new Color(255, 214, 102, Math.round(180 * (1f - t))));
            g.drawRect(flight.target.x - radius, flight.target.y - radius,
                    radius * 2, radius * 2);
            if (t < 0.35f) {
                g.setColor(new Color(255, 244, 180, Math.round(130 * (1f - t / 0.35f))));
                g.fillRect(flight.target.x - 8, flight.target.y - 8, 16, 16);
            }
        }
    }

    private static final class Flight {
        final Point source, target;
        final long started;
        Flight(Point source, Point target, long started) {
            this.source = source; this.target = target; this.started = started;
        }
    }
}

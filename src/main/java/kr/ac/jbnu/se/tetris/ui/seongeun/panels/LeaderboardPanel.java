package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardEntry;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.*;

/** 서버가 저장한 온라인 PvP 전적만 표시한다. */
public final class LeaderboardPanel extends kr.ac.jbnu.se.tetris.ui.seongeun.components.ScenePanel {
    private final PixelButton back = new PixelButton("로비로");
    private final PixelButton reload = new PixelButton("새로고침");
    private final JLabel status = new JLabel("");
    private final CardLayout views = new CardLayout();
    private final JPanel body = new JPanel(views);
    private final JLabel emptyTitle = new JLabel("랭킹을 불러오지 않았습니다", SwingConstants.CENTER);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"순위", "이름", "Elo", "승", "패", "경기"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    public LeaderboardPanel() {
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(18, UniversityPixelTheme.GUTTER, 18, UniversityPixelTheme.GUTTER));
        setBackground(UniversityPixelTheme.BG);
        back.secondary(); reload.secondary();
        back.setPreferredSize(new Dimension(96, 38)); reload.setPreferredSize(new Dimension(104, 38));
        add(UniversityPixelTheme.screenHeader("RANKED PvP", "온라인 랭킹 TOP 100", reload, back), BorderLayout.NORTH);
        JTable table = new JTable(model); table.setName("ranked-leaderboard");
        table.setRowHeight(34); table.setFont(UniversityPixelTheme.font(15, Font.PLAIN));
        table.setBackground(UniversityPixelTheme.PANEL); table.setForeground(UniversityPixelTheme.TEXT);
        table.setSelectionBackground(UniversityPixelTheme.PANEL_LIGHT);
        table.setSelectionForeground(UniversityPixelTheme.TEXT);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(UniversityPixelTheme.font(13, Font.BOLD));
        table.getTableHeader().setBackground(UniversityPixelTheme.PANEL_LIGHT);
        table.getTableHeader().setForeground(UniversityPixelTheme.TEXT_SUB);
        table.getTableHeader().setReorderingAllowed(false);
        table.setGridColor(UniversityPixelTheme.LINE); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, false, row, column);
                setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                // 1~3위는 금색 굵은 글씨로 눈에 띄게 한다.
                boolean podium = row < 3;
                setFont(UniversityPixelTheme.font(15, podium && (column == 0 || column == 1) ? Font.BOLD : Font.PLAIN));
                setForeground(podium && column == 0 ? UniversityPixelTheme.GOLD : UniversityPixelTheme.TEXT);
                if (!selected) setBackground(row % 2 == 0 ? UniversityPixelTheme.PANEL : UniversityPixelTheme.BG);
                return cell;
            }
        };
        for (int column = 0; column < model.getColumnCount(); column++)
            table.getColumnModel().getColumn(column).setCellRenderer(centered);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.getViewport().setBackground(UniversityPixelTheme.PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3));
        body.setOpaque(false);
        body.add(scroll, "table");
        body.add(emptyState(), "empty");
        views.show(body, "empty");
        add(body, BorderLayout.CENTER);
        status.setForeground(UniversityPixelTheme.TEXT_SUB);
        status.setFont(UniversityPixelTheme.font(12, Font.PLAIN));
        add(status, BorderLayout.SOUTH);
    }

    private JPanel emptyState() {
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBackground(UniversityPixelTheme.PANEL);
        empty.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3));
        JLabel trophy = new JLabel("♛", SwingConstants.CENTER);
        trophy.setFont(UniversityPixelTheme.font(48, Font.BOLD));
        trophy.setForeground(UniversityPixelTheme.GOLD);
        emptyTitle.setFont(UniversityPixelTheme.font(18, Font.BOLD));
        emptyTitle.setForeground(UniversityPixelTheme.TEXT);
        empty.add(Box.createVerticalGlue());
        for (JLabel label : new JLabel[] { trophy, emptyTitle }) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(label); empty.add(Box.createVerticalStrut(8));
        }
        empty.add(Box.createVerticalGlue());
        return empty;
    }

    public void setEntries(List<LeaderboardEntry> entries) {
        model.setRowCount(0);
        for (LeaderboardEntry entry : entries) model.addRow(new Object[] {entry.getRank(), entry.getDisplayName(),
                entry.getRating(), entry.getWins(), entry.getLosses(), entry.getGames()});
        if (entries.isEmpty()) {
            emptyTitle.setText("전적 없음");
            views.show(body, "empty");
        } else views.show(body, "table");
        setStatus(entries.isEmpty() ? "" : entries.size() + "명");
    }
    public void setStatus(String text) { status.setText(text); }
    /** 조회 실패. 이전 목록이 없으면 빈 화면 안내도 실패 내용으로 바꾼다. */
    public void setError(String text) {
        setStatus(text);
        if (model.getRowCount() == 0) {
            emptyTitle.setText("랭킹을 불러오지 못했습니다");
            views.show(body, "empty");
        }
    }
    public void setBusy(boolean busy) {
        reload.setEnabled(!busy);
        if (busy) {
            status.setText("랭킹을 불러오는 중...");
            if (model.getRowCount() == 0) {
                emptyTitle.setText("랭킹을 불러오는 중...");
            }
        }
    }
    public void setBackAction(ActionListener action) { back.addActionListener(action); }
    public void setReloadAction(ActionListener action) { reload.addActionListener(action); }
}

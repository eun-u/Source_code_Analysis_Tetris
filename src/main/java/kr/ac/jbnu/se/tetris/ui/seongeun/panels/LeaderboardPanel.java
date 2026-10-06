package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardEntry;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.*;

/** 서버가 저장한 온라인 PvP 전적만 표시한다. */
public final class LeaderboardPanel extends JPanel {
    private final JButton back = new PixelButton("로비로");
    private final JButton reload = new PixelButton("새로고침");
    private final JLabel status = new JLabel("온라인 로그인 후 랭킹을 조회하세요.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"순위", "이름", "Elo", "승", "패", "경기"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    public LeaderboardPanel() {
        setLayout(new BorderLayout(0, 14)); setBorder(new EmptyBorder(18, 22, 18, 22));
        setBackground(UniversityPixelTheme.BG);
        JPanel header = new JPanel(new BorderLayout(8, 0)); header.setOpaque(false);
        JLabel title = new JLabel("RANKED PvP  /  TOP 100");
        title.setFont(UniversityPixelTheme.font(23, Font.BOLD));
        title.setForeground(UniversityPixelTheme.GOLD);
        header.add(title, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); buttons.setOpaque(false);
        buttons.add(reload); buttons.add(back); header.add(buttons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JTable table = new JTable(model); table.setName("ranked-leaderboard");
        table.setRowHeight(35); table.setFont(UniversityPixelTheme.font(15, Font.PLAIN));
        table.setBackground(UniversityPixelTheme.PANEL); table.setForeground(UniversityPixelTheme.TEXT);
        table.setSelectionBackground(UniversityPixelTheme.PANEL_LIGHT);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(UniversityPixelTheme.font(14, Font.BOLD));
        table.getTableHeader().setBackground(UniversityPixelTheme.PANEL_LIGHT);
        table.getTableHeader().setForeground(UniversityPixelTheme.TEXT);
        table.setGridColor(UniversityPixelTheme.LINE); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.getViewport().setBackground(UniversityPixelTheme.PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3));
        add(scroll, BorderLayout.CENTER);
        status.setForeground(UniversityPixelTheme.TEXT_SUB);
        status.setFont(UniversityPixelTheme.font(13, Font.PLAIN));
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 1),
                new EmptyBorder(7, 11, 7, 11)));
        add(status, BorderLayout.SOUTH);
    }
    public void setEntries(List<LeaderboardEntry> entries) {
        model.setRowCount(0);
        for (LeaderboardEntry entry : entries) model.addRow(new Object[] {entry.getRank(), entry.getDisplayName(),
                entry.getRating(), entry.getWins(), entry.getLosses(), entry.getGames()});
        setStatus(entries.isEmpty() ? "아직 등록된 공식 경기 전적이 없습니다." : "서버가 확정한 온라인 PvP 결과 · " + entries.size() + "명");
    }
    public void setStatus(String text) { status.setText(text); }
    public void setBusy(boolean busy) { reload.setEnabled(!busy); if (busy) status.setText("랭킹을 불러오는 중..."); }
    public void setBackAction(ActionListener action) { back.addActionListener(action); }
    public void setReloadAction(ActionListener action) { reload.addActionListener(action); }
}

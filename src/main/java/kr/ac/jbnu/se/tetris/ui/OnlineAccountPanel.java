package kr.ac.jbnu.se.tetris.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import kr.ac.jbnu.se.tetris.ranking.LeaderboardEntry;

/** 계정 입력과 서버 확정 랭킹 표시. 네트워크·비밀번호 저장 책임은 없음. */
public final class OnlineAccountPanel extends JPanel implements Screen {
    public interface Actions {
        void signIn(String email, String password);
        void signUp(String email, String password);
        void recover(String email);
        void completeRecovery(String email, String code, String password);
        void signOut();
        void refreshLeaderboard();
    }
    private final JTextField email = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JTextField code = new JTextField(12);
    private final JPasswordField newPassword = new JPasswordField(24);
    private final JLabel status = new JLabel("로그인하면 온라인 대전과 랭킹을 이용할 수 있습니다.");
    private final JLabel account = new JLabel("로그인 전");
    private final List<JButton> actions = new ArrayList<JButton>();
    private final JButton logout;
    private final JButton refresh;
    private final DefaultTableModel rows = new DefaultTableModel(new String[] {"순위", "닉네임", "레이팅", "승", "패", "경기"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private boolean configured;
    private boolean signedIn;
    private boolean busy;

    public OnlineAccountPanel(Actions handler, Runnable back) {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel top = new JPanel(); top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("계정 · 온라인 PvP 랭킹");
        title.setFont(title.getFont().deriveFont(22f)); top.add(title);
        top.add(account);
        top.add(field("이메일", email)); top.add(field("비밀번호", password));
        JPanel login = new JPanel(new FlowLayout(FlowLayout.LEFT));
        login.add(button("로그인", "accountSignIn", () -> handler.signIn(email.getText().trim(), take(password))));
        login.add(button("회원가입", "accountSignUp", () -> handler.signUp(email.getText().trim(), take(password))));
        logout = button("로그아웃", "accountSignOut", handler::signOut); login.add(logout); top.add(login);
        JPanel recovery = new JPanel(new BorderLayout(8, 4));
        recovery.setBorder(BorderFactory.createTitledBorder("비밀번호 재설정"));
        JPanel recoveryFields = new JPanel(new GridLayout(0, 1, 0, 4));
        recoveryFields.add(field("메일 인증번호", code)); recoveryFields.add(field("새 비밀번호", newPassword));
        recovery.add(recoveryFields, BorderLayout.CENTER);
        JPanel recoveryButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recoveryButtons.add(button("인증번호 받기", "accountRecovery", () -> handler.recover(email.getText().trim())));
        recoveryButtons.add(button("비밀번호 변경", "accountReset", () -> {
            String otp = code.getText().trim(); code.setText("");
            handler.completeRecovery(email.getText().trim(), otp, take(newPassword));
        })); recovery.add(recoveryButtons, BorderLayout.SOUTH); top.add(recovery);
        status.setName("accountStatus"); top.add(status);
        for (java.awt.Component component : top.getComponents()) {
            component.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, component.getPreferredSize().height + 4));
            if (component instanceof javax.swing.JComponent) ((javax.swing.JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
        }
        add(top, BorderLayout.NORTH);
        JPanel ranking = new JPanel(new BorderLayout(4, 4));
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT)); heading.add(new JLabel("확정된 온라인 대전 기록 · 상위 100명"));
        refresh = button("랭킹 새로고침", "leaderboardRefresh", handler::refreshLeaderboard); heading.add(refresh);
        ranking.add(heading, BorderLayout.NORTH);
        JTable table = new JTable(rows); table.setName("leaderboardTable"); table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader());
        ranking.add(scroll, BorderLayout.CENTER); add(ranking, BorderLayout.CENTER);
        JButton home = new JButton("로비로"); home.setName("accountBack"); home.addActionListener(event -> back.run()); add(home, BorderLayout.SOUTH);
        email.setName("accountEmail"); password.setName("accountPassword"); code.setName("recoveryCode"); newPassword.setName("recoveryPassword");
        applyEnabled();
    }
    private JButton button(String label, String name, Runnable task) {
        JButton button = new JButton(label); button.setName(name); button.addActionListener(event -> task.run()); actions.add(button); return button;
    }
    private static JPanel field(String title, JTextField input) {
        JPanel row = new JPanel(new BorderLayout(10, 0)); JLabel label = new JLabel(title); label.setPreferredSize(new java.awt.Dimension(100, 24));
        label.setLabelFor(input); row.add(label, BorderLayout.WEST); row.add(input, BorderLayout.CENTER); return row;
    }
    private static String take(JPasswordField field) {
        char[] value = field.getPassword(); field.setText("");
        try { return new String(value); } finally { Arrays.fill(value, '\0'); }
    }
    public void setConfigured(boolean value) { ScreenRouter.requireEdt(); configured = value; applyEnabled(); }
    public void setBusy(boolean value) { ScreenRouter.requireEdt(); busy = value; applyEnabled(); }
    public void setSignedIn(boolean value) {
        ScreenRouter.requireEdt(); signedIn = value; account.setText(value ? "로그인 완료" : "로그인 전");
        if (!value) rows.setRowCount(0); applyEnabled();
    }
    public void setMessage(String message) { ScreenRouter.requireEdt(); status.setText(message); }
    public void setLeaderboard(List<LeaderboardEntry> entries) {
        ScreenRouter.requireEdt(); rows.setRowCount(0);
        for (LeaderboardEntry entry : entries) rows.addRow(new Object[] {entry.getRank(), entry.getDisplayName(), entry.getRating(), entry.getWins(), entry.getLosses(), entry.getGames()});
        if (entries.isEmpty()) setMessage("아직 확정된 대전 기록이 없습니다.");
    }
    private void applyEnabled() {
        for (JButton button : actions) button.setEnabled(configured && !busy);
        logout.setEnabled(configured && signedIn && !busy); refresh.setEnabled(configured && signedIn && !busy);
        email.setEnabled(configured && !busy); password.setEnabled(configured && !busy);
        code.setEnabled(configured && !busy); newPassword.setEnabled(configured && !busy);
    }
    @Override public void onExit() { password.setText(""); newPassword.setText(""); code.setText(""); }
    @Override public String getId() { return "account"; }
    @Override public JPanel getPanel() { return this; }
}

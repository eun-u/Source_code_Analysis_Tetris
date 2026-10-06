package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class LoginPanel extends JPanel {
    private final JTextField idField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final GameButton loginButton = new GameButton("로컬 시작");
    private final GameButton signUpButton = new GameButton("회원가입");
    private final GameButton onlineLoginButton = new GameButton("온라인 로그인");
    private final JLabel status = new JLabel("스토리는 계정 없이 플레이할 수 있습니다.");

    public LoginPanel() {
        setLayout(new BorderLayout());
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel content = new JPanel(new GridBagLayout()); content.setOpaque(false);
        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0)); columns.setOpaque(false);
        columns.setPreferredSize(new Dimension(660, 390));
        columns.add(localCard()); columns.add(onlineCard());
        content.add(columns); add(content, BorderLayout.CENTER);

        JLabel foot = new JLabel("CAMPUS QUEST  ·  대학교 → 졸업 → 취업", SwingConstants.CENTER);
        foot.setForeground(UniversityPixelTheme.TEXT_SUB);
        foot.setFont(UniversityPixelTheme.font(13, Font.BOLD));
        add(foot, BorderLayout.SOUTH);
    }

    private JPanel localCard() {
        JPanel card = card(UniversityPixelTheme.GOLD);
        JLabel eyebrow = label("STORY MODE", 14, UniversityPixelTheme.GOLD);
        JLabel title = label("캠퍼스 퀘스트", 22, UniversityPixelTheme.TEXT);
        JLabel symbol = label("▣  9 LEVELS", 24, UniversityPixelTheme.MINT);
        JLabel help = label("교양부터 취업까지, 퍼즐로 전투하세요.", 13, UniversityPixelTheme.TEXT_SUB);
        for (JLabel element : new JLabel[] { eyebrow, title, symbol, help }) card.add(element);
        card.add(Box.createVerticalGlue());
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(210, 42));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(8));
        JLabel note = label("로그인 없이 진행 가능", 12, UniversityPixelTheme.TEXT_SUB);
        card.add(note);
        return card;
    }

    private JPanel onlineCard() {
        JPanel card = card(UniversityPixelTheme.LINE);
        card.add(label("RANKED PvP", 14, UniversityPixelTheme.GOLD));
        card.add(label("온라인 계정", 21, UniversityPixelTheme.TEXT));
        card.add(Box.createVerticalStrut(13));
        card.add(label("이메일", 13, UniversityPixelTheme.TEXT_SUB));
        field(idField); card.add(idField);
        card.add(Box.createVerticalStrut(10));
        card.add(label("비밀번호", 13, UniversityPixelTheme.TEXT_SUB));
        field(passwordField); card.add(passwordField);
        card.add(Box.createVerticalStrut(15));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0)); buttons.setOpaque(false);
        buttons.add(onlineLoginButton); buttons.add(signUpButton);
        card.add(buttons);
        card.add(Box.createVerticalStrut(9));
        status.setForeground(UniversityPixelTheme.TEXT_SUB);
        status.setFont(UniversityPixelTheme.font(12, Font.PLAIN));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(status);
        return card;
    }

    private JPanel card(Color accent) {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UniversityPixelTheme.PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 3), new EmptyBorder(20, 17, 20, 17)));
        return panel;
    }
    private JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(UniversityPixelTheme.font(size, Font.BOLD)); label.setForeground(color);
        return label;
    }
    private void field(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setPreferredSize(new Dimension(245, 36));
        field.setFont(UniversityPixelTheme.font(14, Font.PLAIN));
        field.setForeground(UniversityPixelTheme.TEXT);
        field.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        field.setCaretColor(UniversityPixelTheme.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(5, 8, 5, 8)));
    }

    public void setLoginAction(ActionListener listener) { loginButton.addActionListener(listener); }
    public void setSignUpAction(ActionListener listener) { signUpButton.addActionListener(listener); }
    public String getEmail() { return idField.getText().trim(); }
    public char[] getPassword() { return passwordField.getPassword(); }
    public void clearPassword() { passwordField.setText(""); }
    public void setOnlineLoginAction(ActionListener listener) { onlineLoginButton.addActionListener(listener); }
    public void setOnlineAvailable(boolean available) {
        onlineLoginButton.setEnabled(available); signUpButton.setEnabled(available);
        if (!available) status.setText("온라인 설정이 없습니다. 로컬 시작을 이용하세요.");
    }
    public void setBusy(boolean busy) { onlineLoginButton.setEnabled(!busy); signUpButton.setEnabled(!busy); }
    public void setStatus(String text) { status.setText(text); }
}

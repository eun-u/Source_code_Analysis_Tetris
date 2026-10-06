package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class LoginPanel extends JPanel {

    private JTextField idField;
    private JPasswordField passwordField;

    private GameButton loginButton;
    private GameButton signUpButton;
    private final GameButton onlineLoginButton = new GameButton("온라인 로그인");
    private final JLabel status = new JLabel("스토리는 계정 없이 플레이할 수 있습니다.");

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(13, 23, 40));

        JPanel loginBox = new JPanel();
        loginBox.setOpaque(false);
        loginBox.setLayout(new BoxLayout(loginBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("TETRIS MONSTER");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField = new JTextField(15);
        passwordField = new JPasswordField(15);

        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());

        loginButton = new GameButton("로컬 시작");
        signUpButton = new GameButton("회원가입");

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBox.add(titleLabel);
        loginBox.add(Box.createVerticalStrut(15));
        JLabel localNotice = new JLabel("대학교 → 졸업 → 취업  /  퍼즐 전투 RPG");
        localNotice.setForeground(new Color(174, 207, 221));
        loginBox.add(localNotice);

        loginBox.add(Box.createVerticalStrut(15));
        loginBox.add(loginButton);

        loginBox.add(Box.createVerticalStrut(22));
        loginBox.add(new JLabel("온라인 PvP 계정 이메일"));
        loginBox.add(idField);
        loginBox.add(Box.createVerticalStrut(8));
        loginBox.add(new JLabel("비밀번호"));
        loginBox.add(passwordField);
        loginBox.add(Box.createVerticalStrut(12));
        onlineLoginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBox.add(onlineLoginButton);

        loginBox.add(Box.createVerticalStrut(5));
        loginBox.add(signUpButton);
        loginBox.add(Box.createVerticalStrut(12));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setForeground(new Color(174, 207, 221));
        loginBox.add(status);

        add(loginBox);
    }

    public void setLoginAction(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void setSignUpAction(ActionListener listener) {
        signUpButton.addActionListener(listener);
    }
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

package kr.ac.jbnu.se.tetris.ui;

import java.awt.Component;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

/** 성은 원본 로그인 화면의 중앙 정렬과 입력 순서를 유지한다. */
public final class LoginPanel extends JPanel implements Screen {
    private final JTextField idField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JLabel message = new JLabel("계정 인증 준비 중");
    private final GameButton localButton = new GameButton("로컬로 시작");

    public LoginPanel(Runnable login, Runnable signup) {
        super(new GridBagLayout());
        if (login == null || signup == null) throw new IllegalArgumentException("Actions are required");

        JPanel loginBox = new JPanel();
        loginBox.setLayout(new BoxLayout(loginBox, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("로그인");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField.setName("loginId");
        passwordField.setName("loginPassword");
        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());

        GameButton loginButton = new GameButton("로그인");
        GameButton signUpButton = new GameButton("회원가입");
        loginButton.setName("login");
        loginButton.setEnabled(false);
        loginButton.setToolTipText("계정 로그인 서비스 연결 준비 중입니다.");
        signUpButton.setName("signUp");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(event -> login.run());
        signUpButton.addActionListener(event -> signup.run());

        loginBox.add(titleLabel);
        loginBox.add(Box.createVerticalStrut(15));
        loginBox.add(new JLabel("ID"));
        loginBox.add(idField);
        loginBox.add(Box.createVerticalStrut(10));
        loginBox.add(new JLabel("Password"));
        loginBox.add(passwordField);
        loginBox.add(Box.createVerticalStrut(15));
        loginBox.add(loginButton);
        loginBox.add(Box.createVerticalStrut(5));
        loginBox.add(signUpButton);
        loginBox.add(Box.createVerticalStrut(8));
        message.setName("loginMessage");
        message.setFont(message.getFont().deriveFont(11f));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBox.add(message);
        loginBox.add(Box.createVerticalStrut(5));
        localButton.setName("localLogin");
        localButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        localButton.addActionListener(event -> login.run());
        loginBox.add(localButton);
        add(loginBox);
    }

    public void clearSecrets() { passwordField.setText(""); }
    public void setMessage(String value) {
        ScreenRouter.requireEdt();
        message.setText(value == null ? "" : value);
    }
    @Override public String getId() { return "login"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onExit() { clearSecrets(); }
}

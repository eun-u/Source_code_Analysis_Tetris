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

/** 성은 원본 가입 화면의 중앙 정렬과 입력 순서를 유지한다. */
public final class SignUpPanel extends JPanel implements Screen {
    private final JTextField idField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JPasswordField confirmField = new JPasswordField(15);
    private final JLabel message = new JLabel(" ");

    public SignUpPanel(Runnable register, Runnable back) {
        super(new GridBagLayout());
        if (register == null || back == null) throw new IllegalArgumentException("Actions are required");

        JPanel signUpBox = new JPanel();
        signUpBox.setLayout(new BoxLayout(signUpBox, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("회원가입");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField.setName("signupId");
        passwordField.setName("signupPassword");
        confirmField.setName("signupPasswordConfirm");
        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());
        confirmField.setMaximumSize(confirmField.getPreferredSize());

        GameButton registerButton = new GameButton("가입");
        GameButton backButton = new GameButton("Back");
        registerButton.setName("register");
        backButton.setName("signUpBack");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.addActionListener(event -> register.run());
        backButton.addActionListener(event -> back.run());

        signUpBox.add(titleLabel);
        signUpBox.add(Box.createVerticalStrut(15));
        signUpBox.add(new JLabel("ID"));
        signUpBox.add(idField);
        signUpBox.add(Box.createVerticalStrut(10));
        signUpBox.add(new JLabel("Password"));
        signUpBox.add(passwordField);
        signUpBox.add(Box.createVerticalStrut(10));
        signUpBox.add(new JLabel("Password 확인"));
        signUpBox.add(confirmField);
        signUpBox.add(Box.createVerticalStrut(15));
        signUpBox.add(registerButton);
        signUpBox.add(Box.createVerticalStrut(5));
        signUpBox.add(backButton);
        signUpBox.add(Box.createVerticalStrut(8));
        message.setName("signupMessage");
        message.setFont(message.getFont().deriveFont(11f));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpBox.add(message);
        add(signUpBox);
    }

    public void clearSecrets() {
        passwordField.setText("");
        confirmField.setText("");
    }
    public void setMessage(String value) {
        ScreenRouter.requireEdt();
        message.setText(value == null || value.isEmpty() ? " " : value);
    }
    @Override public String getId() { return "signup"; }
    @Override public JPanel getPanel() { return this; }
    @Override public void onExit() { clearSecrets(); }
}

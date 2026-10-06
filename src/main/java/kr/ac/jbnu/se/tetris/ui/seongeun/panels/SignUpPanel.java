package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class SignUpPanel extends JPanel {

    private JTextField idField;
    private JPasswordField passwordField;
    private JPasswordField passwordConfirmField;

    private GameButton registerButton;
    private GameButton backButton;

    public SignUpPanel() {
        setLayout(new GridBagLayout());

        JPanel signUpBox = new JPanel();
        signUpBox.setLayout(new BoxLayout(signUpBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("회원가입");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField = new JTextField(15);
        passwordField = new JPasswordField(15);
        passwordConfirmField = new JPasswordField(15);

        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());
        passwordConfirmField.setMaximumSize(passwordConfirmField.getPreferredSize());

        registerButton = new GameButton("가입");
        backButton = new GameButton("Back");

        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        signUpBox.add(titleLabel);
        signUpBox.add(Box.createVerticalStrut(15));

        signUpBox.add(new JLabel("이메일"));
        signUpBox.add(idField);

        signUpBox.add(Box.createVerticalStrut(10));

        signUpBox.add(new JLabel("Password"));
        signUpBox.add(passwordField);

        signUpBox.add(Box.createVerticalStrut(10));

        signUpBox.add(new JLabel("Password 확인"));
        signUpBox.add(passwordConfirmField);

        signUpBox.add(Box.createVerticalStrut(15));

        signUpBox.add(registerButton);
        signUpBox.add(Box.createVerticalStrut(5));
        signUpBox.add(backButton);

        add(signUpBox);
    }

    public void setRegisterAction(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    public void setBackAction(ActionListener listener) {
        backButton.addActionListener(listener);
    }
    public String getEmail() { return idField.getText().trim(); }
    public char[] getPassword() { return passwordField.getPassword(); }
    public char[] getPasswordConfirmation() { return passwordConfirmField.getPassword(); }
    public void clearPasswords() { passwordField.setText(""); passwordConfirmField.setText(""); }
    public void setBusy(boolean busy) { registerButton.setEnabled(!busy); }
}

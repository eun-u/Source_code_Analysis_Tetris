package kr.ac.jbnu.se.tetris.ui.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.components.GameButton;

public class LoginPanel extends JPanel {

    private JTextField idField;
    private JPasswordField passwordField;

    private GameButton loginButton;
    private GameButton signUpButton;

    public LoginPanel() {
        setLayout(new GridBagLayout());

        JPanel loginBox = new JPanel();
        loginBox.setLayout(new BoxLayout(loginBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("로그인");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField = new JTextField(15);
        passwordField = new JPasswordField(15);

        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());

        loginButton = new GameButton("로그인");
        signUpButton = new GameButton("회원가입");

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        add(loginBox);
    }

    public void setLoginAction(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void setSignUpAction(ActionListener listener) {
        signUpButton.addActionListener(listener);
    }

    
}
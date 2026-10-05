package kr.ac.jbnu.se.tetris.ui.components;

import java.awt.*;
import javax.swing.*;

public class FeverBar extends JPanel {

    private JLabel feverLabel;
    private JProgressBar feverProgressBar;

    public FeverBar(int currentFever, int maxFever) {
        setLayout(new BorderLayout(5, 0));

        feverLabel = new JLabel("FEVER");

        feverProgressBar = new JProgressBar(0, maxFever);
        feverProgressBar.setValue(currentFever);
        feverProgressBar.setStringPainted(true);
        feverProgressBar.setString(currentFever + " / " + maxFever);

        add(feverLabel, BorderLayout.WEST);
        add(feverProgressBar, BorderLayout.CENTER);
    }

    public void setFever(int currentFever, int maxFever) {
        feverProgressBar.setMaximum(maxFever);
        feverProgressBar.setValue(currentFever);
        feverProgressBar.setString(currentFever + " / " + maxFever);
    }
}
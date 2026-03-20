package edu.hitsz.application;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * 简易开始菜单：选择难度与音效开关，点击难度按钮开始游戏。
 */
public class StartMenuPanel extends JPanel {

    private final JComboBox<String> soundBox;
    private final BiConsumer<Difficulty, Boolean> onStart;

    public StartMenuPanel(BiConsumer<Difficulty, Boolean> onStart) {
        this.onStart = onStart;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0;

        JButton easy = new JButton("简单模式");
        JButton normal = new JButton("普通模式");
        JButton hard = new JButton("困难模式");

        soundBox = new JComboBox<>(new String[]{"开", "关"});
        JPanel soundPanel = new JPanel(new FlowLayout());
        soundPanel.add(new JLabel("音效"));
        soundPanel.add(soundBox);

        c.gridy = 0; add(easy, c);
        c.gridy = 1; add(normal, c);
        c.gridy = 2; add(hard, c);
        c.gridy = 3; add(soundPanel, c);

        easy.addActionListener(e -> start(Difficulty.EASY));
        normal.addActionListener(e -> start(Difficulty.NORMAL));
        hard.addActionListener(e -> start(Difficulty.HARD));
    }

    private void start(Difficulty d) {
        boolean soundOn = soundBox.getSelectedIndex() == 0;
        onStart.accept(d, soundOn);
    }
}


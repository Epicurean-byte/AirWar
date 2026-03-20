package edu.hitsz.application;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * 程序入口
 * @author hitsz
 */
public class Main {

    public static final int WINDOW_WIDTH = 512;
    public static final int WINDOW_HEIGHT = 768;
    public static final boolean SOUND_ENABLED = true; // 简单开关，可在后续界面配置

    public static void main(String[] args) {

        System.out.println("Hello Aircraft War");

        // 获得屏幕的分辨率，初始化 Frame
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        JFrame frame = new JFrame("Aircraft War");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setResizable(false);
        //设置窗口的大小和位置,居中放置
        frame.setBounds(((int) screenSize.getWidth() - WINDOW_WIDTH) / 2, 0,
                WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 启动菜单
        BiConsumer<Difficulty, Boolean> starter = (difficulty, soundOn) -> {
            frame.getContentPane().removeAll();
            Game game = new Game(difficulty, soundOn);
            frame.add(game);
            frame.revalidate();
            frame.repaint();
            game.action();
        };
        StartMenuPanel menu = new StartMenuPanel(starter);
        frame.add(menu);
        frame.setVisible(true);
    }
}

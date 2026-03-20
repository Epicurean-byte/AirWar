package edu.hitsz.audio;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 轻量音频管理器：基于提供的 MusicThread 实现背景音乐循环、一次性音效播放及停止控制。
 * 资源文件默认位于 src/videos/ 目录下，可根据需要调整路径。
 */
public final class SoundManager {

    public enum Effect {
        BULLET_HIT("src/videos/bullet_hit.wav"),
        BOMB_EXPLOSION("src/videos/bomb_explosion.wav"),
        GET_SUPPLY("src/videos/get_supply.wav"),
        GAME_OVER("src/videos/game_over.wav");

        final String path;
        Effect(String p) { this.path = p; }
    }

    private static final String BGM = "src/videos/bgm.wav";
    private static final String BOSS_BGM = "src/videos/bgm_boss.wav";

    private static volatile boolean enabled = true;
    private static LoopPlayer bgmPlayer;
    private static LoopPlayer bossPlayer;

    private SoundManager() {}

    public static void init(boolean on) {
        enabled = on;
        if (!enabled) stopAll();
    }

    public static void startBgm() {
        if (!enabled) return;
        if (bgmPlayer == null) bgmPlayer = new LoopPlayer(BGM);
        bgmPlayer.start();
    }

    public static void stopBgm() {
        if (bgmPlayer != null) bgmPlayer.stop();
    }

    public static void startBossBgm() {
        if (!enabled) return;
        if (bossPlayer == null) bossPlayer = new LoopPlayer(BOSS_BGM);
        bossPlayer.start();
    }

    public static void stopBossBgm() {
        if (bossPlayer != null) bossPlayer.stop();
    }

    public static void stopAll() {
        stopBgm();
        stopBossBgm();
    }

    public static void playEffect(Effect e) {
        if (!enabled) return;
        new MusicThread(e.path).start();
    }

    /** 背景循环播放器，反复拉起 MusicThread 播放直到停止。 */
    private static final class LoopPlayer {
        private final String file;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Thread thread;

        private LoopPlayer(String file) { this.file = file; }

        void start() {
            if (running.get()) return;
            running.set(true);
            thread = new Thread(() -> {
                while (running.get()) {
                    MusicThread t = new MusicThread(file);
                    t.setDaemon(true);
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "loop-player-" + file);
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            running.set(false);
            if (thread != null) thread.interrupt();
        }
    }
}


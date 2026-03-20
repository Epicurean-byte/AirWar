package edu.hitsz.aircraftwar.android.game;

import android.os.SystemClock;

public final class GameLoopThread extends Thread {
    public interface FrameCallback {
        void onFrame(long nowMs);
    }

    private final int frameIntervalMs;
    private final FrameCallback frameCallback;
    private volatile boolean running = true;

    public GameLoopThread(int frameIntervalMs, FrameCallback frameCallback) {
        super("aircraft-war-loop");
        this.frameIntervalMs = Math.max(16, frameIntervalMs);
        this.frameCallback = frameCallback;
    }

    @Override
    public void run() {
        while (running) {
            long frameStart = SystemClock.elapsedRealtime();
            frameCallback.onFrame(frameStart);
            long frameCost = SystemClock.elapsedRealtime() - frameStart;
            long sleepMs = frameIntervalMs - frameCost;
            if (sleepMs > 0) {
                SystemClock.sleep(sleepMs);
            }
        }
    }

    public void requestStop() {
        running = false;
        interrupt();
    }
}

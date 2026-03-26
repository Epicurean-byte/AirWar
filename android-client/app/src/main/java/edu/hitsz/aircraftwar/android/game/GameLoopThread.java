package edu.hitsz.aircraftwar.android.game;

import android.os.Process;
import android.os.SystemClock;

public final class GameLoopThread extends Thread {
    private static final int MAX_CATCH_UP_STEPS = 3;

    public interface FrameCallback {
        void onFrame(long nowMs, int updateCount);
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
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        long previousFrameAt = SystemClock.elapsedRealtime();
        long accumulatorMs = 0L;
        while (running) {
            long frameStart = SystemClock.elapsedRealtime();
            long elapsedMs = Math.min(frameIntervalMs * MAX_CATCH_UP_STEPS, Math.max(0L, frameStart - previousFrameAt));
            previousFrameAt = frameStart;
            accumulatorMs += elapsedMs;

            int updateCount = 0;
            while (accumulatorMs >= frameIntervalMs && updateCount < MAX_CATCH_UP_STEPS) {
                accumulatorMs -= frameIntervalMs;
                updateCount++;
            }
            if (updateCount == MAX_CATCH_UP_STEPS && accumulatorMs >= frameIntervalMs) {
                accumulatorMs %= frameIntervalMs;
            }

            frameCallback.onFrame(frameStart, updateCount);
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

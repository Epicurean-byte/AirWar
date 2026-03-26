package edu.hitsz.aircraftwar.android.game;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import edu.hitsz.aircraftwar.android.audio.AndroidAudioPlayer;
import edu.hitsz.game.core.engine.GameEngine;
import edu.hitsz.game.core.engine.GameSnapshot;
import edu.hitsz.game.core.event.GameEvent;
import edu.hitsz.game.core.mode.Difficulty;

public class AircraftWarSurfaceView extends SurfaceView implements SurfaceHolder.Callback, GameLoopThread.FrameCallback {
    private static final int FRAME_INTERVAL_MS = 16;

    public interface GameOverListener {
        void onGameOver(int score);
    }

    public interface HeroMoveListener {
        void onHeroMove(float worldX, float worldY);
    }

    private final Difficulty difficulty;
    private final BitmapSkinManager skinManager;
    private final AndroidGameRenderer renderer;
    private final AndroidAudioPlayer audioPlayer;

    private GameEngine gameEngine;
    private GameLoopThread gameLoopThread;
    private boolean surfaceReady = false;
    private int warmedSurfaceWidth = -1;
    private int warmedSurfaceHeight = -1;
//    private GameViewport viewport = GameViewport.fill(1, 1, BitmapSkinManager.DESKTOP_WORLD_WIDTH, BitmapSkinManager.DESKTOP_WORLD_HEIGHT);
    private boolean gameOverNotified = false;
    private GameOverListener gameOverListener;
    private HeroMoveListener heroMoveListener;
    private volatile GameSnapshot latestSnapshot;
    private GameViewport viewport = GameViewport.fit(1, 1, BitmapSkinManager.DESKTOP_WORLD_WIDTH, BitmapSkinManager.DESKTOP_WORLD_HEIGHT);

    public AircraftWarSurfaceView(Context context, Difficulty difficulty) {
        this(context, null, difficulty);
    }

    public AircraftWarSurfaceView(Context context, @Nullable AttributeSet attrs, Difficulty difficulty) {
        super(context, attrs);
        this.difficulty = difficulty;
        this.skinManager = new BitmapSkinManager(context);
        this.renderer = new AndroidGameRenderer(skinManager);
        this.audioPlayer = new AndroidAudioPlayer(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        if (hasValidSurfaceSize()) {
            updateViewport();
            ensureEngine();
            startLoop();
            audioPlayer.startStageBgm();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (width > 0 && height > 0) {
            updateViewport();
            ensureEngine();
            startLoop();
            audioPlayer.startStageBgm();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
        audioPlayer.pauseAll();
    }

    @Override
    public void onFrame(long nowMs, int updateCount) {
        if (gameEngine == null || !surfaceReady) {
            return;
        }
        for (int i = 0; i < updateCount; i++) {
            gameEngine.update(nowMs);
        }
        for (GameEvent event : gameEngine.drainEvents()) {
            audioPlayer.playEvent(event);
        }
        GameSnapshot snapshot = gameEngine.snapshot();
//         viewport = GameViewport.fill(getWidth(), getHeight(), snapshot.getWorldWidth(), snapshot.getWorldHeight());
        latestSnapshot = snapshot;
        if (snapshot.isGameOver() && !gameOverNotified) {
            gameOverNotified = true;
            if (gameOverListener != null) {
                gameOverListener.onGameOver(snapshot.getScore());
            }
        }
        viewport = GameViewport.fit(getWidth(), getHeight(), snapshot.getWorldWidth(), snapshot.getWorldHeight());

        Canvas canvas = null;
        try {
            try {
                canvas = getHolder().lockHardwareCanvas();
            } catch (IllegalStateException ignored) {
                canvas = getHolder().lockCanvas();
            }
            if (canvas != null) {
                renderer.render(canvas, snapshot, viewport);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameEngine == null) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float worldX = viewport.screenToWorldX(event.getX());
            float worldY = viewport.screenToWorldY(event.getY());
            gameEngine.moveHeroTo(
                    worldX,
                    worldY
            );
            if (heroMoveListener != null) {
                heroMoveListener.onHeroMove(worldX, worldY);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void resumeGame() {
        if (surfaceReady && hasValidSurfaceSize()) {
            updateViewport();
            startLoop();
            audioPlayer.startStageBgm();
        }
    }

    public void pauseGame() {
        stopLoop();
        audioPlayer.pauseAll();
    }

    public void release() {
        stopLoop();
        audioPlayer.release();
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    public void setHeroMoveListener(HeroMoveListener listener) {
        this.heroMoveListener = listener;
    }

    @Nullable
    public GameSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    private void ensureEngine() {
        if (gameEngine == null) {
            gameEngine = new GameEngine(difficulty, skinManager.createDesktopSessionConfig(), FRAME_INTERVAL_MS);
            gameOverNotified = false;
        }
    }

    private boolean hasValidSurfaceSize() {
        return getWidth() > 0 && getHeight() > 0;
    }

    private void updateViewport() {
        viewport = GameViewport.fill(
                getWidth(),
                getHeight(),
                BitmapSkinManager.DESKTOP_WORLD_WIDTH,
                BitmapSkinManager.DESKTOP_WORLD_HEIGHT
        );
        warmUpResourcesIfNeeded();
    }

    private void warmUpResourcesIfNeeded() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width == warmedSurfaceWidth && height == warmedSurfaceHeight) {
            return;
        }
        skinManager.warmUp(viewport, difficulty);
        warmedSurfaceWidth = width;
        warmedSurfaceHeight = height;
    }

    private void startLoop() {
        if (gameLoopThread != null) {
            return;
        }
        ensureEngine();
        if (gameEngine == null) {
            return;
        }
        gameLoopThread = new GameLoopThread(FRAME_INTERVAL_MS, this);
        gameLoopThread.start();
    }

    private void stopLoop() {
        if (gameLoopThread == null) {
            return;
        }
        gameLoopThread.requestStop();
        try {
            gameLoopThread.join(300);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        gameLoopThread = null;
    }
}

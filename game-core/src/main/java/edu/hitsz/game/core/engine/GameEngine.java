package edu.hitsz.game.core.engine;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.event.GameEvent;
import edu.hitsz.game.core.mode.Difficulty;

import java.util.List;

public final class GameEngine {
    private final GameWorld world;
    private final int frameIntervalMs;

    public GameEngine(Difficulty difficulty, int worldWidth, int worldHeight, int frameIntervalMs) {
        this.world = GameWorld.create(difficulty, worldWidth, worldHeight);
        this.frameIntervalMs = Math.max(16, frameIntervalMs);
    }

    public GameEngine(Difficulty difficulty, GameSessionConfig sessionConfig, int frameIntervalMs) {
        this.world = GameWorld.create(difficulty, sessionConfig);
        this.frameIntervalMs = Math.max(16, frameIntervalMs);
    }

    public void update(long now) {
        world.update(frameIntervalMs, now);
    }

    public void moveHeroTo(float x, float y) {
        world.moveHeroTo(x, y);
    }

    public GameSnapshot snapshot() {
        return world.snapshot();
    }

    public List<GameEvent> drainEvents() {
        return world.drainEvents();
    }

    public GameWorld getWorld() {
        return world;
    }

    public int getFrameIntervalMs() {
        return frameIntervalMs;
    }
}

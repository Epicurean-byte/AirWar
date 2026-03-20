package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;

public final class SpawnContext {
    private final int timeMs;
    private final int currentEnemyCount;
    private final int maxEnemyCount;
    private final GameSessionConfig sessionConfig;

    public SpawnContext(int timeMs, int currentEnemyCount, int maxEnemyCount, GameSessionConfig sessionConfig) {
        this.timeMs = timeMs;
        this.currentEnemyCount = currentEnemyCount;
        this.maxEnemyCount = maxEnemyCount;
        this.sessionConfig = sessionConfig;
    }

    public int getTimeMs() {
        return timeMs;
    }

    public int getCurrentEnemyCount() {
        return currentEnemyCount;
    }

    public int getMaxEnemyCount() {
        return maxEnemyCount;
    }

    public GameSessionConfig getSessionConfig() {
        return sessionConfig;
    }
}

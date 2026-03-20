package edu.hitsz.game.core.mode;

import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.entity.BossEnemy;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;

public abstract class AbstractGameMode {
    protected final Difficulty difficulty;

    protected AbstractGameMode(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public final void apply(GameWorld world) {
        world.setEnemyMaxNumber(initialEnemyMaxNumber());
        world.setCycleDuration(initialCycleDuration());
        world.enableBoss(isBossEnabled());
        if (isBossEnabled()) {
            world.setBossScoreThreshold(initialBossScoreThreshold());
            world.resetNextBossScore();
        }
        EnemyFactoryManager manager = buildEnemyFactoryManager();
        manager.setProbabilities(initialEliteProbability(), initialSuperEliteProbability());
        world.setEnemyFactoryManager(manager);
        world.setBossEnemyFactory(buildBossFactory());
        afterApplied(world);
    }

    public final void update(GameWorld world, int timeMs, int score) {
        adjustDifficulty(world, timeMs, score);
    }

    public void onBossSpawn(GameWorld world, BossEnemy boss, int spawnIndex) {
        adjustBossOnSpawn(world, boss, spawnIndex);
    }

    protected abstract int initialEnemyMaxNumber();

    protected abstract int initialCycleDuration();

    protected abstract double initialEliteProbability();

    protected abstract double initialSuperEliteProbability();

    protected abstract int initialBossScoreThreshold();

    protected abstract boolean isBossEnabled();

    protected abstract EnemyFactoryManager buildEnemyFactoryManager();

    protected abstract BossEnemyFactory buildBossFactory();

    protected void afterApplied(GameWorld world) {
    }

    protected abstract void adjustDifficulty(GameWorld world, int timeMs, int score);

    protected void adjustBossOnSpawn(GameWorld world, BossEnemy boss, int spawnIndex) {
    }
}

package edu.hitsz.game.core.mode;

import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.entity.BossEnemy;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EliteEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;
import edu.hitsz.game.core.factory.MobEnemyFactory;
import edu.hitsz.game.core.factory.SuperEliteEnemyFactory;

public class NormalGameMode extends AbstractGameMode {
    private int stage = 0;

    public NormalGameMode() {
        super(Difficulty.NORMAL);
    }

    @Override
    protected int initialEnemyMaxNumber() {
        return 6;
    }

    @Override
    protected int initialCycleDuration() {
        return 320;
    }

    @Override
    protected double initialEliteProbability() {
        return 0.25;
    }

    @Override
    protected double initialSuperEliteProbability() {
        return 0.10;
    }

    @Override
    protected int initialBossScoreThreshold() {
        return 220;
    }

    @Override
    protected boolean isBossEnabled() {
        return true;
    }

    @Override
    protected EnemyFactoryManager buildEnemyFactoryManager() {
        return new EnemyFactoryManager(
                new MobEnemyFactory(8, 60),
                new EliteEnemyFactory(6, 140),
                new SuperEliteEnemyFactory(4, 200),
                initialEliteProbability(),
                initialSuperEliteProbability()
        );
    }

    @Override
    protected BossEnemyFactory buildBossFactory() {
        return new BossEnemyFactory(550, 5);
    }

    @Override
    protected void adjustDifficulty(GameWorld world, int timeMs, int score) {
        int newStage = timeMs / 20000;
        if (newStage > stage) {
            stage = newStage;
            world.setEnemyMaxNumber(Math.min(9, world.getEnemyMaxNumber() + 1));
            world.setCycleDuration(Math.max(220, world.getCycleDuration() - 20));
            world.getEnemyFactoryManager().setProbabilities(
                    Math.min(0.38, world.getEnemyFactoryManager().getEliteProbability() + 0.02),
                    Math.min(0.16, world.getEnemyFactoryManager().getSuperEliteProbability() + 0.01)
            );
        }
    }

    @Override
    protected void adjustBossOnSpawn(GameWorld world, BossEnemy boss, int spawnIndex) {
        // Reserved for boss-side scaling hooks.
    }
}

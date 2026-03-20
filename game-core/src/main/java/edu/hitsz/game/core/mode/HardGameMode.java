package edu.hitsz.game.core.mode;

import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.entity.BossEnemy;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EliteEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;
import edu.hitsz.game.core.factory.MobEnemyFactory;
import edu.hitsz.game.core.factory.SuperEliteEnemyFactory;

public class HardGameMode extends AbstractGameMode {
    private int stage = 0;
    private final int baseBossHp = 650;
    private final int bossHpGrowth = 180;

    public HardGameMode() {
        super(Difficulty.HARD);
    }

    @Override
    protected int initialEnemyMaxNumber() {
        return 7;
    }

    @Override
    protected int initialCycleDuration() {
        return 280;
    }

    @Override
    protected double initialEliteProbability() {
        return 0.32;
    }

    @Override
    protected double initialSuperEliteProbability() {
        return 0.16;
    }

    @Override
    protected int initialBossScoreThreshold() {
        return 200;
    }

    @Override
    protected boolean isBossEnabled() {
        return true;
    }

    @Override
    protected EnemyFactoryManager buildEnemyFactoryManager() {
        return new EnemyFactoryManager(
                new MobEnemyFactory(9, 70),
                new EliteEnemyFactory(7, 160),
                new SuperEliteEnemyFactory(5, 220),
                initialEliteProbability(),
                initialSuperEliteProbability()
        );
    }

    @Override
    protected BossEnemyFactory buildBossFactory() {
        return new BossEnemyFactory(baseBossHp, 6);
    }

    @Override
    protected void adjustDifficulty(GameWorld world, int timeMs, int score) {
        int newStage = timeMs / 15000;
        if (newStage > stage) {
            stage = newStage;
            world.setEnemyMaxNumber(Math.min(11, world.getEnemyMaxNumber() + 1));
            world.setCycleDuration(Math.max(200, world.getCycleDuration() - 25));
            world.getEnemyFactoryManager().setProbabilities(
                    Math.min(0.48, world.getEnemyFactoryManager().getEliteProbability() + 0.03),
                    Math.min(0.25, world.getEnemyFactoryManager().getSuperEliteProbability() + 0.015)
            );
            if (stage % 2 == 1) {
                world.setBossScoreThreshold(Math.max(150, world.getBossScoreThreshold() - 10));
            }
        }
    }

    @Override
    protected void adjustBossOnSpawn(GameWorld world, BossEnemy boss, int spawnIndex) {
        int extraHp = bossHpGrowth * Math.max(0, spawnIndex - 1);
        if (extraHp > 0) {
            boss.increaseHp(extraHp);
        }
        if (world.getBossEnemyFactory() != null) {
            world.getBossEnemyFactory().setHp(baseBossHp + bossHpGrowth * spawnIndex);
        }
    }
}

package edu.hitsz.application.mode;

import edu.hitsz.EnemyFactory.BossEnemyFactory;
import edu.hitsz.EnemyFactory.EliteEnemyFactory;
import edu.hitsz.EnemyFactory.Manager.EnemyFactoryManager;
import edu.hitsz.EnemyFactory.MobEnemyFactory;
import edu.hitsz.EnemyFactory.SuperEliteEnemyFactory;
import edu.hitsz.application.Difficulty;
import edu.hitsz.application.Game;

public class EasyGameMode extends AbstractGameMode {

    public EasyGameMode() {
        super(Difficulty.EASY);
    }

    @Override
    protected int initialEnemyMaxNumber() {
        return 5;
    }

    @Override
    protected int initialCycleDuration() {
        return 380;
    }

    @Override
    protected double initialEliteProbability() {
        return 0.18;
    }

    @Override
    protected double initialSuperEliteProbability() {
        return 0.05;
    }

    @Override
    protected int initialBossScoreThreshold() {
        return Integer.MAX_VALUE; // 不生成Boss
    }

    @Override
    protected boolean isBossEnabled() {
        return false;
    }

    @Override
    protected EnemyFactoryManager buildEnemyFactoryManager() {
        return new EnemyFactoryManager(
                new MobEnemyFactory(6, 45),
                new EliteEnemyFactory(5, 100),
                new SuperEliteEnemyFactory(4, 150),
                initialEliteProbability(),
                initialSuperEliteProbability()
        );
    }

    @Override
    protected BossEnemyFactory buildBossFactory() {
        return new BossEnemyFactory(500, 5);
    }

    @Override
    protected void adjustDifficulty(Game game, int timeMs, int score) {
        // 简单模式不随时间变化
    }
}


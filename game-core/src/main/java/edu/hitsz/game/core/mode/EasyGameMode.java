package edu.hitsz.game.core.mode;

import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EliteEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;
import edu.hitsz.game.core.factory.MobEnemyFactory;

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
        return 440;
    }

    @Override
    protected double initialEliteProbability() {
        return 0.18;
    }

    @Override
    protected double initialSuperEliteProbability() {
        return 0.0;
    }

    @Override
    protected int initialBossScoreThreshold() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean isBossEnabled() {
        return false;
    }

    @Override
    protected EnemyFactoryManager buildEnemyFactoryManager() {
        MobEnemyFactory mobFactory = new MobEnemyFactory(5, 45);
        return new EnemyFactoryManager(
                mobFactory,
                new EliteEnemyFactory(4, 100),
                mobFactory,
                initialEliteProbability(),
                initialSuperEliteProbability()
        );
    }

    @Override
    protected BossEnemyFactory buildBossFactory() {
        return new BossEnemyFactory(500, 5);
    }

    @Override
    protected void adjustDifficulty(GameWorld world, int timeMs, int score) {
        // Easy mode keeps a stable curve.
    }
}

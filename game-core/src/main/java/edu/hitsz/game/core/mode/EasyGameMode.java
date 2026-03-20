package edu.hitsz.game.core.mode;

import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EliteEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;
import edu.hitsz.game.core.factory.MobEnemyFactory;
import edu.hitsz.game.core.factory.SuperEliteEnemyFactory;

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
        return Integer.MAX_VALUE;
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
    protected void adjustDifficulty(GameWorld world, int timeMs, int score) {
        // Easy mode keeps a stable curve.
    }
}

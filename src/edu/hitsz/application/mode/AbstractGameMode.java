package edu.hitsz.application.mode;

import edu.hitsz.EnemyFactory.BossEnemyFactory;
import edu.hitsz.EnemyFactory.Manager.EnemyFactoryManager;
import edu.hitsz.application.Difficulty;
import edu.hitsz.application.Game;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;

/**
 * 游戏模式模板：定义配置和随时间调整的算法骨架。
 */
public abstract class AbstractGameMode {

    protected final Difficulty difficulty;

    protected AbstractGameMode(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /** 初始化游戏参数（模板方法）。 */
    public final void apply(Game game) {
        game.setEnemyMaxNumber(initialEnemyMaxNumber());
        game.setCycleDuration(initialCycleDuration());
        game.enableBoss(isBossEnabled());
        if (isBossEnabled()) {
            game.setBossScoreThreshold(initialBossScoreThreshold());
            game.resetNextBossScore();
        }
        EnemyFactoryManager manager = buildEnemyFactoryManager();
        manager.setProbabilities(initialEliteProbability(), initialSuperEliteProbability());
        game.setEnemyFactoryManager(manager);
        game.setBossEnemyFactory(buildBossFactory());
        afterApplied(game);
    }

    /** 周期性调用，用于提升难度。 */
    public final void update(Game game, int timeMs, int score) {
        adjustDifficulty(game, timeMs, score);
    }

    /** Boss 出现时回调，可调整其属性或下一次阈值。 */
    public void onBossSpawn(Game game, BossEnemy boss, int spawnIndex) {
        adjustBossOnSpawn(game, boss, spawnIndex);
    }

    protected abstract int initialEnemyMaxNumber();
    protected abstract int initialCycleDuration();
    protected abstract double initialEliteProbability();
    protected abstract double initialSuperEliteProbability();
    protected abstract int initialBossScoreThreshold();
    protected abstract boolean isBossEnabled();
    protected abstract EnemyFactoryManager buildEnemyFactoryManager();
    protected abstract BossEnemyFactory buildBossFactory();

    protected void afterApplied(Game game) {
        // 默认无额外操作
    }

    protected abstract void adjustDifficulty(Game game, int timeMs, int score);

    protected void adjustBossOnSpawn(Game game, BossEnemy boss, int spawnIndex) {
        // 默认无额外操作
    }
}


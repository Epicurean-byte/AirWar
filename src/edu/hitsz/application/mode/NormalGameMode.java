package edu.hitsz.application.mode;

import edu.hitsz.EnemyFactory.BossEnemyFactory;
import edu.hitsz.EnemyFactory.EliteEnemyFactory;
import edu.hitsz.EnemyFactory.Manager.EnemyFactoryManager;
import edu.hitsz.EnemyFactory.MobEnemyFactory;
import edu.hitsz.EnemyFactory.SuperEliteEnemyFactory;
import edu.hitsz.application.Difficulty;
import edu.hitsz.application.Game;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;

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
    protected void adjustDifficulty(Game game, int timeMs, int score) {
        int newStage = timeMs / 20000; // 每20秒提升一次
        if (newStage > stage) {
            stage = newStage;
            int newLimit = Math.min(9, game.getEnemyMaxNumber() + 1);
            int newCycle = Math.max(220, game.getCycleDuration() - 20);
            double newElite = Math.min(0.38, game.getEnemyFactoryManager().getEliteProbability() + 0.02);
            double newSuper = Math.min(0.16, game.getEnemyFactoryManager().getSuperEliteProbability() + 0.01);
            game.setEnemyMaxNumber(newLimit);
            game.setCycleDuration(newCycle);
            game.getEnemyFactoryManager().setProbabilities(newElite, newSuper);
            System.out.printf("[NORMAL] 难度提升至阶段%d -> 敌机上限:%d, 产生周期:%dms, 精英概率:%.2f, 超级概率:%.2f%n",
                    stage, newLimit, newCycle, newElite, newSuper);
        }
    }

    @Override
    protected void adjustBossOnSpawn(Game game, BossEnemy boss, int spawnIndex) {
        System.out.printf("[NORMAL] Boss 第 %d 次出现，当前血量 %d%n", spawnIndex, boss.getHp());
    }
}


package edu.hitsz.application.mode;

import edu.hitsz.EnemyFactory.BossEnemyFactory;
import edu.hitsz.EnemyFactory.EliteEnemyFactory;
import edu.hitsz.EnemyFactory.Manager.EnemyFactoryManager;
import edu.hitsz.EnemyFactory.MobEnemyFactory;
import edu.hitsz.EnemyFactory.SuperEliteEnemyFactory;
import edu.hitsz.application.Difficulty;
import edu.hitsz.application.Game;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;

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
    protected void adjustDifficulty(Game game, int timeMs, int score) {
        int newStage = timeMs / 15000; // 每15秒提高一次
        if (newStage > stage) {
            stage = newStage;
            int newLimit = Math.min(11, game.getEnemyMaxNumber() + 1);
            int newCycle = Math.max(200, game.getCycleDuration() - 25);
            double newElite = Math.min(0.48, game.getEnemyFactoryManager().getEliteProbability() + 0.03);
            double newSuper = Math.min(0.25, game.getEnemyFactoryManager().getSuperEliteProbability() + 0.015);
            game.setEnemyMaxNumber(newLimit);
            game.setCycleDuration(newCycle);
            game.getEnemyFactoryManager().setProbabilities(newElite, newSuper);
            if (stage % 2 == 1) {
                int newThreshold = Math.max(150, game.getBossScoreThreshold() - 10);
                game.setBossScoreThreshold(newThreshold);
                System.out.printf("[HARD] 阶段%d: Boss分数阈值降低至 %d%n", stage, newThreshold);
            }
            System.out.printf("[HARD] 难度提升至阶段%d -> 敌机上限:%d, 产生周期:%dms, 精英概率:%.2f, 超级概率:%.2f%n",
                    stage, newLimit, newCycle, newElite, newSuper);
        }
    }

    @Override
    protected void adjustBossOnSpawn(Game game, BossEnemy boss, int spawnIndex) {
        int extra = bossHpGrowth * Math.max(0, spawnIndex - 1);
        if (extra > 0) {
            boss.increaseHp(extra);
        }
        BossEnemyFactory factory = game.getBossEnemyFactory();
        if (factory != null) {
            factory.setHp(baseBossHp + bossHpGrowth * spawnIndex);
        }
        System.out.printf("[HARD] Boss 第 %d 次出现，血量提升至 %d，下次基础血量:%d%n",
                spawnIndex, boss.getHp(), baseBossHp + bossHpGrowth * spawnIndex);
    }
}

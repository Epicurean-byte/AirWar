package edu.hitsz.EnemyFactory.Manager;

import edu.hitsz.EnemyFactory.EnemyFactory;
import edu.hitsz.aircraft.AbstractAircraft;
import java.util.Random;

public final class EnemyFactoryManager {

    private final EnemyFactory mobFactory;
    private final EnemyFactory eliteFactory;
    private final EnemyFactory superEliteFactory;
    private double eliteProbability; // 例如 0.3
    private double superEliteProbability; // e.g., 0.1
    private final Random random;

    public EnemyFactoryManager(EnemyFactory mobFactory,
                               EnemyFactory eliteFactory,
                               EnemyFactory superEliteFactory,
                               double eliteProbability,
                               double superEliteProbability) {
        this.mobFactory = mobFactory;
        this.eliteFactory = eliteFactory;
        this.superEliteFactory = superEliteFactory;
        this.eliteProbability = eliteProbability;
        this.superEliteProbability = superEliteProbability;
        this.random = new Random();
    }

    // 返回一个创建好的敌机；
    public AbstractAircraft createEnemy(ManagerContext context) {
        double p = random.nextDouble();
        if (p < superEliteProbability) {
            return superEliteFactory.createEnemy(context);
        } else if (p < superEliteProbability + eliteProbability) {
            return eliteFactory.createEnemy(context);
        } else {
            return mobFactory.createEnemy(context);
        }
    }

    public void setProbabilities(double eliteProbability, double superEliteProbability) {
        this.eliteProbability = Math.max(0, Math.min(1, eliteProbability));
        this.superEliteProbability = Math.max(0, Math.min(1, superEliteProbability));
    }

    public double getEliteProbability() {
        return eliteProbability;
    }

    public double getSuperEliteProbability() {
        return superEliteProbability;
    }
}

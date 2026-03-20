package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.entity.AbstractEnemy;

import java.util.Random;

public final class EnemyFactoryManager {
    private final EnemyFactory mobFactory;
    private final EnemyFactory eliteFactory;
    private final EnemyFactory superEliteFactory;
    private final Random random = new Random();
    private double eliteProbability;
    private double superEliteProbability;

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
    }

    public AbstractEnemy createEnemy(SpawnContext context) {
        double p = random.nextDouble();
        if (p < superEliteProbability) {
            return superEliteFactory.createEnemy(context);
        }
        if (p < superEliteProbability + eliteProbability) {
            return eliteFactory.createEnemy(context);
        }
        return mobFactory.createEnemy(context);
    }

    public void setProbabilities(double eliteProbability, double superEliteProbability) {
        this.eliteProbability = Math.max(0.0, Math.min(1.0, eliteProbability));
        this.superEliteProbability = Math.max(0.0, Math.min(1.0, superEliteProbability));
    }

    public double getEliteProbability() {
        return eliteProbability;
    }

    public double getSuperEliteProbability() {
        return superEliteProbability;
    }
}

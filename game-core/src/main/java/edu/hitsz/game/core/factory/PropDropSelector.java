package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.entity.AbstractAircraft;
import edu.hitsz.game.core.entity.AbstractProp;
import edu.hitsz.game.core.entity.MobEnemy;

import java.util.Random;

public class PropDropSelector {
    private final PropFactory bloodFactory;
    private final PropFactory bombFactory;
    private final PropFactory fireFactory;
    private final PropFactory superFireFactory;
    private final Random random = new Random();
    private final double dropRate;
    private final double bloodRatio;
    private final double bombRatio;
    private final double fireRatio;
    private final double superFireRatio;

    public PropDropSelector(PropFactory bloodFactory,
                            PropFactory bombFactory,
                            PropFactory fireFactory,
                            PropFactory superFireFactory,
                            double dropRate,
                            double bloodRatio,
                            double bombRatio,
                            double fireRatio,
                            double superFireRatio) {
        double sum = bloodRatio + bombRatio + fireRatio + superFireRatio;
        this.bloodFactory = bloodFactory;
        this.bombFactory = bombFactory;
        this.fireFactory = fireFactory;
        this.superFireFactory = superFireFactory;
        this.dropRate = dropRate;
        this.bloodRatio = bloodRatio / sum;
        this.bombRatio = bombRatio / sum;
        this.fireRatio = fireRatio / sum;
        this.superFireRatio = superFireRatio / sum;
    }

    public AbstractProp createFor(AbstractAircraft deadEnemy, GameSessionConfig config) {
        if (deadEnemy instanceof MobEnemy) {
            return null;
        }
        if (random.nextDouble() >= dropRate) {
            return null;
        }
        float x = deadEnemy.getLocationX();
        float y = deadEnemy.getLocationY();
        double roll = random.nextDouble();
        if (roll < bloodRatio) {
            return bloodFactory.createProp(config, x, y);
        }
        if (roll < bloodRatio + bombRatio) {
            return bombFactory.createProp(config, x, y);
        }
        if (roll < bloodRatio + bombRatio + fireRatio) {
            return fireFactory.createProp(config, x, y);
        }
        return superFireFactory.createProp(config, x, y);
    }
}

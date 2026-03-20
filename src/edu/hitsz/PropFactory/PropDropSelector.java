package edu.hitsz.PropFactory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EnemyAircraft.EliteEnemy;
import edu.hitsz.aircraft.EnemyAircraft.MobEnemy;
import edu.hitsz.prop.AbstractProp;

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
        this.bombFactory  = bombFactory;
        this.fireFactory  = fireFactory;
        this.superFireFactory = superFireFactory;
        this.dropRate     = dropRate;
        this.bloodRatio   = bloodRatio / sum;
        this.bombRatio    = bombRatio  / sum;
        this.fireRatio    = fireRatio  / sum;
        this.superFireRatio = superFireRatio / sum;
    }

    /** 根据死亡的敌机决定掉落；不掉则返回 null */
    public AbstractProp createFor(AbstractAircraft deadEnemy) {
        if (deadEnemy instanceof MobEnemy) return null;

        // 是否掉落
        if (random.nextDouble() >= dropRate) return null;

        int x = deadEnemy.getLocationX();
        int y = deadEnemy.getLocationY();

        // 掉哪一种
        double r = random.nextDouble();
        if (r < bloodRatio) return bloodFactory.createProp(x, y);
        else if (r < bloodRatio + bombRatio) return bombFactory.createProp(x, y);
        else if (r < bloodRatio + bombRatio + fireRatio) return fireFactory.createProp(x, y);
        else return superFireFactory.createProp(x, y);
    }
}

package edu.hitsz.aircraft.EnemyAircraft;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.application.Main;
import edu.hitsz.bomb.BombObserver;
import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractEnemy extends AbstractAircraft implements BombObserver {
    protected int bombScore = 10;
    public AbstractEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }
    /*public int getAddScore() {
        return AddScore;
    }*/
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= Main.WINDOW_HEIGHT ) {
            vanish();
        }
    }

    public List<BaseBullet> shoot() {
        return new LinkedList<>();
    }

    public abstract List<BaseBullet> shoot(long now);

    @Override
    public int onBombActivated() {
        if (notValid()) {
            return 0;
        }
        vanish();
        return bombScore;
    }

    @Override
    public boolean isActive() {
        return !notValid();
    }

    //public abstract AbstractProp dropProp();
}

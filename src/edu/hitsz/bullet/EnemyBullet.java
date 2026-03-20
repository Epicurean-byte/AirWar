package edu.hitsz.bullet;

import edu.hitsz.bomb.BombObserver;

/**
 * @Author hitsz
 */
public class EnemyBullet extends BaseBullet implements BombObserver {
    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public int onBombActivated() {
        if (!notValid()) {
            vanish();
        }
        return 0;
    }

    @Override
    public boolean isActive() {
        return !notValid();
    }
}

package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

import java.util.List;

public abstract class AbstractPatternBulletFactory implements BulletFactory {
    protected final int shootNum;
    protected final int direction; // -1 up, +1 down
    protected final BulletCreator creator;

    protected AbstractPatternBulletFactory(int shootNum, int direction, BulletCreator creator) {
        this.shootNum = shootNum;
        this.direction = direction;
        this.creator = creator;
    }

    @Override
    public abstract List<BaseBullet> createBullets(int x, int y, int speedX, int speedY, int power);
}


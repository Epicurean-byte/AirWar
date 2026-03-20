package edu.hitsz.game.core.factory;

public abstract class AbstractPatternBulletFactory implements BulletFactory {
    protected final int shootNum;
    protected final int direction;
    protected final BulletCreator creator;

    protected AbstractPatternBulletFactory(int shootNum, int direction, BulletCreator creator) {
        this.shootNum = shootNum;
        this.direction = direction;
        this.creator = creator;
    }
}

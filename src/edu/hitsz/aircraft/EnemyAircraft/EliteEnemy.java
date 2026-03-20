package edu.hitsz.aircraft.EnemyAircraft;

import edu.hitsz.BulletFactory.*;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class EliteEnemy extends AbstractEnemy {
    private int shootNum = 1;
    private BulletFactory bulletFactory = new StraightBulletFactory(shootNum, 1, edu.hitsz.bullet.EnemyBullet::new);
    private static final int SHOOT_INTERVAL = 1000;
    private long lastShootTime = 0;

    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.bombScore = 10;
    }
    public List<BaseBullet> shoot(long now) {
        int direction = 1, power = 10;
        List<BaseBullet> res = new LinkedList<>();
        if (now - lastShootTime < SHOOT_INTERVAL) {
            return res;
        }
        lastShootTime = now;
        int x = this.getLocationX();
        int y = this.getLocationY() + direction * 2;
        int speedX = 0;
        int speedY = this.getSpeedY() + direction * 5;
        return bulletFactory.createBullets(x, y, speedX, speedY, power);
    }
    /*@Override
    public AbstractProp dropProp() {
        double p = Math.random();
        if (p < 0.25)
            return new BombSupply(getLocationX(), getLocationY(), 0, 3);
        else if (p < 0.5)
            return new BombSupply(getLocationX(), getLocationY(), 0, 3);
        else if (p < 0.75)
            return new FireSupply(getLocationX(), getLocationY(), 0, 3);
        else return null;
    }*/
    public BufferedImage getImage() {
        return ImageManager.ELITE_IMAGE;
    }
}

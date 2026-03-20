package edu.hitsz.aircraft.EnemyAircraft;

import edu.hitsz.BulletFactory.BulletFactory;
import edu.hitsz.BulletFactory.ScatterBulletFactory;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class SuperEliteEnemy extends AbstractEnemy {
    private final BulletFactory bulletFactory = new ScatterBulletFactory(3, 1, edu.hitsz.bullet.EnemyBullet::new);
    private static final int SHOOT_INTERVAL = 1200; // ms
    private long lastShootTime = 0;

    public SuperEliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.bombScore = 10;
    }

    @Override
    public void forward() {
        super.forward();
        int halfWidth = getWidth() / 2;
        if ((locationX <= halfWidth && speedX < 0) || (locationX >= Main.WINDOW_WIDTH - halfWidth && speedX > 0)) {
            speedX = -speedX;
            // keep inside bounds
            locationX = Math.max(halfWidth, Math.min(locationX, Main.WINDOW_WIDTH - halfWidth));
        }
    }

    @Override
    public List<BaseBullet> shoot(long now) {
        List<BaseBullet> res = new LinkedList<>();
        if (now - lastShootTime < SHOOT_INTERVAL) return res;
        lastShootTime = now;
        return bulletFactory.createBullets(getLocationX(), getLocationY() + 2, 0, getSpeedY(), 12);
    }

    public BufferedImage getImage() {
        return ImageManager.SUPER_ELITE_IMAGE; // reuse elite image
    }

    @Override
    public int onBombActivated() {
        if (notValid()) {
            return 0;
        }
        decreaseHp(120);
        if (notValid()) {
            return bombScore;
        }
        return 0;
    }
}

package edu.hitsz.aircraft.EnemyAircraft;

import edu.hitsz.BulletFactory.BulletFactory;
import edu.hitsz.BulletFactory.RingBulletFactory;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class BossEnemy extends AbstractEnemy {
    private final BulletFactory bulletFactory = new RingBulletFactory(20, 1, edu.hitsz.bullet.EnemyBullet::new);
    private static final int SHOOT_INTERVAL = 1000; // ms
    private long lastShootTime = 0;

    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public void forward() {
        // float left-right near top; prevent leaving screen
        locationX += speedX;
        int leftBound = 60;
        int rightBound = Main.WINDOW_WIDTH - 60;
        if ((locationX <= leftBound && speedX < 0) || (locationX >= rightBound && speedX > 0)) {
            speedX = -speedX;
            locationX = Math.max(leftBound, Math.min(locationX, rightBound));
        }
    }

    @Override
    public List<BaseBullet> shoot(long now) {
        List<BaseBullet> res = new LinkedList<>();
        if (now - lastShootTime < SHOOT_INTERVAL) return res;
        lastShootTime = now;
        return bulletFactory.createBullets(getLocationX(), getLocationY() + 10, 0, 0, 15);
    }

    public BufferedImage getImage() {
        // reuse mob enemy image as placeholder (no boss asset provided)
        return ImageManager.BOSS_ELITE_IMAGE;
    }

    @Override
    public int onBombActivated() {
        return 0;
    }
}

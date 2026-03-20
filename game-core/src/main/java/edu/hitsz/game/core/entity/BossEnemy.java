package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.factory.BulletFactory;
import edu.hitsz.game.core.factory.RingBulletFactory;

import java.util.List;

public class BossEnemy extends AbstractEnemy {
    private static final long SHOOT_INTERVAL_MS = 1000L;

    private final BulletFactory bulletFactory = new RingBulletFactory(20, 1, EnemyBullet::new);
    private long lastShootTime = 0L;

    public BossEnemy(float locationX, float locationY, float speedX, float speedY, int hp, Size size) {
        super(SpriteId.BOSS_ENEMY, locationX, locationY, speedX, speedY, hp, 10, 0, size);
    }

    @Override
    public void forward(GameSessionConfig config) {
        locationX += speedX;
        float halfWidth = getWidth() / 2.0f;
        float leftBound = halfWidth + 20.0f;
        float rightBound = config.getWorldWidth() - halfWidth - 20.0f;
        if ((locationX <= leftBound && speedX < 0) || (locationX >= rightBound && speedX > 0)) {
            speedX = -speedX;
            locationX = Math.max(leftBound, Math.min(locationX, rightBound));
        }
    }

    @Override
    public List<BaseBullet> shoot(GameSessionConfig config, long now) {
        if (now - lastShootTime < SHOOT_INTERVAL_MS) {
            return List.of();
        }
        lastShootTime = now;
        return bulletFactory.createBullets(
                config.sizeOf(SpriteId.ENEMY_BULLET),
                getLocationX(),
                getLocationY() + 10.0f,
                0.0f,
                0.0f,
                15
        );
    }

    @Override
    public int onBombActivated() {
        return 0;
    }
}

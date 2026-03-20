package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.factory.BulletFactory;
import edu.hitsz.game.core.factory.ScatterBulletFactory;

import java.util.List;

public class SuperEliteEnemy extends AbstractEnemy {
    private static final long SHOOT_INTERVAL_MS = 1200L;

    private final BulletFactory bulletFactory = new ScatterBulletFactory(3, 1, EnemyBullet::new);
    private long lastShootTime = 0L;

    public SuperEliteEnemy(float locationX, float locationY, float speedX, float speedY, int hp, Size size) {
        super(SpriteId.SUPER_ELITE_ENEMY, locationX, locationY, speedX, speedY, hp, 10, 10, size);
    }

    @Override
    public void forward(GameSessionConfig config) {
        super.forward(config);
        float halfWidth = getWidth() / 2.0f;
        if ((locationX <= halfWidth && speedX < 0)
                || (locationX >= config.getWorldWidth() - halfWidth && speedX > 0)) {
            speedX = -speedX;
            locationX = Math.max(halfWidth, Math.min(locationX, config.getWorldWidth() - halfWidth));
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
                getLocationY() + 2.0f,
                0.0f,
                getSpeedY(),
                12
        );
    }

    @Override
    public int onBombActivated() {
        if (notValid()) {
            return 0;
        }
        decreaseHp(120);
        return notValid() ? bombScore : 0;
    }
}

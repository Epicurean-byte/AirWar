package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.factory.BulletFactory;
import edu.hitsz.game.core.factory.StraightBulletFactory;

import java.util.List;

public class EliteEnemy extends AbstractEnemy {
    private static final long SHOOT_INTERVAL_MS = 1000L;

    private final BulletFactory bulletFactory = new StraightBulletFactory(1, 1, EnemyBullet::new);
    private long lastShootTime = 0L;

    public EliteEnemy(float locationX, float locationY, float speedX, float speedY, int hp, Size size) {
        super(SpriteId.ELITE_ENEMY, locationX, locationY, speedX, speedY, hp, 10, 10, size);
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
                10
        );
    }
}

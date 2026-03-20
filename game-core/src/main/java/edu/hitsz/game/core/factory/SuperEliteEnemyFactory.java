package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.entity.AbstractEnemy;
import edu.hitsz.game.core.entity.SuperEliteEnemy;

import java.util.Random;

public class SuperEliteEnemyFactory implements EnemyFactory {
    private final Random random = new Random();
    private final int speedY;
    private final int hp;
    private final int speedX;

    public SuperEliteEnemyFactory() {
        this(4, 180, 3);
    }

    public SuperEliteEnemyFactory(int speedY, int hp) {
        this(speedY, hp, 3);
    }

    public SuperEliteEnemyFactory(int speedY, int hp, int speedX) {
        this.speedY = speedY;
        this.hp = hp;
        this.speedX = speedX;
    }

    @Override
    public AbstractEnemy createEnemy(SpawnContext context) {
        GameSessionConfig config = context.getSessionConfig();
        float spawnX = random.nextInt(Math.max(1, config.getWorldWidth() - config.widthOf(SpriteId.SUPER_ELITE_ENEMY))) + config.widthOf(SpriteId.SUPER_ELITE_ENEMY) / 2.0f;
        float spawnY = random.nextFloat() * config.getWorldHeight() * 0.05f;
        int horizontalDirection = random.nextBoolean() ? speedX : -speedX;
        return new SuperEliteEnemy(
                spawnX,
                spawnY,
                horizontalDirection,
                speedY,
                hp,
                config.sizeOf(SpriteId.SUPER_ELITE_ENEMY)
        );
    }
}

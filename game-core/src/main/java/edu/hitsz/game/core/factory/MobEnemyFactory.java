package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.entity.AbstractEnemy;
import edu.hitsz.game.core.entity.MobEnemy;

import java.util.Random;

public class MobEnemyFactory implements EnemyFactory {
    private final Random random = new Random();
    private final int speedY;
    private final int hp;

    public MobEnemyFactory() {
        this(8, 50);
    }

    public MobEnemyFactory(int speedY, int hp) {
        this.speedY = speedY;
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(SpawnContext context) {
        GameSessionConfig config = context.getSessionConfig();
        float spawnX = random.nextInt(Math.max(1, config.getWorldWidth() - config.widthOf(SpriteId.MOB_ENEMY))) + config.widthOf(SpriteId.MOB_ENEMY) / 2.0f;
        float spawnY = random.nextFloat() * config.getWorldHeight() * 0.05f;
        return new MobEnemy(spawnX, spawnY, 0.0f, speedY, hp, config.sizeOf(SpriteId.MOB_ENEMY));
    }
}

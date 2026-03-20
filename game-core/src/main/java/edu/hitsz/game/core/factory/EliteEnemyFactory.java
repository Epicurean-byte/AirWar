package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.entity.AbstractEnemy;
import edu.hitsz.game.core.entity.EliteEnemy;

import java.util.Random;

public class EliteEnemyFactory implements EnemyFactory {
    private final Random random = new Random();
    private final int speedY;
    private final int hp;

    public EliteEnemyFactory() {
        this(6, 120);
    }

    public EliteEnemyFactory(int speedY, int hp) {
        this.speedY = speedY;
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(SpawnContext context) {
        GameSessionConfig config = context.getSessionConfig();
        float spawnX = random.nextInt(Math.max(1, config.getWorldWidth() - config.widthOf(SpriteId.ELITE_ENEMY))) + config.widthOf(SpriteId.ELITE_ENEMY) / 2.0f;
        float spawnY = random.nextFloat() * config.getWorldHeight() * 0.05f;
        return new EliteEnemy(spawnX, spawnY, 0.0f, speedY, hp, config.sizeOf(SpriteId.ELITE_ENEMY));
    }
}

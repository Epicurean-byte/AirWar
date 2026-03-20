package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.entity.AbstractEnemy;
import edu.hitsz.game.core.entity.BossEnemy;

public class BossEnemyFactory implements EnemyFactory {
    private int hp;
    private int speedX;

    public BossEnemyFactory() {
        this(500, 5);
    }

    public BossEnemyFactory(int hp, int speedX) {
        this.hp = hp;
        this.speedX = speedX;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(SpawnContext context) {
        GameSessionConfig config = context.getSessionConfig();
        return new BossEnemy(
                config.getWorldWidth() / 2.0f,
                60.0f,
                speedX,
                0.0f,
                hp,
                config.sizeOf(edu.hitsz.game.core.config.SpriteId.BOSS_ENEMY)
        );
    }
}

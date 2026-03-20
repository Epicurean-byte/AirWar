package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

import java.util.List;

public class MobEnemy extends AbstractEnemy {
    public MobEnemy(float locationX, float locationY, float speedX, float speedY, int hp, Size size) {
        super(SpriteId.MOB_ENEMY, locationX, locationY, speedX, speedY, hp, 10, 10, size);
    }

    @Override
    public List<BaseBullet> shoot(GameSessionConfig config, long now) {
        return List.of();
    }
}

package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

public class EnemyBullet extends BaseBullet {
    public EnemyBullet(float locationX, float locationY, float speedX, float speedY, int power, Size size) {
        super(SpriteId.ENEMY_BULLET, locationX, locationY, speedX, speedY, power, size);
    }
}

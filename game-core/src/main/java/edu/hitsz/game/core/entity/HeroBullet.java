package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

public class HeroBullet extends BaseBullet {
    public HeroBullet(float locationX, float locationY, float speedX, float speedY, int power, Size size) {
        super(SpriteId.HERO_BULLET, locationX, locationY, speedX, speedY, power, size);
    }
}

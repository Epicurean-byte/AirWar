package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.entity.BaseBullet;

@FunctionalInterface
public interface BulletCreator {
    BaseBullet create(float x, float y, float speedX, float speedY, int power, Size size);
}

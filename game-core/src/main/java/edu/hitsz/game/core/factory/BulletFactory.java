package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.entity.BaseBullet;

import java.util.List;

public interface BulletFactory {
    List<BaseBullet> createBullets(Size bulletSize,
                                   float x,
                                   float y,
                                   float speedX,
                                   float speedY,
                                   int power);
}

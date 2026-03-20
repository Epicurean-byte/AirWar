package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.entity.BaseBullet;

import java.util.ArrayList;
import java.util.List;

public class StraightBulletFactory extends AbstractPatternBulletFactory {
    public StraightBulletFactory(int shootNum, int direction, BulletCreator creator) {
        super(shootNum, direction, creator);
    }

    @Override
    public List<BaseBullet> createBullets(Size bulletSize,
                                          float x,
                                          float y,
                                          float speedX,
                                          float speedY,
                                          int power) {
        List<BaseBullet> bullets = new ArrayList<>();
        float realSpeedY = speedY + direction * 5.0f;
        for (int i = 0; i < shootNum; i++) {
            float offsetX = (i * 2 - shootNum + 1) * 10.0f;
            bullets.add(creator.create(x + offsetX, y, speedX, realSpeedY, power, bulletSize));
        }
        return bullets;
    }
}

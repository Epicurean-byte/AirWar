package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.entity.BaseBullet;

import java.util.ArrayList;
import java.util.List;

public class ScatterBulletFactory extends AbstractPatternBulletFactory {
    public ScatterBulletFactory(int shootNum, int direction, BulletCreator creator) {
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
        if (shootNum <= 1) {
            bullets.add(creator.create(x, y, speedX, realSpeedY, power, bulletSize));
            return bullets;
        }
        float center = (shootNum - 1) / 2.0f;
        for (int i = 0; i < shootNum; i++) {
            float spread = i - center;
            bullets.add(creator.create(
                    x + spread * 12.0f,
                    y,
                    spread * 2.0f,
                    realSpeedY,
                    power,
                    bulletSize
            ));
        }
        return bullets;
    }
}

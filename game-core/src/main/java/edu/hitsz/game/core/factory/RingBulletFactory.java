package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.entity.BaseBullet;

import java.util.ArrayList;
import java.util.List;

public class RingBulletFactory extends AbstractPatternBulletFactory {
    public RingBulletFactory(int shootNum, int direction, BulletCreator creator) {
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
        double velocity = 5.0;
        int count = Math.max(1, shootNum);
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            float sx = (float) Math.round(velocity * Math.cos(angle));
            float sy = (float) Math.round(velocity * Math.sin(angle)) + direction * 2.0f;
            bullets.add(creator.create(x, y, sx, sy, power, bulletSize));
        }
        return bullets;
    }
}

package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * Fan-shaped scatter shooting. Common for super-elite enemies or powered hero.
 */
public class ScatterBulletFactory extends AbstractPatternBulletFactory {
    public ScatterBulletFactory(int shootNum, int direction, BulletCreator creator) {
        super(shootNum, direction, creator);
    }

    @Override
    public List<BaseBullet> createBullets(int x, int y, int speedX, int speedY, int power) {
        List<BaseBullet> res = new LinkedList<>();
        int realSpeedY = speedY + direction * 5;
        if (shootNum <= 1) {
            res.add(creator.create(x, y, speedX, realSpeedY, power));
            return res;
        }
        // spread horizontally with slight speedX variations
        for (int i = 0; i < shootNum; i++) {
            int offsetX = (i - (shootNum - 1) / 2) * 12;
            int sx = (i - (shootNum - 1) / 2) * 2; // small horizontal speed spread
            res.add(creator.create(x + offsetX, y, sx, realSpeedY, power));
        }
        return res;
    }
}


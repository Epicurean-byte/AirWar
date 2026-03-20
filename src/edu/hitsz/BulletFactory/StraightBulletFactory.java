package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * Straight line shooting. Creates {@code shootNum} bullets centered horizontally.
 */
public class StraightBulletFactory extends AbstractPatternBulletFactory {

    public StraightBulletFactory(int shootNum, int direction, BulletCreator creator) {
        super(shootNum, direction, creator);
    }

    @Override
    public List<BaseBullet> createBullets(int x, int y, int speedX, int speedY, int power) {
        List<BaseBullet> res = new LinkedList<>();
        int realSpeedY = speedY + direction * 5;
        for (int i = 0; i < shootNum; i++) {
            int offsetX = (i * 2 - shootNum + 1) * 10;
            res.add(creator.create(x + offsetX, y, speedX, realSpeedY, power));
        }
        return res;
    }
}


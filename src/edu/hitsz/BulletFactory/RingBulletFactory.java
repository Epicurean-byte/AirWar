package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * Ring shooting: evenly distributes bullets around the shooter.
 */
public class RingBulletFactory extends AbstractPatternBulletFactory {
    public RingBulletFactory(int shootNum, int direction, BulletCreator creator) {
        super(shootNum, direction, creator);
    }

    @Override
    public List<BaseBullet> createBullets(int x, int y, int speedX, int speedY, int power) {
        List<BaseBullet> res = new LinkedList<>();
        // base speed magnitude
        double v = 5.0;
        int n = Math.max(1, shootNum);
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            int sx = (int) Math.round(v * Math.cos(angle));
            int sy = (int) Math.round(v * Math.sin(angle));
            // direction controls global vertical sense for consistency
            sy = sy + direction * 2; // slight bias to move away from shooter
            res.add(creator.create(x, y, sx, sy, power));
        }
        return res;
    }
}


package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

@FunctionalInterface
public interface BulletCreator {
    BaseBullet create(int x, int y, int speedX, int speedY, int power);
}


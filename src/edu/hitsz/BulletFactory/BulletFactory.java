package edu.hitsz.BulletFactory;

import edu.hitsz.bullet.BaseBullet;

import java.util.List;

public interface BulletFactory {
    List<BaseBullet> createBullets(int x, int y, int speedX, int speedY, int power);
}

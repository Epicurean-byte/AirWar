package edu.hitsz.aircraft.EnemyAircraft;

import edu.hitsz.bullet.BaseBullet;

import java.util.List;
import java.util.LinkedList;

/**
 * 普通敌机
 * 不可射击
 *
 * @author hitsz
 */
public class MobEnemy extends AbstractEnemy {
    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.bombScore = 10;
    }
    @Override
    public List<BaseBullet> shoot(long now){
        return new LinkedList<>();
    };
    /*@Override
    public AbstractProp dropProp(){
        return null;
    }*/
}

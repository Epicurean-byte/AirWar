package edu.hitsz.aircraft;

import edu.hitsz.BulletFactory.*;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;

import java.util.List;

/**
 * 英雄飞机，游戏玩家操控
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    private static HeroAircraft instance = null;
    /**攻击方式 */

    /**
     * 子弹伤害
     */
    private int power = 20;

    private BulletFactory bulletFactory = new StraightBulletFactory(1, -1, edu.hitsz.bullet.HeroBullet::new);

    public enum FireMode { STRAIGHT, SCATTER, RING }
    private FireMode fireMode = FireMode.STRAIGHT;
    private volatile long fireModeExpireAt = 0L;
    /**
     * @param locationX 英雄机位置x坐标
     * @param locationY 英雄机位置y坐标
     * @param speedX 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param speedY 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param hp    初始生命值
     */
    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    private HeroAircraft(){
        super();
    }
    public static synchronized HeroAircraft getInstance(){
        if(instance == null){
            instance = new HeroAircraft(Main.WINDOW_WIDTH / 2,
                    Main.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight() ,
                    0, 0, 1000);
        }
        return instance;
    }

    @Override
    public void forward() {
        // 英雄机由鼠标控制，不通过forward函数移动
    }

    @Override
    /**
     * 通过射击产生子弹
     * @return 射击出的子弹List
     */
    public List<BaseBullet> shoot() {
        int x = this.getLocationX();
        int y = this.getLocationY() - 2; // direction = -1，往上
        return bulletFactory.createBullets(x, y, 0, this.getSpeedY(), power);
    }

    public void setFireMode(FireMode mode) {
        this.fireMode = mode;
        switch (mode) {
            case STRAIGHT -> this.bulletFactory = new StraightBulletFactory(1, -1, edu.hitsz.bullet.HeroBullet::new);
            case SCATTER -> this.bulletFactory = new ScatterBulletFactory(3, -1, edu.hitsz.bullet.HeroBullet::new);
            case RING -> this.bulletFactory = new RingBulletFactory(12, -1, edu.hitsz.bullet.HeroBullet::new);
        }
    }

    public void cycleFireMode() {
        FireMode next = switch (fireMode) {
            case STRAIGHT -> FireMode.SCATTER;
            case SCATTER -> FireMode.RING;
            case RING -> FireMode.STRAIGHT;
        };
        setFireMode(next);
    }

    /** 应用限时火力模式，持续 durationMs 后自动恢复直射（若期间未被新的道具覆盖）。 */
    public synchronized void applyTimedFireMode(FireMode mode, long durationMs) {
        setFireMode(mode);
        long deadline = System.currentTimeMillis() + Math.max(0, durationMs);
        fireModeExpireAt = deadline;
        Thread t = new Thread(() -> {
            long wait = Math.max(0, deadline - System.currentTimeMillis());
            try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
            synchronized (HeroAircraft.this) {
                if (System.currentTimeMillis() >= fireModeExpireAt) {
                    setFireMode(FireMode.STRAIGHT);
                }
            }
        }, "firemode-timer");
        t.setDaemon(true);
        t.start();
    }

}

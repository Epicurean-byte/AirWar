package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.factory.BulletFactory;
import edu.hitsz.game.core.factory.RingBulletFactory;
import edu.hitsz.game.core.factory.ScatterBulletFactory;
import edu.hitsz.game.core.factory.StraightBulletFactory;

import java.util.List;

public class HeroAircraft extends AbstractAircraft {
    public enum FireMode {
        STRAIGHT,
        SCATTER,
        RING
    }

    private static final int DEFAULT_HP = 1000;
    private static final int POWER = 20;

    private BulletFactory bulletFactory = new StraightBulletFactory(1, -1, HeroBullet::new);
    private FireMode fireMode = FireMode.STRAIGHT;
    private long fireModeExpireAt = 0L;

    private HeroAircraft(float locationX, float locationY, float speedX, float speedY, int hp, Size size) {
        super(SpriteId.HERO, locationX, locationY, speedX, speedY, hp, size);
    }

    public static HeroAircraft createDefault(GameSessionConfig config) {
        Size size = config.sizeOf(SpriteId.HERO);
        return new HeroAircraft(
                config.getWorldWidth() / 2.0f,
                config.getWorldHeight() - size.height(),
                0.0f,
                0.0f,
                DEFAULT_HP,
                size
        );
    }

    @Override
    public void forward(GameSessionConfig config) {
        // Hero movement is controlled by touch input.
    }

    @Override
    public List<BaseBullet> shoot(GameSessionConfig config, long now) {
        return bulletFactory.createBullets(
                config.sizeOf(SpriteId.HERO_BULLET),
                getLocationX(),
                getLocationY() - 2.0f,
                0.0f,
                getSpeedY(),
                POWER
        );
    }

    public void moveTo(float x, float y, GameSessionConfig config) {
        float clampedX = Math.max(width / 2.0f, Math.min(x, config.getWorldWidth() - width / 2.0f));
        float clampedY = Math.max(height / 2.0f, Math.min(y, config.getWorldHeight() - height / 2.0f));
        setLocation(clampedX, clampedY);
    }

    public void setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
        switch (fireMode) {
            case STRAIGHT -> bulletFactory = new StraightBulletFactory(1, -1, HeroBullet::new);
            case SCATTER -> bulletFactory = new ScatterBulletFactory(3, -1, HeroBullet::new);
            case RING -> bulletFactory = new RingBulletFactory(12, -1, HeroBullet::new);
        }
    }

    public FireMode getFireMode() {
        return fireMode;
    }

    public void applyTimedFireMode(FireMode mode, long durationMs, long now) {
        setFireMode(mode);
        fireModeExpireAt = now + Math.max(0L, durationMs);
    }

    public void refreshTimedState(long now) {
        if (fireMode != FireMode.STRAIGHT && now >= fireModeExpireAt) {
            setFireMode(FireMode.STRAIGHT);
        }
    }
}

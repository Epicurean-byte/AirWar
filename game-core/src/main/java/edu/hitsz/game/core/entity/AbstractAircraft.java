package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

import java.util.List;

public abstract class AbstractAircraft extends AbstractFlyingObject {
    protected final int maxHp;
    protected int hp;

    protected AbstractAircraft(SpriteId spriteId,
                               float locationX,
                               float locationY,
                               float speedX,
                               float speedY,
                               int hp,
                               Size size) {
        super(spriteId, locationX, locationY, speedX, speedY, size);
        this.hp = hp;
        this.maxHp = hp;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void decreaseHp(int decrease) {
        hp -= decrease;
        if (hp <= 0) {
            hp = 0;
            vanish();
        }
    }

    public void increaseHp(int increase) {
        hp = Math.min(maxHp, hp + Math.max(0, increase));
    }

    public abstract List<BaseBullet> shoot(GameSessionConfig config, long now);
}

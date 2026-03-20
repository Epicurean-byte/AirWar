package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

public abstract class BaseBullet extends AbstractFlyingObject {
    private final int power;

    protected BaseBullet(SpriteId spriteId,
                         float locationX,
                         float locationY,
                         float speedX,
                         float speedY,
                         int power,
                         Size size) {
        super(spriteId, locationX, locationY, speedX, speedY, size);
        this.power = power;
    }

    @Override
    public void forward(GameSessionConfig config) {
        super.forward(config);
        if (locationX <= 0 || locationX >= config.getWorldWidth()) {
            vanish();
        }
        if (speedY > 0 && locationY >= config.getWorldHeight()) {
            vanish();
        } else if (locationY <= 0) {
            vanish();
        }
    }

    public int getPower() {
        return power;
    }
}

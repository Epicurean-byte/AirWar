package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.engine.GameWorld;

public abstract class AbstractProp extends AbstractFlyingObject {
    protected AbstractProp(SpriteId spriteId,
                           float locationX,
                           float locationY,
                           float speedX,
                           float speedY,
                           Size size) {
        super(spriteId, locationX, locationY, speedX, speedY, size);
    }

    @Override
    public void forward(GameSessionConfig config) {
        locationX += speedX;
        locationY += speedY;
        if (locationY >= config.getWorldHeight() + height / 2.0f) {
            vanish();
        }
    }

    public abstract int activate(GameWorld gameWorld, long now);
}

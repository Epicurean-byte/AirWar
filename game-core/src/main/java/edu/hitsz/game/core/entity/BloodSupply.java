package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.engine.GameWorld;

public class BloodSupply extends AbstractProp {
    public BloodSupply(float locationX, float locationY, float speedX, float speedY, Size size) {
        super(SpriteId.BLOOD_SUPPLY, locationX, locationY, speedX, speedY, size);
    }

    @Override
    public int activate(GameWorld gameWorld, long now) {
        gameWorld.getHeroAircraft().increaseHp(20);
        return 0;
    }
}

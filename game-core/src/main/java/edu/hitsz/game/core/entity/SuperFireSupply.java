package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.engine.GameWorld;

public class SuperFireSupply extends AbstractProp {
    public SuperFireSupply(float locationX, float locationY, float speedX, float speedY, Size size) {
        super(SpriteId.SUPER_FIRE_SUPPLY, locationX, locationY, speedX, speedY, size);
    }

    @Override
    public int activate(GameWorld gameWorld, long now) {
        gameWorld.getHeroAircraft().applyTimedFireMode(HeroAircraft.FireMode.RING, 3000L, now);
        return 0;
    }
}

package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.entity.AbstractProp;
import edu.hitsz.game.core.entity.SuperFireSupply;

public class SuperFireFactory implements PropFactory {
    @Override
    public AbstractProp createProp(GameSessionConfig config, float x, float y) {
        return new SuperFireSupply(x, y, 0.0f, 3.0f, config.sizeOf(SpriteId.SUPER_FIRE_SUPPLY));
    }
}

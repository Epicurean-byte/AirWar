package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.engine.GameWorld;
import edu.hitsz.game.core.event.GameEvent;

public class BombSupply extends AbstractProp {
    public BombSupply(float locationX, float locationY, float speedX, float speedY, Size size) {
        super(SpriteId.BOMB_SUPPLY, locationX, locationY, speedX, speedY, size);
    }

    @Override
    public int activate(GameWorld gameWorld, long now) {
        gameWorld.emitEvent(new GameEvent(GameEvent.Type.BOMB_EXPLOSION));
        return gameWorld.activateBomb();
    }
}

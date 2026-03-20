package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.event.BombObserver;

public abstract class AbstractEnemy extends AbstractAircraft implements BombObserver {
    private final int scoreValue;
    protected int bombScore;

    protected AbstractEnemy(SpriteId spriteId,
                            float locationX,
                            float locationY,
                            float speedX,
                            float speedY,
                            int hp,
                            int scoreValue,
                            int bombScore,
                            Size size) {
        super(spriteId, locationX, locationY, speedX, speedY, hp, size);
        this.scoreValue = scoreValue;
        this.bombScore = bombScore;
    }

    @Override
    public void forward(GameSessionConfig config) {
        super.forward(config);
        if (locationY >= config.getWorldHeight()) {
            vanish();
        }
    }

    public int getScoreValue() {
        return scoreValue;
    }

    @Override
    public int onBombActivated() {
        if (notValid()) {
            return 0;
        }
        vanish();
        return bombScore;
    }

    @Override
    public boolean isActive() {
        return !notValid();
    }
}

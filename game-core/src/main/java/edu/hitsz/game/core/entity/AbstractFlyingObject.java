package edu.hitsz.game.core.entity;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;

public abstract class AbstractFlyingObject {
    protected float locationX;
    protected float locationY;
    protected float speedX;
    protected float speedY;
    protected final int width;
    protected final int height;
    protected final SpriteId spriteId;
    protected boolean valid = true;

    protected AbstractFlyingObject(SpriteId spriteId,
                                   float locationX,
                                   float locationY,
                                   float speedX,
                                   float speedY,
                                   Size size) {
        this.spriteId = spriteId;
        this.locationX = locationX;
        this.locationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
        this.width = size.width();
        this.height = size.height();
    }

    public void forward(GameSessionConfig config) {
        locationX += speedX;
        locationY += speedY;
        if (locationX <= 0 || locationX >= config.getWorldWidth()) {
            speedX = -speedX;
        }
    }

    public boolean crash(AbstractFlyingObject other) {
        int factor = this instanceof AbstractAircraft ? 2 : 1;
        int otherFactor = other instanceof AbstractAircraft ? 2 : 1;
        return other.locationX + (other.width + this.width) / 2.0f > locationX
                && other.locationX - (other.width + this.width) / 2.0f < locationX
                && other.locationY + (other.height / (float) otherFactor + this.height / (float) factor) / 2.0f > locationY
                && other.locationY - (other.height / (float) otherFactor + this.height / (float) factor) / 2.0f < locationY;
    }

    public void setLocation(float locationX, float locationY) {
        this.locationX = locationX;
        this.locationY = locationY;
    }

    public float getLocationX() {
        return locationX;
    }

    public float getLocationY() {
        return locationY;
    }

    public float getSpeedX() {
        return speedX;
    }

    public float getSpeedY() {
        return speedY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public SpriteId getSpriteId() {
        return spriteId;
    }

    public boolean notValid() {
        return !valid;
    }

    public void vanish() {
        valid = false;
    }
}

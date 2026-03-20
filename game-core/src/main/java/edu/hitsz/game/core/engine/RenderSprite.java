package edu.hitsz.game.core.engine;

import edu.hitsz.game.core.config.SpriteId;

public final class RenderSprite {
    private final SpriteId spriteId;
    private final float x;
    private final float y;
    private final int width;
    private final int height;

    public RenderSprite(SpriteId spriteId, float x, float y, int width, int height) {
        this.spriteId = spriteId;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public SpriteId getSpriteId() {
        return spriteId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}

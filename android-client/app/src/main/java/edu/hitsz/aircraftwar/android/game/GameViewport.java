package edu.hitsz.aircraftwar.android.game;

import android.graphics.RectF;

public final class GameViewport {
    private final float positionScaleX;
    private final float positionScaleY;
    private final float spriteScale;
    private final int surfaceWidth;
    private final int surfaceHeight;
    private final int worldWidth;
    private final int worldHeight;

    private GameViewport(float positionScaleX,
                         float positionScaleY,
                         float spriteScale,
                         int surfaceWidth,
                         int surfaceHeight,
                         int worldWidth,
                         int worldHeight) {
        this.positionScaleX = positionScaleX;
        this.positionScaleY = positionScaleY;
        this.spriteScale = spriteScale;
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public static GameViewport fill(int surfaceWidth, int surfaceHeight, int worldWidth, int worldHeight) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || worldWidth <= 0 || worldHeight <= 0) {
            return new GameViewport(
                    1.0f,
                    1.0f,
                    1.0f,
                    Math.max(surfaceWidth, 1),
                    Math.max(surfaceHeight, 1),
                    Math.max(worldWidth, 1),
                    Math.max(worldHeight, 1)
            );
        }
        float positionScaleX = surfaceWidth / (float) worldWidth;
        float positionScaleY = surfaceHeight / (float) worldHeight;
        float spriteScale = Math.min(positionScaleX, positionScaleY);
        return new GameViewport(
                positionScaleX,
                positionScaleY,
                spriteScale,
                surfaceWidth,
                surfaceHeight,
                worldWidth,
                worldHeight
        );
    }

    public RectF worldRectToScreen(float centerX, float centerY, float width, float height) {
        float halfWidth = width * spriteScale / 2.0f;
        float halfHeight = height * spriteScale / 2.0f;
        float screenCenterX = worldToScreenX(centerX);
        float screenCenterY = worldToScreenY(centerY);
        return new RectF(
                screenCenterX - halfWidth,
                screenCenterY - halfHeight,
                screenCenterX + halfWidth,
                screenCenterY + halfHeight
        );
    }

    public float worldToScreenX(float x) {
        return x * positionScaleX;
    }

    public float worldToScreenY(float y) {
        return y * positionScaleY;
    }

    public float screenToWorldX(float x) {
        return x / positionScaleX;
    }

    public float screenToWorldY(float y) {
        return y / positionScaleY;
    }

    public float scale() {
        return spriteScale;
    }

    public float getOffsetX() {
        return 0.0f;
    }

    public float getOffsetY() {
        return 0.0f;
    }

    public float getScaledWidth() {
        return surfaceWidth;
    }

    public float getScaledHeight() {
        return surfaceHeight;
    }

    public float spriteWidthToScreen(float width) {
        return width * spriteScale;
    }

    public float spriteHeightToScreen(float height) {
        return height * spriteScale;
    }
}

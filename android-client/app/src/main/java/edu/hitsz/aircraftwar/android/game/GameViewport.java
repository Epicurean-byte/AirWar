package edu.hitsz.aircraftwar.android.game;

import android.graphics.RectF;

public final class GameViewport {
    private final float scale;
    private final float offsetX;
    private final float offsetY;
    private final int worldWidth;
    private final int worldHeight;

    private GameViewport(float scale, float offsetX, float offsetY, int worldWidth, int worldHeight) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public static GameViewport fit(int surfaceWidth, int surfaceHeight, int worldWidth, int worldHeight) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || worldWidth <= 0 || worldHeight <= 0) {
            return new GameViewport(1.0f, 0.0f, 0.0f, Math.max(worldWidth, 1), Math.max(worldHeight, 1));
        }
        float scale = Math.min(surfaceWidth / (float) worldWidth, surfaceHeight / (float) worldHeight);
        float scaledWidth = worldWidth * scale;
        float scaledHeight = worldHeight * scale;
        float offsetX = (surfaceWidth - scaledWidth) / 2.0f;
        float offsetY = (surfaceHeight - scaledHeight) / 2.0f;
        return new GameViewport(scale, offsetX, offsetY, worldWidth, worldHeight);
    }

    public RectF worldRectToScreen(float centerX, float centerY, float width, float height) {
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        return new RectF(
                worldToScreenX(centerX - halfWidth),
                worldToScreenY(centerY - halfHeight),
                worldToScreenX(centerX + halfWidth),
                worldToScreenY(centerY + halfHeight)
        );
    }

    public float worldToScreenX(float x) {
        return offsetX + x * scale;
    }

    public float worldToScreenY(float y) {
        return offsetY + y * scale;
    }

    public float screenToWorldX(float x) {
        return (x - offsetX) / scale;
    }

    public float screenToWorldY(float y) {
        return (y - offsetY) / scale;
    }

    public float scale() {
        return scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getScaledWidth() {
        return worldWidth * scale;
    }

    public float getScaledHeight() {
        return worldHeight * scale;
    }
}

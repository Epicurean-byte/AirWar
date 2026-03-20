package edu.hitsz.game.core.config;

public final class Size {
    private final int width;
    private final int height;

    public Size(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Sprite size must be positive");
        }
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}

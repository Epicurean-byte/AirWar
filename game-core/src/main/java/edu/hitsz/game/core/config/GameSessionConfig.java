package edu.hitsz.game.core.config;

import java.util.EnumMap;
import java.util.Map;

public final class GameSessionConfig {
    private final int worldWidth;
    private final int worldHeight;
    private final EnumMap<SpriteId, Size> spriteSizes;

    public GameSessionConfig(int worldWidth, int worldHeight, Map<SpriteId, Size> spriteSizes) {
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("World size must be positive");
        }
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.spriteSizes = new EnumMap<>(SpriteId.class);
        this.spriteSizes.putAll(spriteSizes);
    }

    public static GameSessionConfig defaultConfig(int worldWidth, int worldHeight) {
        EnumMap<SpriteId, Size> spriteSizes = new EnumMap<>(SpriteId.class);
        spriteSizes.put(SpriteId.BACKGROUND_EASY, new Size(worldWidth, worldHeight));
        spriteSizes.put(SpriteId.BACKGROUND_NORMAL, new Size(worldWidth, worldHeight));
        spriteSizes.put(SpriteId.BACKGROUND_HARD, new Size(worldWidth, worldHeight));
        spriteSizes.put(SpriteId.HERO, new Size(66, 82));
        spriteSizes.put(SpriteId.HERO_BULLET, new Size(10, 24));
        spriteSizes.put(SpriteId.ENEMY_BULLET, new Size(10, 24));
        spriteSizes.put(SpriteId.MOB_ENEMY, new Size(56, 43));
        spriteSizes.put(SpriteId.ELITE_ENEMY, new Size(60, 46));
        spriteSizes.put(SpriteId.SUPER_ELITE_ENEMY, new Size(66, 50));
        spriteSizes.put(SpriteId.BOSS_ENEMY, new Size(128, 96));
        spriteSizes.put(SpriteId.BLOOD_SUPPLY, new Size(32, 32));
        spriteSizes.put(SpriteId.BOMB_SUPPLY, new Size(32, 32));
        spriteSizes.put(SpriteId.FIRE_SUPPLY, new Size(32, 32));
        spriteSizes.put(SpriteId.SUPER_FIRE_SUPPLY, new Size(36, 36));
        return new GameSessionConfig(worldWidth, worldHeight, spriteSizes);
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public Size sizeOf(SpriteId spriteId) {
        Size size = spriteSizes.get(spriteId);
        if (size == null) {
            throw new IllegalArgumentException("Missing sprite size for " + spriteId);
        }
        return size;
    }

    public int widthOf(SpriteId spriteId) {
        return sizeOf(spriteId).width();
    }

    public int heightOf(SpriteId spriteId) {
        return sizeOf(spriteId).height();
    }
}

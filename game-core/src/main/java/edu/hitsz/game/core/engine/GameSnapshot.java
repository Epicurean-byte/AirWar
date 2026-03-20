package edu.hitsz.game.core.engine;

import edu.hitsz.game.core.mode.Difficulty;

import java.util.List;

public final class GameSnapshot {
    private final Difficulty difficulty;
    private final int worldWidth;
    private final int worldHeight;
    private final int backgroundOffset;
    private final int score;
    private final int heroHp;
    private final int heroMaxHp;
    private final boolean gameOver;
    private final List<RenderSprite> renderSprites;

    public GameSnapshot(Difficulty difficulty,
                        int worldWidth,
                        int worldHeight,
                        int backgroundOffset,
                        int score,
                        int heroHp,
                        int heroMaxHp,
                        boolean gameOver,
                        List<RenderSprite> renderSprites) {
        this.difficulty = difficulty;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.backgroundOffset = backgroundOffset;
        this.score = score;
        this.heroHp = heroHp;
        this.heroMaxHp = heroMaxHp;
        this.gameOver = gameOver;
        this.renderSprites = List.copyOf(renderSprites);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public int getBackgroundOffset() {
        return backgroundOffset;
    }

    public int getScore() {
        return score;
    }

    public int getHeroHp() {
        return heroHp;
    }

    public int getHeroMaxHp() {
        return heroMaxHp;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public List<RenderSprite> getRenderSprites() {
        return renderSprites;
    }
}

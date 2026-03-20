package edu.hitsz.game.core.mode;

public final class GameModeFactory {
    private GameModeFactory() {
    }

    public static AbstractGameMode create(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new EasyGameMode();
            case NORMAL -> new NormalGameMode();
            case HARD -> new HardGameMode();
        };
    }
}

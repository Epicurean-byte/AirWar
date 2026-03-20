package edu.hitsz.application.mode;

import edu.hitsz.application.Difficulty;

public final class GameModeFactory {
    private GameModeFactory() {}

    public static AbstractGameMode create(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new EasyGameMode();
            case NORMAL -> new NormalGameMode();
            case HARD -> new HardGameMode();
        };
    }
}


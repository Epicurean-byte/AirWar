package edu.hitsz.application;

public enum Difficulty {
    EASY, NORMAL, HARD;

    @Override
    public String toString() {
        return switch (this) {
            case EASY -> "EASY";
            case NORMAL -> "NORMAL";
            case HARD -> "HARD";
        };
    }
}


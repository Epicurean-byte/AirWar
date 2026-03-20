package edu.hitsz.game.core.event;

public final class GameEvent {
    public enum Type {
        BULLET_HIT,
        BOMB_EXPLOSION,
        GET_SUPPLY,
        GAME_OVER,
        BOSS_SPAWN,
        BOSS_DEFEATED
    }

    private final Type type;
    private final String message;

    public GameEvent(Type type) {
        this(type, "");
    }

    public GameEvent(Type type, String message) {
        this.type = type;
        this.message = message == null ? "" : message;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}

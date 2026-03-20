package edu.hitsz.game.core.event;

public interface BombObserver {
    int onBombActivated();

    boolean isActive();
}

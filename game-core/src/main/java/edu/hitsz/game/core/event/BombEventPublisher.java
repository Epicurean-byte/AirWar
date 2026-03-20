package edu.hitsz.game.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BombEventPublisher {
    private final List<BombObserver> observers = new CopyOnWriteArrayList<>();

    public void register(BombObserver observer) {
        if (observer != null && observer.isActive()) {
            observers.add(observer);
        }
    }

    public void unregister(BombObserver observer) {
        observers.remove(observer);
    }

    public int notifyObservers() {
        int total = 0;
        for (BombObserver observer : observers) {
            total += observer.onBombActivated();
        }
        observers.removeIf(observer -> !observer.isActive());
        return total;
    }
}

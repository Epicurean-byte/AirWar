package edu.hitsz.bomb;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 炸弹事件发布者（单例）。
 * 负责维护所有可受炸弹影响对象，并在炸弹道具生效时逐一通知。
 */
public final class BombEventPublisher {

    private static final BombEventPublisher INSTANCE = new BombEventPublisher();

    public static BombEventPublisher getInstance() {
        return INSTANCE;
    }

    private final List<BombObserver> observers = new CopyOnWriteArrayList<>();

    private BombEventPublisher() {}

    public void register(BombObserver observer) {
        if (observer != null && observer.isActive()) {
            observers.add(observer);
        }
    }

    public void unregister(BombObserver observer) {
        observers.remove(observer);
    }

    /**
     * 触发炸弹事件，返回英雄机获得的总分。
     */
    public int notifyObservers() {
        int total = 0;
        for (BombObserver observer : observers) {
            total += observer.onBombActivated();
        }
        observers.removeIf(observer -> !observer.isActive());
        return total;
    }
}

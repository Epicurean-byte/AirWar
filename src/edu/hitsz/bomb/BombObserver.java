package edu.hitsz.bomb;

/** 观察者接口：响应炸弹道具触发。 */
public interface BombObserver {
    /**
     * 当炸弹生效时的回调。
     * @return 英雄机因该对象受到影响而获得的分数（若无则返回0）。
     */
    int onBombActivated();

    /**
     * 是否仍需要继续监听炸弹事件。
     * 若对象已经无效，返回 false 以便取消订阅。
     */
    boolean isActive();
}


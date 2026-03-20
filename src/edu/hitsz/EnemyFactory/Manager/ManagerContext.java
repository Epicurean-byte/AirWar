package edu.hitsz.EnemyFactory.Manager;

public final class ManagerContext {
    public final int timeMs;
    public final int enemyCount;
    public final int enemyMaxNumber;
    public final int screenWidth;
    public final int screenHeight;
    public ManagerContext(int timeMs, int enemyCount, int enemyMaxNumber, int w, int h) {
        this.timeMs = timeMs;
        this.enemyCount = enemyCount;
        this.enemyMaxNumber = enemyMaxNumber;
        this.screenWidth = w;
        this.screenHeight = h;
    }
}

package edu.hitsz.EnemyFactory;

import edu.hitsz.EnemyFactory.Manager.ManagerContext;
import edu.hitsz.aircraft.EnemyAircraft.AbstractEnemy;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;
import edu.hitsz.application.Main;

public class BossEnemyFactory implements EnemyFactory {
    private int hp;
    private int speedX;

    public BossEnemyFactory() {
        this(500, 5);
    }

    public BossEnemyFactory(int hp, int speedX) {
        this.hp = hp;
        this.speedX = speedX;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(ManagerContext context) {
        int spawnX = Main.WINDOW_WIDTH / 2;
        int spawnY = 60; // near top
        return new BossEnemy(spawnX, spawnY, speedX, 0, hp);
    }
}

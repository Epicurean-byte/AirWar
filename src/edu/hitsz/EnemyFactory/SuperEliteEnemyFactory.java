package edu.hitsz.EnemyFactory;

import edu.hitsz.EnemyFactory.Manager.ManagerContext;
import edu.hitsz.aircraft.EnemyAircraft.AbstractEnemy;
import edu.hitsz.aircraft.EnemyAircraft.SuperEliteEnemy;
import edu.hitsz.application.ImageManager;

public class SuperEliteEnemyFactory implements EnemyFactory {
    private final int speedY;
    private final int hp;

    public SuperEliteEnemyFactory() {
        this(4, 180);
    }

    public SuperEliteEnemyFactory(int speedY, int hp) {
        this.speedY = speedY;
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(ManagerContext context) {
        int spawnX = (int) (Math.random() * (context.screenWidth - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int spawnY = (int) (Math.random() * context.screenHeight * 0.05);
        int speedX = Math.random() < 0.5 ? -4 : 4;
        return new SuperEliteEnemy(spawnX, spawnY, speedX, speedY, hp);
    }
}

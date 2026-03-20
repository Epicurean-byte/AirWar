package edu.hitsz.EnemyFactory;

import edu.hitsz.EnemyFactory.Manager.ManagerContext;
import edu.hitsz.aircraft.EnemyAircraft.AbstractEnemy;
import edu.hitsz.aircraft.EnemyAircraft.EliteEnemy;
import edu.hitsz.application.ImageManager;

public class EliteEnemyFactory implements EnemyFactory{
    private final int speedY;
    private final int hp;

    public EliteEnemyFactory() {
        this(6, 120);
    }

    public EliteEnemyFactory(int speedY, int hp) {
        this.speedY = speedY;
        this.hp = hp;
    }

    @Override
    public AbstractEnemy createEnemy(ManagerContext context) {
        int spawnX = (int) (Math.random() * (context.screenWidth - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int spawnY = (int) (Math.random() * context.screenHeight * 0.05);
        return new EliteEnemy(spawnX, spawnY, 0, speedY, hp);
    }
}

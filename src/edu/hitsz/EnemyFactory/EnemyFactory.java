package edu.hitsz.EnemyFactory;

import edu.hitsz.EnemyFactory.Manager.ManagerContext;
import edu.hitsz.aircraft.EnemyAircraft.AbstractEnemy;

public interface EnemyFactory {
    public abstract AbstractEnemy createEnemy(ManagerContext context);
}
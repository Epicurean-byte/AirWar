package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.entity.AbstractEnemy;

public interface EnemyFactory {
    AbstractEnemy createEnemy(SpawnContext context);
}

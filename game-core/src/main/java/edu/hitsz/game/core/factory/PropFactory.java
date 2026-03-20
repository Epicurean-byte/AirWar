package edu.hitsz.game.core.factory;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.entity.AbstractProp;

public interface PropFactory {
    AbstractProp createProp(GameSessionConfig config, float x, float y);
}

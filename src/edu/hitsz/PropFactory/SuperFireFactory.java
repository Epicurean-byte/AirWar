package edu.hitsz.PropFactory;

import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.SuperFireSupply;

public class SuperFireFactory implements PropFactory {
    @Override
    public AbstractProp createProp(int locationX, int locationY) {
        return new SuperFireSupply(locationX, locationY, 0, 3);
    }
}


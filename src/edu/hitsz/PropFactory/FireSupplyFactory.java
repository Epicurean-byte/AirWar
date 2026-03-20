package edu.hitsz.PropFactory;

import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.FireSupply;

public class FireSupplyFactory implements PropFactory {
    @Override
    public AbstractProp createProp(int x, int y) {
        return new FireSupply(x, y, 0, 3);
    }
}

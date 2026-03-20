package edu.hitsz.PropFactory;

import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BloodSupply;

public class BloodSupplyFactory implements PropFactory {
    @Override
    public AbstractProp createProp(int x, int y) {
        return new BloodSupply(x, y, 0, 3);
    }
}

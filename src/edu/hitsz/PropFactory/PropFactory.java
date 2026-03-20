package edu.hitsz.PropFactory;

import edu.hitsz.prop.AbstractProp;

public interface PropFactory {
    public AbstractProp createProp(int x, int y);
}

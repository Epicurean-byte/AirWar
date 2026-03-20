package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.Game;
import edu.hitsz.application.ImageManager;

import java.awt.image.BufferedImage;

public class BloodSupply extends AbstractProp{
    public BloodSupply(int locationX, int locationY, int speedX, int speedY){
        super(locationX, locationY, speedX, speedY);
    }
    @Override
    public int activate(Game game) {
        HeroAircraft heroAircraft = game.getHeroAircraft();
        int heal = 20;
        if (heroAircraft.getHp() + heal < heroAircraft.getMaxHp()) {
            heroAircraft.increaseHp(heal);
        }else{
            heroAircraft.increaseHp(heroAircraft.getMaxHp() - heroAircraft.getHp());
        }
        return 0;
    }
    @Override
    public BufferedImage getImage() {
        return ImageManager.BLOOD_SUPPLY_IMAGE;
    }
}

package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.Game;
import edu.hitsz.application.ImageManager;
import edu.hitsz.basic.AbstractFlyingObject;

import java.awt.image.BufferedImage;

public class FireSupply extends AbstractProp {
    public FireSupply(int locationX, int locationY, int speedX, int speedY){
        super(locationX, locationY, speedX, speedY);
    }
    @Override
    public int activate(Game game){
        HeroAircraft heroAircraft = game.getHeroAircraft();
        System.out.println("FireSupply active! Switching to scatter mode for 3s.");
        heroAircraft.applyTimedFireMode(HeroAircraft.FireMode.SCATTER, 3000);
        return 0;
    }
    @Override
    public BufferedImage getImage() {
        return ImageManager.FIRE_SUPPLY_IMAGE;
    }
}

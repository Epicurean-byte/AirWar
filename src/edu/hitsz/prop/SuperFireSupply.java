package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.Game;
import edu.hitsz.application.ImageManager;

import java.awt.image.BufferedImage;

public class SuperFireSupply extends AbstractProp {

    public SuperFireSupply(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public int activate(Game game) {
        HeroAircraft heroAircraft = game.getHeroAircraft();
        System.out.println("SuperFireSupply active! Switching to ring mode for 3s.");
        heroAircraft.applyTimedFireMode(HeroAircraft.FireMode.RING, 3000);
        return 0;
    }

    @Override
    public BufferedImage getImage() {
        // Reuse fire supply image as placeholder; user can update ImageManager later.
        return ImageManager.SUPER_FIRE_SUPPLY_IMAGE;
    }
}

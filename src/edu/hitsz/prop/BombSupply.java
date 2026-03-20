package edu.hitsz.prop;

import edu.hitsz.application.Game;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bomb.BombEventPublisher;

import java.awt.image.BufferedImage;

public class BombSupply extends AbstractProp {
    public BombSupply(int locationX, int locationY, int speedX, int speedY){
        super(locationX,locationY,speedX,speedY);
    }
    @Override
    public int activate(Game game){
        System.out.println("BoobSupply active! Explosion effect.");
        edu.hitsz.audio.SoundManager.playEffect(edu.hitsz.audio.SoundManager.Effect.BOMB_EXPLOSION);
        int gained = BombEventPublisher.getInstance().notifyObservers();
        if (gained > 0) {
            System.out.println("Bomb cleared enemies, gained score: " + gained);
        }
        return gained;
    }
    @Override
    public BufferedImage getImage() {
        return ImageManager.BOMB_SUPPLY_IMAGE;
    }
}

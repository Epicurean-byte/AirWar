package edu.hitsz.application;


import edu.hitsz.aircraft.EnemyAircraft.EliteEnemy;
import edu.hitsz.aircraft.EnemyAircraft.SuperEliteEnemy;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.EnemyAircraft.MobEnemy;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.BloodSupply;
import edu.hitsz.prop.BombSupply;
import edu.hitsz.prop.FireSupply;
import edu.hitsz.prop.SuperFireSupply;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 综合管理图片的加载，访问
 * 提供图片的静态访问方法
 *
 * @author hitsz
 */
public class ImageManager {

    /**
     * 类名-图片 映射，存储各基类的图片 <br>
     * 可使用 CLASSNAME_IMAGE_MAP.get( obj.getClass().getName() ) 获得 obj 所属基类对应的图片
     */
    private static final Map<String, BufferedImage> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static BufferedImage BACKGROUND_IMAGE;
    private static BufferedImage BACKGROUND_IMAGE_EASY;
    private static BufferedImage BACKGROUND_IMAGE_NORMAL;
    private static BufferedImage BACKGROUND_IMAGE_HARD;
    public static BufferedImage HERO_IMAGE;
    public static BufferedImage HERO_BULLET_IMAGE;
    public static BufferedImage ENEMY_BULLET_IMAGE;
    public static BufferedImage MOB_ENEMY_IMAGE;
    public static BufferedImage ELITE_IMAGE;
    public static BufferedImage BLOOD_SUPPLY_IMAGE;
    public static BufferedImage BOMB_SUPPLY_IMAGE;
    public static BufferedImage FIRE_SUPPLY_IMAGE;
    public static BufferedImage SUPER_ELITE_IMAGE;
    public static BufferedImage BOSS_ELITE_IMAGE;
    public static BufferedImage SUPER_FIRE_SUPPLY_IMAGE;

    static {
        try {

            BACKGROUND_IMAGE = ImageIO.read(new FileInputStream("src/images/bg.jpg"));
            try { BACKGROUND_IMAGE_EASY = ImageIO.read(new FileInputStream("src/images/bg.jpg")); } catch (IOException ignored) {}
            try { BACKGROUND_IMAGE_NORMAL = ImageIO.read(new FileInputStream("src/images/bg2.jpg")); } catch (IOException ignored) {}
            try { BACKGROUND_IMAGE_HARD = ImageIO.read(new FileInputStream("src/images/bg3.jpg")); } catch (IOException ignored) {}

            HERO_IMAGE = ImageIO.read(new FileInputStream("src/images/hero.png"));
            MOB_ENEMY_IMAGE = ImageIO.read(new FileInputStream("src/images/mob.png"));
            HERO_BULLET_IMAGE = ImageIO.read(new FileInputStream("src/images/bullet_hero.png"));
            ENEMY_BULLET_IMAGE = ImageIO.read(new FileInputStream("src/images/bullet_enemy.png"));
            ELITE_IMAGE = ImageIO.read(new FileInputStream("src/images/elite.png"));
            BLOOD_SUPPLY_IMAGE = ImageIO.read(new FileInputStream("src/images/prop_blood.png"));
            BOMB_SUPPLY_IMAGE =  ImageIO.read(new FileInputStream("src/images/prop_bomb.png"));
            FIRE_SUPPLY_IMAGE = ImageIO.read(new FileInputStream("src/images/prop_bullet.png"));
            SUPER_ELITE_IMAGE = ImageIO.read(new FileInputStream("src/images/elitePlus.png"));
            BOSS_ELITE_IMAGE = ImageIO.read(new FileInputStream("src/images/boss.png"));
            SUPER_FIRE_SUPPLY_IMAGE = ImageIO.read(new FileInputStream("src/images/prop_bulletPlus.png"));

            CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
            CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
            CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
            CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
            CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_IMAGE);
            CLASSNAME_IMAGE_MAP.put(SuperEliteEnemy.class.getName(),
                    SUPER_ELITE_IMAGE != null ? SUPER_ELITE_IMAGE : ELITE_IMAGE);
            CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(),
                    BOSS_ELITE_IMAGE != null ? BOSS_ELITE_IMAGE : MOB_ENEMY_IMAGE);
            CLASSNAME_IMAGE_MAP.put(BloodSupply.class.getName(), BLOOD_SUPPLY_IMAGE);
            CLASSNAME_IMAGE_MAP.put(BombSupply.class.getName(), BOMB_SUPPLY_IMAGE);
            CLASSNAME_IMAGE_MAP.put(FireSupply.class.getName(), FIRE_SUPPLY_IMAGE);
            CLASSNAME_IMAGE_MAP.put(SuperFireSupply.class.getName(), SUPER_FIRE_SUPPLY_IMAGE);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static BufferedImage get(String className){
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static BufferedImage get(Object obj){
        if (obj == null){
            return null;
        }
        return get(obj.getClass().getName());
    }

    /** 根据难度切换背景图片：默认使用 bg.jpg；normal -> bg2.jpg；hard -> bg3.jpg。*/
    public static void useBackgroundForDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case EASY -> { if (BACKGROUND_IMAGE_EASY != null) BACKGROUND_IMAGE = BACKGROUND_IMAGE_EASY; }
            case NORMAL -> { if (BACKGROUND_IMAGE_NORMAL != null) BACKGROUND_IMAGE = BACKGROUND_IMAGE_NORMAL; }
            case HARD -> { if (BACKGROUND_IMAGE_HARD != null) BACKGROUND_IMAGE = BACKGROUND_IMAGE_HARD; }
        }
    }

}

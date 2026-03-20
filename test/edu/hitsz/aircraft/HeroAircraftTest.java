package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HeroAircraftTest {

    private Field instanceField;
    private HeroAircraft hero;

    @BeforeEach
    void setUp() throws Exception {
        instanceField = HeroAircraft.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        hero = HeroAircraft.getInstance();
        hero.setFireMode(HeroAircraft.FireMode.STRAIGHT);
        hero.setLocation(Main.WINDOW_WIDTH / 2, Main.WINDOW_HEIGHT / 2);
    }

    @AfterEach
    void tearDown() throws Exception {
        instanceField.set(null, null);
    }

    @Test
    void getInstance() {
        HeroAircraft another = HeroAircraft.getInstance();
        assertSame(hero, another, "HeroAircraft should follow singleton pattern");
        assertTrue(hero.getHp() > 0);
    }

    @Test
    void forward() {
        int originX = hero.getLocationX();
        int originY = hero.getLocationY();
        hero.forward();
        assertEquals(originX, hero.getLocationX(), "Hero should not move horizontally in forward()");
        assertEquals(originY, hero.getLocationY(), "Hero should not move vertically in forward()");
    }

    @Test
    void shoot() {
        hero.setFireMode(HeroAircraft.FireMode.STRAIGHT);
        List<BaseBullet> bullets = hero.shoot();
        assertEquals(1, bullets.size(), "Straight mode should emit exactly one bullet");
        BaseBullet bullet = bullets.get(0);
        assertEquals(hero.getLocationX(), bullet.getLocationX());
        assertTrue(bullet.getLocationY() < hero.getLocationY(), "Hero bullet should spawn above hero");
    }

    @Test
    void setFireMode() {
        hero.setFireMode(HeroAircraft.FireMode.SCATTER);
        List<BaseBullet> scatter = hero.shoot();
        assertEquals(3, scatter.size(), "Scatter mode should fire three bullets");
        Set<Integer> distinctX = scatter.stream().map(BaseBullet::getLocationX).collect(Collectors.toSet());
        assertEquals(3, distinctX.size(), "Scatter bullets should have different horizontal positions");

        hero.setFireMode(HeroAircraft.FireMode.RING);
        List<BaseBullet> ring = hero.shoot();
        assertEquals(12, ring.size(), "Ring mode should fire twelve bullets");
    }

    @Test
    void cycleFireMode() {
        hero.setFireMode(HeroAircraft.FireMode.STRAIGHT);
        hero.cycleFireMode();
        assertEquals(3, hero.shoot().size(), "After first cycle fire mode should be scatter");
        hero.cycleFireMode();
        assertEquals(12, hero.shoot().size(), "After second cycle fire mode should be ring");
        hero.cycleFireMode();
        assertEquals(1, hero.shoot().size(), "After third cycle fire mode should return to straight");

        int hpBefore = hero.getHp();
        hero.decreaseHp(20);
        assertEquals(hpBefore - 20, hero.getHp(), "HP should decrease via AbstractAircraft.decreaseHp()");
    }
}

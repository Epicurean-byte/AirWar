package edu.hitsz.game.core.engine;

import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.entity.AbstractAircraft;
import edu.hitsz.game.core.entity.AbstractEnemy;
import edu.hitsz.game.core.entity.AbstractFlyingObject;
import edu.hitsz.game.core.entity.AbstractProp;
import edu.hitsz.game.core.entity.BaseBullet;
import edu.hitsz.game.core.entity.BombSupply;
import edu.hitsz.game.core.entity.BossEnemy;
import edu.hitsz.game.core.entity.HeroAircraft;
import edu.hitsz.game.core.event.BombEventPublisher;
import edu.hitsz.game.core.event.BombObserver;
import edu.hitsz.game.core.event.GameEvent;
import edu.hitsz.game.core.factory.BloodSupplyFactory;
import edu.hitsz.game.core.factory.BombFactory;
import edu.hitsz.game.core.factory.BossEnemyFactory;
import edu.hitsz.game.core.factory.EnemyFactoryManager;
import edu.hitsz.game.core.factory.FireSupplyFactory;
import edu.hitsz.game.core.factory.PropDropSelector;
import edu.hitsz.game.core.factory.SpawnContext;
import edu.hitsz.game.core.factory.SuperFireFactory;
import edu.hitsz.game.core.mode.AbstractGameMode;
import edu.hitsz.game.core.mode.Difficulty;
import edu.hitsz.game.core.mode.GameModeFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class GameWorld {
    private final Difficulty difficulty;
    private final GameSessionConfig sessionConfig;
    private final HeroAircraft heroAircraft;
    private final List<AbstractEnemy> enemyAircrafts = new LinkedList<>();
    private final List<BaseBullet> heroBullets = new LinkedList<>();
    private final List<BaseBullet> enemyBullets = new LinkedList<>();
    private final List<AbstractProp> props = new LinkedList<>();
    private final BombEventPublisher bombPublisher = new BombEventPublisher();
    private final PropDropSelector propDropSelector;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    private final AbstractGameMode gameMode;

    private EnemyFactoryManager enemyFactoryManager;
    private BossEnemyFactory bossEnemyFactory = new BossEnemyFactory();
    private int enemyMaxNumber = 5;
    private boolean bossEnabled = true;
    private int bossScoreThreshold = 200;
    private int nextBossScore = bossScoreThreshold;
    private int bossSpawnCount = 0;
    private int score = 0;
    private int timeMs = 0;
    private int cycleDuration = 300;
    private int cycleTime = 0;
    private int backgroundOffset = 0;
    private boolean gameOver = false;

    private GameWorld(Difficulty difficulty, GameSessionConfig sessionConfig) {
        this.difficulty = difficulty;
        this.sessionConfig = sessionConfig;
        this.heroAircraft = HeroAircraft.createDefault(sessionConfig);
        this.propDropSelector = new PropDropSelector(
                new BloodSupplyFactory(),
                new BombFactory(),
                new FireSupplyFactory(),
                new SuperFireFactory(),
                0.80,
                0.40,
                0.20,
                0.25,
                0.15
        );
        this.gameMode = GameModeFactory.create(difficulty);
        this.gameMode.apply(this);
    }

    public static GameWorld create(Difficulty difficulty, int worldWidth, int worldHeight) {
        return create(difficulty, GameSessionConfig.defaultConfig(worldWidth, worldHeight));
    }

    public static GameWorld create(Difficulty difficulty, GameSessionConfig sessionConfig) {
        return new GameWorld(difficulty, sessionConfig);
    }

    public void update(int deltaMs, long now) {
        if (gameOver) {
            return;
        }

        timeMs += deltaMs;
        backgroundOffset = (backgroundOffset + Math.max(1, deltaMs / 16)) % sessionConfig.getWorldHeight();
        heroAircraft.refreshTimedState(now);

        if (timeCountAndNewCycleJudge(deltaMs)) {
            gameMode.update(this, timeMs, score);
            spawnEnemyIfNeeded();
            shootAction(now);
        }

        bulletsMoveAction();
        aircraftsMoveAction();
        propsMoveAction();
        crashCheckAction(now);
        postProcessAction();

        if (heroAircraft.getHp() <= 0) {
            gameOver = true;
            emitEvent(new GameEvent(GameEvent.Type.GAME_OVER));
        }
    }

    public void moveHeroTo(float x, float y) {
        if (!gameOver) {
            heroAircraft.moveTo(x, y, sessionConfig);
        }
    }

    public GameSnapshot snapshot() {
        List<RenderSprite> renderSprites = new ArrayList<>();
        addRenderSprites(renderSprites, enemyBullets);
        addRenderSprites(renderSprites, heroBullets);
        addRenderSprites(renderSprites, enemyAircrafts);
        addRenderSprites(renderSprites, props);
        renderSprites.add(new RenderSprite(
                heroAircraft.getSpriteId(),
                heroAircraft.getLocationX(),
                heroAircraft.getLocationY(),
                heroAircraft.getWidth(),
                heroAircraft.getHeight()
        ));
        return new GameSnapshot(
                difficulty,
                sessionConfig.getWorldWidth(),
                sessionConfig.getWorldHeight(),
                backgroundOffset,
                score,
                heroAircraft.getHp(),
                heroAircraft.getMaxHp(),
                gameOver,
                renderSprites
        );
    }

    public List<GameEvent> drainEvents() {
        List<GameEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    public void emitEvent(GameEvent event) {
        pendingEvents.add(event);
    }

    public int activateBomb() {
        return bombPublisher.notifyObservers();
    }

    private boolean timeCountAndNewCycleJudge(int deltaMs) {
        cycleTime += deltaMs;
        if (cycleTime >= cycleDuration) {
            cycleTime %= cycleDuration;
            return true;
        }
        return false;
    }

    private void spawnEnemyIfNeeded() {
        if (enemyAircrafts.size() >= enemyMaxNumber) {
            return;
        }
        SpawnContext context = new SpawnContext(timeMs, enemyAircrafts.size(), enemyMaxNumber, sessionConfig);
        boolean bossAlive = enemyAircrafts.stream().anyMatch(enemy -> enemy instanceof BossEnemy && !enemy.notValid());
        if (bossEnabled && !bossAlive && score >= nextBossScore) {
            AbstractEnemy boss = bossEnemyFactory.createEnemy(context);
            enemyAircrafts.add(boss);
            registerBombObserver(boss);
            if (boss instanceof BossEnemy bossEnemy) {
                incrementBossSpawnCount();
                gameMode.onBossSpawn(this, bossEnemy, bossSpawnCount);
            }
            emitEvent(new GameEvent(GameEvent.Type.BOSS_SPAWN));
            nextBossScore += bossScoreThreshold;
            return;
        }
        AbstractEnemy enemy = enemyFactoryManager.createEnemy(context);
        if (enemy != null) {
            enemyAircrafts.add(enemy);
            registerBombObserver(enemy);
        }
    }

    private void shootAction(long now) {
        for (AbstractEnemy enemy : enemyAircrafts) {
            enemyBullets.addAll(enemy.shoot(sessionConfig, now));
        }
        heroBullets.addAll(heroAircraft.shoot(sessionConfig, now));
    }

    private void bulletsMoveAction() {
        heroBullets.forEach(bullet -> bullet.forward(sessionConfig));
        enemyBullets.forEach(bullet -> bullet.forward(sessionConfig));
    }

    private void aircraftsMoveAction() {
        enemyAircrafts.forEach(enemy -> enemy.forward(sessionConfig));
    }

    private void propsMoveAction() {
        props.forEach(prop -> prop.forward(sessionConfig));
    }

    private void crashCheckAction(long now) {
        for (BaseBullet bullet : enemyBullets) {
            if (!bullet.notValid() && heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractEnemy enemy : enemyAircrafts) {
                if (enemy.notValid()) {
                    continue;
                }
                if (enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    emitEvent(new GameEvent(GameEvent.Type.BULLET_HIT));
                    if (enemy.notValid()) {
                        onEnemyDestroyed(enemy);
                    }
                    break;
                }
            }
        }

        for (AbstractEnemy enemy : enemyAircrafts) {
            if (enemy.notValid()) {
                continue;
            }
            if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                enemy.vanish();
                heroAircraft.decreaseHp(Integer.MAX_VALUE);
                onEnemyDestroyed(enemy);
            }
        }

        Iterator<AbstractProp> iterator = props.iterator();
        while (iterator.hasNext()) {
            AbstractProp prop = iterator.next();
            if (prop.crash(heroAircraft)) {
                int gained = prop.activate(this, now);
                if (!(prop instanceof BombSupply)) {
                    emitEvent(new GameEvent(GameEvent.Type.GET_SUPPLY));
                }
                score += gained;
                prop.vanish();
            }
            if (prop.notValid()) {
                iterator.remove();
            }
        }
    }

    private void onEnemyDestroyed(AbstractEnemy enemy) {
        score += enemy.getScoreValue();
        AbstractProp prop = propDropSelector.createFor(enemy, sessionConfig);
        if (prop != null) {
            props.add(prop);
        }
        if (enemy instanceof BossEnemy) {
            emitEvent(new GameEvent(GameEvent.Type.BOSS_DEFEATED));
        }
    }

    private void postProcessAction() {
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(enemy -> {
            if (enemy.notValid()) {
                unregisterBombObserver(enemy);
                return true;
            }
            return false;
        });
    }

    private void addRenderSprites(List<RenderSprite> renderSprites, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            renderSprites.add(new RenderSprite(
                    object.getSpriteId(),
                    object.getLocationX(),
                    object.getLocationY(),
                    object.getWidth(),
                    object.getHeight()
            ));
        }
    }

    private void registerBombObserver(Object candidate) {
        if (candidate instanceof BombObserver observer) {
            bombPublisher.register(observer);
        }
    }

    private void unregisterBombObserver(Object candidate) {
        if (candidate instanceof BombObserver observer) {
            bombPublisher.unregister(observer);
        }
    }

    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameSessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getBackgroundOffset() {
        return backgroundOffset;
    }

    public void setEnemyFactoryManager(EnemyFactoryManager enemyFactoryManager) {
        this.enemyFactoryManager = enemyFactoryManager;
    }

    public EnemyFactoryManager getEnemyFactoryManager() {
        return enemyFactoryManager;
    }

    public void setEnemyMaxNumber(int enemyMaxNumber) {
        this.enemyMaxNumber = Math.max(1, enemyMaxNumber);
    }

    public int getEnemyMaxNumber() {
        return enemyMaxNumber;
    }

    public void setCycleDuration(int cycleDuration) {
        this.cycleDuration = Math.max(80, cycleDuration);
    }

    public int getCycleDuration() {
        return cycleDuration;
    }

    public void enableBoss(boolean enable) {
        this.bossEnabled = enable;
        this.nextBossScore = enable ? bossScoreThreshold : Integer.MAX_VALUE;
    }

    public boolean isBossEnabled() {
        return bossEnabled;
    }

    public void setBossScoreThreshold(int bossScoreThreshold) {
        this.bossScoreThreshold = bossScoreThreshold;
        if (bossEnabled) {
            nextBossScore = score + bossScoreThreshold;
        }
    }

    public int getBossScoreThreshold() {
        return bossScoreThreshold;
    }

    public void resetNextBossScore() {
        if (bossEnabled) {
            nextBossScore = bossScoreThreshold;
        }
    }

    public void setBossEnemyFactory(BossEnemyFactory bossEnemyFactory) {
        if (bossEnemyFactory != null) {
            this.bossEnemyFactory = bossEnemyFactory;
        }
    }

    public BossEnemyFactory getBossEnemyFactory() {
        return bossEnemyFactory;
    }

    public void incrementBossSpawnCount() {
        bossSpawnCount++;
    }

    public int getBossSpawnCount() {
        return bossSpawnCount;
    }
}

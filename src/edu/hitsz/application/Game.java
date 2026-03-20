package edu.hitsz.application;

import edu.hitsz.EnemyFactory.Manager.EnemyFactoryManager;
import edu.hitsz.EnemyFactory.Manager.ManagerContext;
import edu.hitsz.PropFactory.PropDropSelector;
import edu.hitsz.PropFactory.*;
import edu.hitsz.aircraft.*;
import edu.hitsz.aircraft.EnemyAircraft.AbstractEnemy;
import edu.hitsz.aircraft.EnemyAircraft.BossEnemy;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.prop.AbstractProp;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import edu.hitsz.EnemyFactory.*;
import edu.hitsz.audio.SoundManager;
import edu.hitsz.bomb.BombEventPublisher;
import edu.hitsz.bomb.BombObserver;
import edu.hitsz.application.mode.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * 游戏主面板，游戏启动
 *
 * @author hitsz
 */
public class Game extends JPanel {
    private final Difficulty difficulty;
    private final boolean soundOn;
    private EnemyFactoryManager enemyFactoryManager;
    private BossEnemyFactory bossEnemyFactory = new BossEnemyFactory();
    private PropDropSelector propDropSelector;
    private int backGroundTop = 0;

    /**
     * Scheduled 线程池，用于任务调度
     */
    private final ScheduledExecutorService executorService;

    /**
     * 时间间隔(ms)，控制刷新频率
     */
    private int timeInterval = 40;

    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircrafts;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;
    private final BombEventPublisher bombPublisher = BombEventPublisher.getInstance();

    /**
     * 屏幕中出现的敌机最大数量
     */
    private int enemyMaxNumber = 5;

    // Boss control
    private boolean bossEnabled = true;
    private int bossScoreThreshold = 200;
    private int nextBossScore = bossScoreThreshold;
    private int bossSpawnCount = 0;

    /**
     * 当前得分
     */
    private int score = 0;
    /**
     * 当前时刻
     */
    private int time = 0;

    /**
     * 周期（ms)
     * 指示子弹的发射、敌机的产生频率
     */
    private int cycleDuration = 300;
    private int cycleTime = 0;

    /**
     * 游戏结束标志
     */
    private boolean gameOverFlag = false;
    private boolean scoreRecorded = false;
    private AbstractGameMode gameMode;

    public Game(Difficulty difficulty, boolean soundOn) {
        this.difficulty = difficulty;
        this.soundOn = soundOn;
        heroAircraft = HeroAircraft.getInstance();
        props = new LinkedList<>();
        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        // 根据难度切换背景
        ImageManager.useBackgroundForDifficulty(difficulty);

        // 初始化音效
        SoundManager.init(soundOn);
        SoundManager.startBgm();

        this.propDropSelector = new PropDropSelector(
                new BloodSupplyFactory(),
                new BombFactory(),
                new FireSupplyFactory(),
                new SuperFireFactory(),
                0.80,  // 总掉率
                0.4,   // 血
                0.2,   // 炸
                0.25,  // 火
                0.15   // 超级火力
        );

        this.gameMode = GameModeFactory.create(difficulty);
        this.gameMode.apply(this);
        /**
         * Scheduled 线程池，用于定时任务调度
         * 关于alibaba code guide：可命名的 ThreadFactory 一般需要第三方包
         * apache 第三方库： org.apache.commons.lang3.concurrent.BasicThreadFactory
         */
        this.executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("game-action-%d").daemon(true).build());

        //启动英雄机鼠标监听
        new HeroController(this, heroAircraft);

    }

    /**
     * 游戏启动入口，执行游戏逻辑
     */
    public void action() {

        // 定时任务：绘制、对象产生、碰撞判定、击毁及结束判定
        Runnable task = () -> {

            time += timeInterval;

            // 周期性执行（控制频率）
            if (timeCountAndNewCycleJudge()) {
                gameMode.update(this, time, score);
                System.out.println(time);
                // 新敌机产生
                if (enemyAircrafts.size() < enemyMaxNumber) {
                    ManagerContext context = new ManagerContext(
                            time, enemyAircrafts.size(), enemyMaxNumber,
                            Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT
                    );
                    // Boss check first
                    boolean bossAlive = enemyAircrafts.stream().anyMatch(e -> e instanceof BossEnemy && !e.notValid());
                    if (bossEnabled && !bossAlive && score >= nextBossScore) {
                        AbstractAircraft boss = bossEnemyFactory.createEnemy(context);
                        enemyAircrafts.add(boss);
                        if (boss instanceof BombObserver observer) {
                            bombPublisher.register(observer);
                        }
                        if (boss instanceof BossEnemy bossEnemy) {
                            incrementBossSpawnCount();
                            gameMode.onBossSpawn(this, bossEnemy, bossSpawnCount);
                        }
                        SoundManager.startBossBgm();
                        nextBossScore += bossScoreThreshold; // next threshold
                    } else {
                        AbstractAircraft enemy = enemyFactoryManager.createEnemy(context);
                        if (enemy != null) {
                            enemyAircrafts.add(enemy);
                            if (enemy instanceof BombObserver observer) {
                                bombPublisher.register(observer);
                            }
                        }
                    }
                }
                // 飞机射出子弹
                shootAction();
            }

            // 子弹移动
            bulletsMoveAction();

            // 飞机移动
            aircraftsMoveAction();

            //道具移动
            propsMoveAction();

            // 撞击检测
            crashCheckAction();

            // 后处理
            postProcessAction();

            //每个时刻重绘界面
            repaint();

            // 游戏结束检查英雄机是否存活
            if (heroAircraft.getHp() <= 0) {
                // 游戏结束
                executorService.shutdown();
                gameOverFlag = true;
                System.out.println("Game Over!");
                SoundManager.playEffect(SoundManager.Effect.GAME_OVER);
                SoundManager.stopAll();
                if (!scoreRecorded) {
                    SwingUtilities.invokeLater(() -> {
                        String diffName = difficulty.toString();
                        String file = "data/scores_" + difficulty.name().toLowerCase() + ".txt";
                        String name = JOptionPane.showInputDialog(Game.this,
                                "游戏结束，得分：" + score + "\n请输入名字保存成绩：",
                                "输入", JOptionPane.QUESTION_MESSAGE);
                        if (name == null || name.trim().isEmpty()) name = "Player";
                        new edu.hitsz.score.ScoreBoard(new edu.hitsz.score.FileScoreDao(file))
                                .recordScore(name, score);
                        new LeaderboardFrame(difficulty).setVisible(true);
                    });
                    scoreRecorded = true;
                }
            }

        };

        /**
         * 以固定延迟时间进行执行
         * 本次任务执行完成后，需要延迟设定的延迟时间，才会执行新的任务
         */
        executorService.scheduleWithFixedDelay(task, timeInterval, timeInterval, TimeUnit.MILLISECONDS);

    }

    //***********************
    //      Action 各部分
    //***********************

    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            // 跨越到新的周期
            cycleTime %= cycleDuration;
            return true;
        } else {
            return false;
        }
    }

    private void shootAction() {
        long now = System.currentTimeMillis();
        for (AbstractAircraft enemy : enemyAircrafts){
            if (enemy instanceof AbstractEnemy shooter){
                List<BaseBullet> newBullets = shooter.shoot(now);
                enemyBullets.addAll(newBullets);
                for (BaseBullet bullet : newBullets) {
                    if (bullet instanceof BombObserver observer) {
                        bombPublisher.register(observer);
                    }
                }
            }
        }
        // 英雄射击
        heroBullets.addAll(heroAircraft.shoot());
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp p : props) {
            p.forward(); // 确保道具 speedY > 0，向下飞
        }
    }


    /**
     * 碰撞检测：
     * 1. 敌机攻击英雄
     * 2. 英雄攻击/撞击敌机
     * 3. 英雄获得补给
     */
    private void crashCheckAction() {
        // 敌机子弹攻击英雄
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) continue;
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    // 已被其他子弹击毁的敌机，不再检测
                    // 避免多个子弹重复击毁同一敌机的判定
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    // 敌机撞击到英雄机子弹
                    // 敌机损失一定生命值
                    enemyAircraft.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    SoundManager.playEffect(SoundManager.Effect.BULLET_HIT);
                    if (enemyAircraft.notValid()) {
                        //获得分数，产生道具补给
                        score += 10;
                        var prop = propDropSelector.createFor(enemyAircraft);
                        if (prop != null) {
                            props.add(prop);
                        }
                        if (enemyAircraft instanceof BossEnemy) {
                            SoundManager.stopBossBgm();
                        }
                    }
                }
                // 英雄机 与 敌机 相撞，均损毁
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        //我方获得道具，道具生效
        Iterator<AbstractProp> it = props.iterator();
        while (it.hasNext()) {
            AbstractProp p = it.next();
            if (p.crash(heroAircraft)) {
                int gain = p.activate(this);
                if (!(p instanceof edu.hitsz.prop.BombSupply)) {
                    SoundManager.playEffect(SoundManager.Effect.GET_SUPPLY);
                }
                if (gain > 0) {
                    score += gain;
                }
                it.remove(); // 拾取后移除
            } else if (p.notValid()) {
                it.remove(); // 出界等无效移除
            }
        }

    }

    /**
     * 后处理：
     * 1. 删除无效的子弹
     * 2. 删除无效的敌机
     * <p>
     * 无效的原因可能是撞击或者飞出边界
     */
    private void postProcessAction() {
        enemyBullets.removeIf(b -> {
            if (b.notValid()) {
                if (b instanceof BombObserver observer) {
                    bombPublisher.unregister(observer);
                }
                return true;
            }
            return false;
        });
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(enemy -> {
            if (enemy.notValid()) {
                if (enemy instanceof BombObserver observer) {
                    bombPublisher.unregister(observer);
                }
                return true;
            }
            return false;
        });
    }


    //***********************
    //      Paint 各部分
    //***********************

    /**
     * 重写paint方法
     * 通过重复调用paint方法，实现游戏动画
     *
     * @param  g
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // 绘制背景,图片滚动
        g.drawImage(ImageManager.BACKGROUND_IMAGE, 0, this.backGroundTop - Main.WINDOW_HEIGHT, null);
        g.drawImage(ImageManager.BACKGROUND_IMAGE, 0, this.backGroundTop, null);
        this.backGroundTop += 1;
        if (this.backGroundTop == Main.WINDOW_HEIGHT) {
            this.backGroundTop = 0;
        }

        // 先绘制子弹，后绘制飞机
        // 这样子弹显示在飞机的下层
        paintImageWithPositionRevised(g, enemyBullets);
        paintImageWithPositionRevised(g, heroBullets);

        paintImageWithPositionRevised(g, enemyAircrafts);
        paintImageWithPositionRevised(g, props);

        g.drawImage(ImageManager.HERO_IMAGE, heroAircraft.getLocationX() - ImageManager.HERO_IMAGE.getWidth() / 2,
                heroAircraft.getLocationY() - ImageManager.HERO_IMAGE.getHeight() / 2, null);

        //绘制得分和生命值
        paintScoreAndLife(g);

    }

    private void paintImageWithPositionRevised(Graphics g, List<? extends AbstractFlyingObject> objects) {
        if (objects.size() == 0) {
            return;
        }

        for (AbstractFlyingObject object : objects) {
            BufferedImage image = object.getImage();
            assert image != null : objects.getClass().getName() + " has no image! ";
            g.drawImage(image, object.getLocationX() - image.getWidth() / 2,
                    object.getLocationY() - image.getHeight() / 2, null);
        }
    }

    private void paintScoreAndLife(Graphics g) {
        int x = 10;
        int y = 25;
        g.setColor(new Color(16711680));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("SCORE:" + this.score, x, y);
        y = y + 20;
        g.drawString("LIFE:" + this.heroAircraft.getHp(), x, y);
    }


    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    public void setEnemyFactoryManager(EnemyFactoryManager manager) {
        this.enemyFactoryManager = manager;
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

    public void setBossScoreThreshold(int threshold) {
        this.bossScoreThreshold = threshold;
        if (!bossEnabled) {
            return;
        }
        nextBossScore = score + threshold;
    }

    public int getBossScoreThreshold() {
        return bossScoreThreshold;
    }

    public void resetNextBossScore() {
        if (bossEnabled) {
            this.nextBossScore = bossScoreThreshold;
        }
    }

    public void setBossEnemyFactory(BossEnemyFactory factory) {
        if (factory != null) {
            this.bossEnemyFactory = factory;
        }
    }

    public BossEnemyFactory getBossEnemyFactory() {
        return bossEnemyFactory;
    }

    public int getScore() {
        return score;
    }

    public int getTimeMs() {
        return time;
    }

    public int getBossSpawnCount() {
        return bossSpawnCount;
    }

    public void incrementBossSpawnCount() {
        bossSpawnCount++;
    }


}

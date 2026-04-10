package edu.hitsz.aircraftwar.android.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.aircraftwar.android.R;
import edu.hitsz.aircraftwar.android.ui.ShopItemVisuals;

public class PvpBattleView extends View {

    public interface InputListener {
        void onMove(float x, float y);

        void onFire();
    }

    public static final class PlayerState {
        public long userId;
        public float x;
        public float y;
        public int hp;
        public long score;
        public long coins;
    }

    public static final class EnemyState {
        public int id;
        public int type;
        public float x;
        public float y;
        public int hp;
    }
    
    public static final class BulletState {
        public int id;
        public long ownerId; // 发射子弹的玩家ID
        public float x;
        public float y;
    }

    private static final float WORLD_WIDTH = 512f;
    private static final float WORLD_HEIGHT = 768f;

    private final Paint hudPanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyHpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyHpBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();

    private final Bitmap backgroundBitmap;
    private Bitmap myPlaneBitmap;
    private final Bitmap enemyPlaneBitmap;
    private final Bitmap mobEnemyBitmap;
    private final Bitmap eliteEnemyBitmap;
    private final Bitmap bossEnemyBitmap;
    private final Bitmap heroBulletBitmap;

    private long myUserId;
    private float myX = 256f;
    private float myY = 650f;
    private int myHp = 100;
    private long myScore = 0;
    private long myCoins = 0;

    private float enemyPlayerX = -1f;
    private float enemyPlayerY = -1f;
    private int enemyPlayerHp = 0;
    private Bitmap enemyPlayerBitmap; // 对手玩家的皮肤

    private final List<EnemyState> enemies = new ArrayList<>();
    private final List<BulletState> bullets = new ArrayList<>();
    private InputListener inputListener;
    private String gameMode = "COOP"; // 默认合作模式

    public PvpBattleView(Context context) {
        this(context, null);
    }

    public PvpBattleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_bg_normal);
        myPlaneBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_hero);
        enemyPlaneBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_elite_plus);
        mobEnemyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_mob);
        eliteEnemyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_elite);
        bossEnemyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_boss);
        heroBulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_bullet_hero);
        
        android.util.Log.d("PvpBattleView", "heroBulletBitmap is " + (heroBulletBitmap == null ? "NULL" : "loaded"));

        hudPanelPaint.setColor(0xA6171F14);
        hudStrokePaint.setColor(0xCCB99547);
        hudStrokePaint.setStyle(Paint.Style.STROKE);
        hudStrokePaint.setStrokeWidth(2.5f);

        hudTitlePaint.setColor(0xFFF4F4E8);
        hudTitlePaint.setFakeBoldText(true);
        hudTitlePaint.setTextSize(28f);

        hudBodyPaint.setColor(0xFFE1E6CF);
        hudBodyPaint.setTextSize(22f);

        enemyHpPaint.setColor(0xFFD95D47);
        enemyHpBackPaint.setColor(0x66110E0B);

        scanlinePaint.setColor(0x11FFFFFF);
        scanlinePaint.setStrokeWidth(1.2f);
    }

    public void setMyPlaneSkinId(int equippedSkinId) {
        int resId = ShopItemVisuals.resolveHeroDrawableResId(equippedSkinId);
        myPlaneBitmap = BitmapFactory.decodeResource(getResources(), resId);
        invalidate();
    }
    
    public void setEnemyPlayerSkinId(int skinId) {
        int resId = ShopItemVisuals.resolveHeroDrawableResId(skinId);
        enemyPlayerBitmap = BitmapFactory.decodeResource(getResources(), resId);
        invalidate();
    }
    
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
        invalidate();
    }

    public void setMyUserId(long myUserId) {
        this.myUserId = myUserId;
    }

    public void setInputListener(@Nullable InputListener listener) {
        this.inputListener = listener;
    }

    public void updateState(List<PlayerState> players, List<EnemyState> enemies, List<BulletState> bullets) {
        this.enemies.clear();
        this.enemies.addAll(enemies);
        
        this.bullets.clear();
        this.bullets.addAll(bullets);

        for (PlayerState p : players) {
            if (p.userId == myUserId) {
                myX = p.x;
                myY = p.y;
                myHp = p.hp;
                myScore = p.score;
                myCoins = p.coins;
            } else {
                enemyPlayerX = p.x;
                enemyPlayerY = p.y;
                enemyPlayerHp = p.hp;
            }
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawCombatLane(canvas);
        drawBullets(canvas);
        drawEnemies(canvas);
        drawPlayers(canvas);
        drawHud(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = screenToWorldX(event.getX());
        float y = screenToWorldY(event.getY());

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (inputListener != null) {
                inputListener.onMove(x, y);
                inputListener.onFire();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (inputListener != null) {
                inputListener.onMove(x, y);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundBitmap == null) {
            canvas.drawColor(0xFF0E1611);
            return;
        }
        tempRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(backgroundBitmap, null, tempRect, null);
    }

    private void drawCombatLane(Canvas canvas) {
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        for (int i = 1; i < 10; i++) {
            float y = i * height / 10f;
            canvas.drawLine(width * 0.08f, y, width * 0.92f, y, scanlinePaint);
        }
    }
    
    private void drawBullets(Canvas canvas) {
        android.util.Log.d("PvpBattleView", "Drawing " + bullets.size() + " bullets");
        for (BulletState bullet : bullets) {
            float screenX = worldToScreenX(bullet.x);
            float screenY = worldToScreenY(bullet.y);
            android.util.Log.d("PvpBattleView", "Bullet " + bullet.id + " world(" + bullet.x + ", " + bullet.y + ") screen(" + screenX + ", " + screenY + ")");
            drawSprite(canvas, heroBulletBitmap, screenX, screenY, 12f, 24f);
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (EnemyState enemy : enemies) {
            Bitmap bitmap = switch (enemy.type) {
                case 1 -> eliteEnemyBitmap;
                case 2, 3 -> bossEnemyBitmap;
                default -> mobEnemyBitmap;
            };
            float width = enemy.type >= 2 ? 88f : enemy.type == 1 ? 64f : 52f;
            float height = enemy.type >= 2 ? 88f : enemy.type == 1 ? 64f : 52f;
            drawSprite(canvas, bitmap, worldToScreenX(enemy.x), worldToScreenY(enemy.y), width, height);

            float hpWidth = width * 0.86f;
            float barLeft = worldToScreenX(enemy.x) - hpWidth / 2f;
            float barTop = worldToScreenY(enemy.y) - height / 2f - 12f;
            tempRect.set(barLeft, barTop, barLeft + hpWidth, barTop + 7f);
            canvas.drawRoundRect(tempRect, 4f, 4f, enemyHpBackPaint);
            float hpRatio = Math.max(0f, Math.min(1f, enemy.hp / 240f));
            tempRect.set(barLeft, barTop, barLeft + hpWidth * hpRatio, barTop + 7f);
            canvas.drawRoundRect(tempRect, 4f, 4f, enemyHpPaint);
        }
    }

    private void drawPlayers(Canvas canvas) {
        drawSprite(canvas, myPlaneBitmap, worldToScreenX(myX), worldToScreenY(myY), 76f, 76f);
        if (enemyPlayerX >= 0 && enemyPlayerY >= 0) {
            // 使用对手的实际皮肤，如果没有设置则使用默认
            Bitmap playerBitmap = enemyPlayerBitmap != null ? enemyPlayerBitmap : enemyPlaneBitmap;
            drawSprite(canvas, playerBitmap, worldToScreenX(enemyPlayerX), worldToScreenY(enemyPlayerY), 76f, 76f);
        }
    }

    private void drawHud(Canvas canvas) {
        float panelHeight = 116f;
        tempRect.set(20f, 18f, canvas.getWidth() - 20f, 18f + panelHeight);
        canvas.drawRoundRect(tempRect, 24f, 24f, hudPanelPaint);
        canvas.drawRoundRect(tempRect, 24f, 24f, hudStrokePaint);

        String modeText = "COOP".equals(gameMode) ? "合作模式" : "对战模式";
        canvas.drawText(modeText, 40f, 54f, hudTitlePaint);
        canvas.drawText("MY HP " + myHp + "   SCORE " + myScore + "   COINS " + myCoins, 40f, 84f, hudBodyPaint);
        
        if ("COOP".equals(gameMode)) {
            canvas.drawText("队友 HP " + Math.max(0, enemyPlayerHp) + "   敌机 " + enemies.size(), 40f, 110f, hudBodyPaint);
        } else {
            canvas.drawText("对手 HP " + Math.max(0, enemyPlayerHp) + "   敌机 " + enemies.size(), 40f, 110f, hudBodyPaint);
        }

        float radarTop = canvas.getHeight() - 96f;
        tempRect.set(20f, radarTop, canvas.getWidth() - 20f, canvas.getHeight() - 20f);
        canvas.drawRoundRect(tempRect, 24f, 24f, hudPanelPaint);
        canvas.drawRoundRect(tempRect, 24f, 24f, hudStrokePaint);
        canvas.drawText("拖动控制机体，按下时自动开火", 36f, canvas.getHeight() - 48f, hudBodyPaint);
    }

    private void drawSprite(Canvas canvas, @Nullable Bitmap bitmap, float centerX, float centerY, float drawWidth, float drawHeight) {
        if (bitmap == null) {
            return;
        }
        float pixelWidth = drawWidth / WORLD_WIDTH * getWidth();
        float pixelHeight = drawHeight / WORLD_HEIGHT * getHeight();
        tempRect.set(
                centerX - pixelWidth / 2f,
                centerY - pixelHeight / 2f,
                centerX + pixelWidth / 2f,
                centerY + pixelHeight / 2f
        );
        canvas.drawBitmap(bitmap, null, tempRect, null);
    }

    private float worldToScreenX(float x) {
        return x / WORLD_WIDTH * getWidth();
    }

    private float worldToScreenY(float y) {
        return y / WORLD_HEIGHT * getHeight();
    }

    private float screenToWorldX(float x) {
        return x / Math.max(1f, getWidth()) * WORLD_WIDTH;
    }

    private float screenToWorldY(float y) {
        return y / Math.max(1f, getHeight()) * WORLD_HEIGHT;
    }
}

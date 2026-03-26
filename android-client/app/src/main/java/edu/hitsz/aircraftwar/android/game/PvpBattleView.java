package edu.hitsz.aircraftwar.android.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    private static final float WORLD_WIDTH = 512f;
    private static final float WORLD_HEIGHT = 768f;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint myPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint otherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long myUserId;
    private float myX = 256f;
    private float myY = 650f;
    private int myHp = 100;
    private long myScore = 0;
    private long myCoins = 0;

    private float enemyPlayerX = -1f;
    private float enemyPlayerY = -1f;
    private int enemyPlayerHp = 0;

    private final List<EnemyState> enemies = new ArrayList<>();
    private InputListener inputListener;

    public PvpBattleView(Context context) {
        this(context, null);
    }

    public PvpBattleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        bgPaint.setColor(0xFF0B1220);
        gridPaint.setColor(0x1FFFFFFF);
        gridPaint.setStrokeWidth(1.2f);

        myPaint.setColor(0xFF2ED1FF);
        otherPaint.setColor(0xFFFF8A65);
        enemyPaint.setColor(0xFFFFEB3B);

        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(32f);
    }

    public void setMyUserId(long myUserId) {
        this.myUserId = myUserId;
    }

    public void setInputListener(@Nullable InputListener listener) {
        this.inputListener = listener;
    }

    public void updateState(List<PlayerState> players, List<EnemyState> enemies) {
        this.enemies.clear();
        this.enemies.addAll(enemies);

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
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);

        for (int i = 0; i < 8; i++) {
            float y = i * canvas.getHeight() / 8f;
            canvas.drawLine(0, y, canvas.getWidth(), y, gridPaint);
        }

        for (EnemyState enemy : enemies) {
            float sx = worldToScreenX(enemy.x);
            float sy = worldToScreenY(enemy.y);
            float r = enemy.type == 2 ? 18f : 14f;
            canvas.drawCircle(sx, sy, r, enemyPaint);
        }

        canvas.drawCircle(worldToScreenX(myX), worldToScreenY(myY), 16f, myPaint);

        if (enemyPlayerX >= 0 && enemyPlayerY >= 0) {
            canvas.drawCircle(worldToScreenX(enemyPlayerX), worldToScreenY(enemyPlayerY), 16f, otherPaint);
        }

        canvas.drawText("HP:" + myHp + " SCORE:" + myScore + " COINS:" + myCoins, 16f, 40f, hudPaint);
        canvas.drawText("ENEMY_HP:" + Math.max(0, enemyPlayerHp), 16f, 78f, hudPaint);
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

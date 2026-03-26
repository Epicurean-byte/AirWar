package edu.hitsz.aircraftwar.android.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import edu.hitsz.game.core.engine.GameSnapshot;
import edu.hitsz.game.core.engine.RenderSprite;

public final class AndroidGameRenderer {
    private final BitmapSkinManager skinManager;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AndroidGameRenderer(BitmapSkinManager skinManager) {
        this.skinManager = skinManager;
        hudPaint.setColor(Color.RED);
        overlayPaint.setColor(0xAA000000);
        overlayTextPaint.setColor(Color.WHITE);
        overlayTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void render(Canvas canvas, GameSnapshot snapshot, GameViewport viewport) {
        drawBackground(canvas, snapshot);
        for (RenderSprite sprite : snapshot.getRenderSprites()) {
            int targetWidth = Math.max(1, Math.round(viewport.spriteWidthToScreen(sprite.getWidth())));
            int targetHeight = Math.max(1, Math.round(viewport.spriteHeightToScreen(sprite.getHeight())));
            Bitmap bitmap = skinManager.getBitmap(sprite.getSpriteId(), targetWidth, targetHeight);
            RectF destination = viewport.worldRectToScreen(
                    sprite.getX(),
                    sprite.getY(),
                    sprite.getWidth(),
                    sprite.getHeight()
            );
            canvas.drawBitmap(bitmap, null, destination, bitmapPaint);
        }
        drawHud(canvas, snapshot, viewport);
        if (snapshot.isGameOver()) {
            drawGameOver(canvas, viewport);
        }
    }

    private void drawBackground(Canvas canvas, GameSnapshot snapshot) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        Bitmap background = skinManager.getBackground(
                snapshot.getDifficulty(),
                canvasWidth,
                canvasHeight
        );
        float scrollOffset = snapshot.getBackgroundOffset()
                * (canvasHeight / (float) snapshot.getWorldHeight());
        float topA = scrollOffset - canvasHeight;
        float topB = scrollOffset;
        RectF destinationA = new RectF(
                0.0f,
                topA,
                canvasWidth,
                topA + canvasHeight
        );
        RectF destinationB = new RectF(
                0.0f,
                topB,
                canvasWidth,
                topB + canvasHeight
        );
        canvas.drawBitmap(background, null, destinationA, bitmapPaint);
        canvas.drawBitmap(background, null, destinationB, bitmapPaint);
    }

    private void drawHud(Canvas canvas, GameSnapshot snapshot, GameViewport viewport) {
        hudPaint.setTextSize(Math.max(22.0f, 22.0f * viewport.scale()));
        canvas.drawText(
                "SCORE:" + snapshot.getScore(),
                viewport.worldToScreenX(10.0f),
                viewport.worldToScreenY(25.0f),
                hudPaint
        );
        canvas.drawText(
                "LIFE:" + snapshot.getHeroHp(),
                viewport.worldToScreenX(10.0f),
                viewport.worldToScreenY(45.0f),
                hudPaint
        );
    }

    private void drawGameOver(Canvas canvas, GameViewport viewport) {
        canvas.drawRect(
                new RectF(
                        0.0f,
                        0.0f,
                        canvas.getWidth(),
                        canvas.getHeight()
                ),
                overlayPaint
        );
        overlayTextPaint.setTextSize(Math.max(40.0f, 56.0f * viewport.scale()));
        canvas.drawText(
                "GAME OVER",
                canvas.getWidth() / 2.0f,
                canvas.getHeight() / 2.0f,
                overlayTextPaint
        );
    }
}

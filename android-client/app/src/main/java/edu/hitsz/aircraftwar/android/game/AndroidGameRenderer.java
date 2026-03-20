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
            Bitmap bitmap = skinManager.getBitmap(sprite.getSpriteId(), sprite.getWidth(), sprite.getHeight());
            RectF destination = viewport.worldRectToScreen(
                    sprite.getX(),
                    sprite.getY(),
                    sprite.getWidth(),
                    sprite.getHeight()
            );
            canvas.drawBitmap(bitmap, null, destination, null);
        }
        drawHud(canvas, snapshot, viewport);
        if (snapshot.isGameOver()) {
            drawGameOver(canvas, snapshot, viewport);
        }
    }

    private void drawBackground(Canvas canvas, GameSnapshot snapshot) {
        Bitmap background = skinManager.getBackground(
                snapshot.getDifficulty(),
                snapshot.getWorldWidth(),
                snapshot.getWorldHeight()
        );
        float scale = Math.max(
                canvas.getWidth() / (float) snapshot.getWorldWidth(),
                canvas.getHeight() / (float) snapshot.getWorldHeight()
        );
        float scaledWidth = snapshot.getWorldWidth() * scale;
        float scaledHeight = snapshot.getWorldHeight() * scale;
        float offsetX = (canvas.getWidth() - scaledWidth) / 2.0f;
        float offsetY = (canvas.getHeight() - scaledHeight) / 2.0f;
        float topA = offsetY + (snapshot.getBackgroundOffset() - snapshot.getWorldHeight()) * scale;
        float topB = offsetY + snapshot.getBackgroundOffset() * scale;
        RectF destinationA = new RectF(
                offsetX,
                topA,
                offsetX + scaledWidth,
                topA + scaledHeight
        );
        RectF destinationB = new RectF(
                offsetX,
                topB,
                offsetX + scaledWidth,
                topB + scaledHeight
        );
        canvas.drawBitmap(background, null, destinationA, null);
        canvas.drawBitmap(background, null, destinationB, null);
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

    private void drawGameOver(Canvas canvas, GameSnapshot snapshot, GameViewport viewport) {
        canvas.drawRect(
                new RectF(
                        viewport.getOffsetX(),
                        viewport.getOffsetY(),
                        viewport.getOffsetX() + viewport.getScaledWidth(),
                        viewport.getOffsetY() + viewport.getScaledHeight()
                ),
                overlayPaint
        );
        overlayTextPaint.setTextSize(Math.max(40.0f, 56.0f * viewport.scale()));
        canvas.drawText(
                "GAME OVER",
                viewport.worldToScreenX(snapshot.getWorldWidth() / 2.0f),
                viewport.worldToScreenY(snapshot.getWorldHeight() / 2.0f),
                overlayTextPaint
        );
    }
}

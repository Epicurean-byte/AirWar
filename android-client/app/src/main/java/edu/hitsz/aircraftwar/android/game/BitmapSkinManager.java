package edu.hitsz.aircraftwar.android.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.util.EnumMap;

import edu.hitsz.aircraftwar.android.R;
import edu.hitsz.game.core.config.GameSessionConfig;
import edu.hitsz.game.core.config.Size;
import edu.hitsz.game.core.config.SpriteId;
import edu.hitsz.game.core.mode.Difficulty;

public final class BitmapSkinManager {
    public static final int DESKTOP_WORLD_WIDTH = 512;
    public static final int DESKTOP_WORLD_HEIGHT = 768;

    public enum SkinId {
        CLASSIC
    }

    private final Context appContext;
    private final EnumMap<SkinId, EnumMap<SpriteId, SpriteAsset>> skinRegistry = new EnumMap<>(SkinId.class);
    private final LruCache<String, Bitmap> bitmapCache = new LruCache<>(64);
    private final LruCache<Integer, Bitmap> sourceBitmapCache = new LruCache<>(32);
    private SkinId activeSkin = SkinId.CLASSIC;

    public BitmapSkinManager(Context context) {
        this.appContext = context.getApplicationContext();
        registerClassicSkin();
    }

    public void setActiveSkin(SkinId skinId) {
        this.activeSkin = skinId;
        bitmapCache.evictAll();
    }

    public GameSessionConfig createDesktopSessionConfig() {
        EnumMap<SpriteId, Size> spriteSizes = new EnumMap<>(SpriteId.class);
        EnumMap<SpriteId, SpriteAsset> spriteMap = skinRegistry.get(activeSkin);
        for (SpriteId spriteId : SpriteId.values()) {
            if (isBackground(spriteId)) {
                spriteSizes.put(spriteId, new Size(DESKTOP_WORLD_WIDTH, DESKTOP_WORLD_HEIGHT));
                continue;
            }
            SpriteAsset asset = spriteMap == null ? null : spriteMap.get(spriteId);
            if (asset == null) {
                throw new IllegalStateException("Missing sprite asset for " + spriteId);
            }
            spriteSizes.put(spriteId, new Size(asset.sourceWidth, asset.sourceHeight));
        }
        return new GameSessionConfig(DESKTOP_WORLD_WIDTH, DESKTOP_WORLD_HEIGHT, spriteSizes);
    }

    public Bitmap getBackground(Difficulty difficulty, int width, int height) {
        SpriteId spriteId = switch (difficulty) {
            case EASY -> SpriteId.BACKGROUND_EASY;
            case NORMAL -> SpriteId.BACKGROUND_NORMAL;
            case HARD -> SpriteId.BACKGROUND_HARD;
        };
        return getBitmap(spriteId, width, height);
    }

    public Bitmap getBitmap(SpriteId spriteId, int width, int height) {
        String cacheKey = activeSkin.name() + ":" + spriteId.name() + ":" + width + "x" + height;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        EnumMap<SpriteId, SpriteAsset> spriteMap = skinRegistry.get(activeSkin);
        SpriteAsset asset = spriteMap == null ? null : spriteMap.get(spriteId);
        Bitmap bitmap = createBitmap(asset, width, height);
        bitmapCache.put(cacheKey, bitmap);
        return bitmap;
    }

    private Bitmap createBitmap(SpriteAsset asset, int width, int height) {
        if (asset == null) {
            return Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        }
        Bitmap source = getSourceBitmap(asset.resId);
        int targetWidth = Math.max(1, width);
        int targetHeight = Math.max(1, height);
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source;
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }

    private void registerClassicSkin() {
        EnumMap<SpriteId, SpriteAsset> classic = new EnumMap<>(SpriteId.class);
        registerSprite(classic, SpriteId.BACKGROUND_EASY, R.drawable.pc_bg_easy);
        registerSprite(classic, SpriteId.BACKGROUND_NORMAL, R.drawable.pc_bg_normal);
        registerSprite(classic, SpriteId.BACKGROUND_HARD, R.drawable.pc_bg_hard);
        registerSprite(classic, SpriteId.HERO, R.drawable.pc_hero);
        registerSprite(classic, SpriteId.HERO_BULLET, R.drawable.pc_bullet_hero);
        registerSprite(classic, SpriteId.ENEMY_BULLET, R.drawable.pc_bullet_enemy);
        registerSprite(classic, SpriteId.MOB_ENEMY, R.drawable.pc_mob);
        registerSprite(classic, SpriteId.ELITE_ENEMY, R.drawable.pc_elite);
        registerSprite(classic, SpriteId.SUPER_ELITE_ENEMY, R.drawable.pc_elite_plus);
        registerSprite(classic, SpriteId.BOSS_ENEMY, R.drawable.pc_boss);
        registerSprite(classic, SpriteId.BLOOD_SUPPLY, R.drawable.pc_prop_blood);
        registerSprite(classic, SpriteId.BOMB_SUPPLY, R.drawable.pc_prop_bomb);
        registerSprite(classic, SpriteId.FIRE_SUPPLY, R.drawable.pc_prop_bullet);
        registerSprite(classic, SpriteId.SUPER_FIRE_SUPPLY, R.drawable.pc_prop_bullet_plus);
        skinRegistry.put(SkinId.CLASSIC, classic);
    }

    private void registerSprite(EnumMap<SpriteId, SpriteAsset> spriteMap, SpriteId spriteId, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(appContext.getResources(), resId, options);
        spriteMap.put(spriteId, new SpriteAsset(resId, options.outWidth, options.outHeight));
    }

    private Bitmap getSourceBitmap(int resId) {
        Bitmap cached = sourceBitmapCache.get(resId);
        if (cached != null) {
            return cached;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(), resId, options);
        if (bitmap == null) {
            throw new IllegalStateException("Failed to decode resource " + resId);
        }
        sourceBitmapCache.put(resId, bitmap);
        return bitmap;
    }

    private boolean isBackground(SpriteId spriteId) {
        return spriteId == SpriteId.BACKGROUND_EASY
                || spriteId == SpriteId.BACKGROUND_NORMAL
                || spriteId == SpriteId.BACKGROUND_HARD;
    }

    private static final class SpriteAsset {
        private final int resId;
        private final int sourceWidth;
        private final int sourceHeight;

        private SpriteAsset(int resId, int sourceWidth, int sourceHeight) {
            this.resId = resId;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
        }
    }
}

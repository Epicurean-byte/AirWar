package edu.hitsz.aircraftwar.android.ui;

import androidx.annotation.DrawableRes;

import java.util.HashMap;
import java.util.Map;

import edu.hitsz.aircraftwar.android.R;

public final class ShopItemVisuals {
    private static final Map<String, Integer> PREVIEW_MAP = buildPreviewMap();

    private ShopItemVisuals() {
    }

    @DrawableRes
    public static int resolvePreviewResId(String assetName) {
        Integer resId = PREVIEW_MAP.get(assetName);
        return resId == null ? R.drawable.pc_hero : resId;
    }

    @DrawableRes
    public static int resolveHeroDrawableResId(int equippedSkinId) {
        return switch (equippedSkinId) {
            case 1 -> R.drawable.shop_item_01;
            case 2 -> R.drawable.shop_item_02;
            case 3 -> R.drawable.shop_item_03;
            case 4 -> R.drawable.shop_item_04;
            case 5 -> R.drawable.shop_item_05;
            case 6 -> R.drawable.shop_item_06;
            case 7 -> R.drawable.shop_item_07;
            case 8 -> R.drawable.shop_item_08;
            default -> R.drawable.pc_hero;
        };
    }

    private static Map<String, Integer> buildPreviewMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("plane_default", R.drawable.pc_hero);
        map.put("item_01", R.drawable.shop_item_01);
        map.put("item_02", R.drawable.shop_item_02);
        map.put("item_03", R.drawable.shop_item_03);
        map.put("item_04", R.drawable.shop_item_04);
        map.put("item_05", R.drawable.shop_item_05);
        map.put("item_06", R.drawable.shop_item_06);
        map.put("item_07", R.drawable.shop_item_07);
        map.put("item_08", R.drawable.shop_item_08);
        map.put("item_09", R.drawable.shop_item_09);
        map.put("item_10", R.drawable.shop_item_10);
        map.put("item_11", R.drawable.shop_item_11);
        map.put("item_12", R.drawable.shop_item_12);
        map.put("item_13", R.drawable.shop_item_13);
        map.put("item_14", R.drawable.shop_item_14);
        map.put("item_15", R.drawable.shop_item_15);
        map.put("item_16", R.drawable.shop_item_16);
        return map;
    }
}

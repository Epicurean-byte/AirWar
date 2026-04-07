package edu.hitsz.aircraftwar.android.network.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopCatalog {
    private ShopCatalog() {
    }

    public static List<ShopSkin> fullCatalog() {
        List<ShopSkin> items = new ArrayList<>();
        items.add(new ShopSkin(0, "默认战机", "系统配发的经典战机外观", 0, "plane_skin", true, "plane_default", true));
        items.add(new ShopSkin(1, "苍穹游隼", "轻型高速蓝银机体，适合作为基础换装。", 280, "plane_skin", true, "item_01", false));
        items.add(new ShopSkin(2, "沙暴掠影", "沙黄色机身与深色座舱，偏侦察风格。", 360, "plane_skin", true, "item_02", false));
        items.add(new ShopSkin(3, "猩红矛隼", "机鼻修长，红白对比明显，攻击感更强。", 460, "plane_skin", true, "item_03", false));
        items.add(new ShopSkin(4, "夜幕渡鸦", "暗色系舰载外观，强调压迫感和隐蔽感。", 560, "plane_skin", true, "item_04", false));
        items.add(new ShopSkin(5, "雷霆裁决", "厚重机翼与黄黑警戒涂装，更偏重装风格。", 720, "plane_skin", true, "item_05", false));
        items.add(new ShopSkin(6, "极昼枪骑", "亮色机身与尖锐前翼，视觉最醒目。", 880, "plane_skin", true, "item_06", false));
        items.add(new ShopSkin(7, "霜原巡航者", "冷白与深蓝拼接，强调寒区作战感。", 1020, "plane_skin", true, "item_07", false));
        items.add(new ShopSkin(8, "王冠试作型", "收藏级主力机体，作为高阶终端皮肤。", 1280, "plane_skin", true, "item_08", false));
        items.add(new ShopSkin(9, "纪念徽章一号", "仅可在仓库中陈列的纪念藏品。", 180, "memorabilia", false, "item_09", false));
        items.add(new ShopSkin(10, "纪念徽章二号", "舰队列装纪念款展示图。", 220, "memorabilia", false, "item_10", false));
        items.add(new ShopSkin(11, "战役纪念章", "大型战役主题纪念藏品。", 260, "memorabilia", false, "item_11", false));
        items.add(new ShopSkin(12, "巡航纪念章", "巡航编队留存纪念图。", 300, "memorabilia", false, "item_12", false));
        items.add(new ShopSkin(13, "工业纪念章", "后勤生产线主题纪念图。", 340, "memorabilia", false, "item_13", false));
        items.add(new ShopSkin(14, "战区纪念章", "特殊战区识别系列纪念图。", 400, "memorabilia", false, "item_14", false));
        items.add(new ShopSkin(15, "军械纪念章", "重工军械主题藏品。", 460, "memorabilia", false, "item_15", false));
        items.add(new ShopSkin(16, "终章纪念章", "终局演习留档的收藏品。", 520, "memorabilia", false, "item_16", false));
        return items;
    }

    public static ShopSkin byId(int skinId) {
        for (ShopSkin item : fullCatalog()) {
            if (item.getSkinId() == skinId) {
                return item;
            }
        }
        return null;
    }

    public static ShopInfo merge(ShopInfo remote, LocalInventorySnapshot local) {
        Map<Integer, ShopSkin> remoteById = new LinkedHashMap<>();
        for (ShopSkin skin : remote.getSkins()) {
            remoteById.put(skin.getSkinId(), skin);
        }

        List<ShopSkin> merged = new ArrayList<>();
        for (ShopSkin base : fullCatalog()) {
            ShopSkin remoteItem = remoteById.get(base.getSkinId());
            boolean owned = local.isOwned(base.getSkinId()) || (remoteItem != null && remoteItem.isOwned());
            merged.add(new ShopSkin(
                    base.getSkinId(),
                    base.getName(),
                    base.getDescription(),
                    base.getPrice(),
                    base.getCategory(),
                    base.isEquippable(),
                    base.getAssetName(),
                    owned
            ));
        }
        return new ShopInfo(local.getCoins(), local.getEquippedSkinId(), merged);
    }
}

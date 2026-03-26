package edu.hitsz.aircraftwar.android.network.model;

import java.util.List;

public final class ShopInfo {
    private final long coins;
    private final int equippedSkinId;
    private final List<ShopSkin> skins;

    public ShopInfo(long coins, int equippedSkinId, List<ShopSkin> skins) {
        this.coins = coins;
        this.equippedSkinId = equippedSkinId;
        this.skins = List.copyOf(skins);
    }

    public long getCoins() {
        return coins;
    }

    public int getEquippedSkinId() {
        return equippedSkinId;
    }

    public List<ShopSkin> getSkins() {
        return skins;
    }
}

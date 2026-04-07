package edu.hitsz.aircraftwar.android.network.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class LocalInventorySnapshot {
    private final long coins;
    private final int equippedSkinId;
    private final Set<Integer> ownedIds;

    public LocalInventorySnapshot(long coins, int equippedSkinId, Set<Integer> ownedIds) {
        this.coins = coins;
        this.equippedSkinId = equippedSkinId;
        this.ownedIds = Collections.unmodifiableSet(new HashSet<>(ownedIds));
    }

    public long getCoins() {
        return coins;
    }

    public int getEquippedSkinId() {
        return equippedSkinId;
    }

    public boolean isOwned(int itemId) {
        return ownedIds.contains(itemId);
    }
}

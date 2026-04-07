package edu.hitsz.aircraftwar.android.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import edu.hitsz.aircraftwar.android.network.model.LocalInventorySnapshot;

public final class LocalInventoryManager {
    private static final String PREFS_NAME = "airwar_inventory";

    private final SharedPreferences prefs;

    public LocalInventoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void ensureInitialized(long userId, long coins, int equippedSkinId, Set<Integer> remoteOwned) {
        String ownedKey = ownedKey(userId);
        if (!prefs.contains(coinsKey(userId))) {
            prefs.edit().putLong(coinsKey(userId), coins).apply();
        }
        if (!prefs.contains(equippedKey(userId))) {
            prefs.edit().putInt(equippedKey(userId), equippedSkinId).apply();
        }
        Set<String> owned = prefs.getStringSet(ownedKey, null);
        if (owned == null) {
            Set<String> seed = new HashSet<>();
            seed.add("0");
            for (Integer id : remoteOwned) {
                seed.add(String.valueOf(id));
            }
            prefs.edit().putStringSet(ownedKey, seed).apply();
        } else {
            Set<String> merged = new HashSet<>(owned);
            merged.add("0");
            for (Integer id : remoteOwned) {
                merged.add(String.valueOf(id));
            }
            prefs.edit().putStringSet(ownedKey, merged).apply();
        }
    }

    public LocalInventorySnapshot snapshot(long userId) {
        long coins = prefs.getLong(coinsKey(userId), 0L);
        int equipped = prefs.getInt(equippedKey(userId), 0);
        Set<String> raw = prefs.getStringSet(ownedKey(userId), new HashSet<>());
        Set<Integer> owned = new HashSet<>();
        owned.add(0);
        for (String item : raw) {
            try {
                owned.add(Integer.parseInt(item));
            } catch (NumberFormatException ignored) {
            }
        }
        return new LocalInventorySnapshot(coins, equipped, owned);
    }

    public LocalInventorySnapshot purchase(long userId, int itemId, long price) {
        LocalInventorySnapshot current = snapshot(userId);
        Set<String> raw = new HashSet<>(prefs.getStringSet(ownedKey(userId), new HashSet<>()));
        raw.add(String.valueOf(itemId));
        long updatedCoins = Math.max(0L, current.getCoins() - price);
        prefs.edit()
                .putStringSet(ownedKey(userId), raw)
                .putLong(coinsKey(userId), updatedCoins)
                .apply();
        return snapshot(userId);
    }

    public LocalInventorySnapshot equip(long userId, int itemId) {
        prefs.edit().putInt(equippedKey(userId), itemId).apply();
        return snapshot(userId);
    }

    public void updateCoins(long userId, long coins) {
        prefs.edit().putLong(coinsKey(userId), coins).apply();
    }

    private static String coinsKey(long userId) {
        return "coins_" + userId;
    }

    private static String equippedKey(long userId) {
        return "equipped_" + userId;
    }

    private static String ownedKey(long userId) {
        return "owned_" + userId;
    }
}

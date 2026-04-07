package edu.hitsz.aircraftwar.android.network;

import android.content.Context;
import android.content.SharedPreferences;

import edu.hitsz.aircraftwar.android.network.model.UserProfile;

public final class SessionManager {
    private static final String PREFS_NAME = "airwar_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_COINS = "coins";
    private static final String KEY_EQUIPPED_SKIN_ID = "equipped_skin_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(UserProfile user) {
        prefs.edit()
                .putLong(KEY_USER_ID, user.getUserId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_NICKNAME, user.getNickname())
                .putLong(KEY_COINS, user.getCoins())
                .putInt(KEY_EQUIPPED_SKIN_ID, user.getEquippedSkinId())
                .apply();
    }

    public UserProfile loadUserOrNull() {
        long userId = prefs.getLong(KEY_USER_ID, 0L);
        if (userId <= 0L) {
            return null;
        }
        String username = prefs.getString(KEY_USERNAME, "");
        String nickname = prefs.getString(KEY_NICKNAME, "Player");
        long coins = prefs.getLong(KEY_COINS, 0L);
        int equippedSkinId = prefs.getInt(KEY_EQUIPPED_SKIN_ID, 0);
        return new UserProfile(userId, username, nickname, false, 0L, coins, equippedSkinId);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

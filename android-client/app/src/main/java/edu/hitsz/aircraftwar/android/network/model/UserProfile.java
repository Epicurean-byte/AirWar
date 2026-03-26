package edu.hitsz.aircraftwar.android.network.model;

public final class UserProfile {
    private final long userId;
    private final String username;
    private final String nickname;
    private final boolean online;
    private final long highScore;
    private final long coins;
    private final int equippedSkinId;

    public UserProfile(long userId,
                       String username,
                       String nickname,
                       boolean online,
                       long highScore,
                       long coins,
                       int equippedSkinId) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.online = online;
        this.highScore = highScore;
        this.coins = coins;
        this.equippedSkinId = equippedSkinId;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isOnline() {
        return online;
    }

    public long getHighScore() {
        return highScore;
    }

    public long getCoins() {
        return coins;
    }

    public int getEquippedSkinId() {
        return equippedSkinId;
    }
}

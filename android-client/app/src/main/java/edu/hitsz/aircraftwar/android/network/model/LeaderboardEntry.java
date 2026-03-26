package edu.hitsz.aircraftwar.android.network.model;

public final class LeaderboardEntry {
    private final long userId;
    private final String nickname;
    private final long value;
    private final int equippedSkinId;

    public LeaderboardEntry(long userId, String nickname, long value, int equippedSkinId) {
        this.userId = userId;
        this.nickname = nickname;
        this.value = value;
        this.equippedSkinId = equippedSkinId;
    }

    public long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public long getValue() {
        return value;
    }

    public int getEquippedSkinId() {
        return equippedSkinId;
    }
}

package com.planewar.server.model.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class User {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    /** 加载完文件后，将 ID 生成器推进到 max+1 */
    public static void initIdGen(long nextId) {
        ID_GEN.updateAndGet(cur -> Math.max(cur, nextId));
    }

    private long id;
    private String username;
    private String password;
    /** 昵称（可与 username 相同） */
    private String nickname;
    /** 是否在线 */
    private volatile boolean online;
    /** 历史最高单局分数 */
    private long highScore;
    /** 累计金币总数 */
    private long coins;
    /** 当前装备的皮肤 ID，默认 0 */
    private int equippedSkinId;
    /** 已拥有的皮肤 ID 集合 */
    private Set<Integer> ownedSkins = new HashSet<>();
    /** 好友 ID 列表 */
    private List<Long> friendIds = new ArrayList<>();

    public User() {
    }

    public User(String username, String password, String nickname) {
        this.id = ID_GEN.getAndIncrement();
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.ownedSkins.add(0); // 默认拥有皮肤 0
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getHighScore() {
        return highScore;
    }

    public void setHighScore(long highScore) {
        this.highScore = highScore;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public int getEquippedSkinId() {
        return equippedSkinId;
    }

    public void setEquippedSkinId(int equippedSkinId) {
        this.equippedSkinId = equippedSkinId;
    }

    public Set<Integer> getOwnedSkins() {
        return ownedSkins;
    }

    public void setOwnedSkins(Set<Integer> ownedSkins) {
        this.ownedSkins = ownedSkins;
    }

    public List<Long> getFriendIds() {
        return friendIds;
    }

    public void setFriendIds(List<Long> friendIds) {
        this.friendIds = friendIds;
    }
}

package com.planewar.server.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Data
@NoArgsConstructor
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

    public User(String username, String password, String nickname) {
        this.id = ID_GEN.getAndIncrement();
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.ownedSkins.add(0); // 默认拥有皮肤 0
    }
}

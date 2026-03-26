package com.planewar.server.datastore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.planewar.server.model.entity.FriendRequest;
import com.planewar.server.model.entity.Room;
import com.planewar.server.model.entity.SkinConfig;
import com.planewar.server.model.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存数据库 + JSON 文件持久化。
 *
 * 数据目录由 app.data-dir 配置（默认 ./data），包含：
 *   users.json          - 用户数据
 *   friend_requests.json - 好友申请数据
 *
 * 房间（Room）为会话级数据，不持久化。
 */
@Component
public class InMemoryDataStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDataStore.class);

    @Value("${app.data-dir:./data}")
    private String dataDir;

    private final ObjectMapper mapper;

    // -------- 用户 --------
    public final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Long> usernameIndex = new ConcurrentHashMap<>();

    // -------- 好友申请 --------
    public final ConcurrentHashMap<Long, FriendRequest> friendRequests = new ConcurrentHashMap<>();

    // -------- 房间（会话级，不落盘） --------
    public final ConcurrentHashMap<Long, Room> rooms = new ConcurrentHashMap<>();
    public final LinkedList<Long> matchPool = new LinkedList<>();

    // -------- 商城皮肤配置（静态，不落盘） --------
    public final List<SkinConfig> skinCatalog;

    public InMemoryDataStore(ObjectMapper objectMapper) {
        this.mapper = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT);

        skinCatalog = new ArrayList<>();
        skinCatalog.add(new SkinConfig(0, "默认战机", "经典蓝色战机", 0, "plane_default"));
        skinCatalog.add(new SkinConfig(1, "赤焰战机", "烈焰红色涂装", 500, "plane_red"));
        skinCatalog.add(new SkinConfig(2, "幽灵战机", "隐身黑色涂装", 800, "plane_ghost"));
        skinCatalog.add(new SkinConfig(3, "极光战机", "极光彩虹涂装", 1200, "plane_aurora"));
    }

    // -------- 启动加载 --------

    @PostConstruct
    public void loadAll() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("数据目录已创建：{}", dir.getAbsolutePath());
        }
        loadUsers();
        loadFriendRequests();
        log.info("数据加载完成：{} 个用户，{} 条好友申请", users.size(), friendRequests.size());
    }

    private void loadUsers() {
        File file = usersFile();
        if (!file.exists()) return;
        try {
            List<User> list = mapper.readValue(file, new TypeReference<>() {});
            long maxId = 0;
            for (User u : list) {
                u.setOnline(false); // 服务重启后全部离线
                users.put(u.getId(), u);
                usernameIndex.put(u.getUsername(), u.getId());
                if (u.getId() > maxId) maxId = u.getId();
            }
            User.initIdGen(maxId + 1);
            log.info("加载 users.json：{} 条", list.size());
        } catch (IOException e) {
            log.error("加载 users.json 失败：{}", e.getMessage());
        }
    }

    private void loadFriendRequests() {
        File file = friendRequestsFile();
        if (!file.exists()) return;
        try {
            List<FriendRequest> list = mapper.readValue(file, new TypeReference<>() {});
            long maxId = 0;
            for (FriendRequest r : list) {
                friendRequests.put(r.getId(), r);
                if (r.getId() > maxId) maxId = r.getId();
            }
            FriendRequest.initIdGen(maxId + 1);
            log.info("加载 friend_requests.json：{} 条", list.size());
        } catch (IOException e) {
            log.error("加载 friend_requests.json 失败：{}", e.getMessage());
        }
    }

    // -------- 落盘方法（写操作后调用） --------

    public synchronized void flushUsers() {
        try {
            mapper.writeValue(usersFile(), new ArrayList<>(users.values()));
        } catch (IOException e) {
            log.error("写入 users.json 失败：{}", e.getMessage());
        }
    }

    public synchronized void flushFriendRequests() {
        try {
            mapper.writeValue(friendRequestsFile(), new ArrayList<>(friendRequests.values()));
        } catch (IOException e) {
            log.error("写入 friend_requests.json 失败：{}", e.getMessage());
        }
    }

    // -------- 便捷查询 --------

    public Optional<User> findByUsername(String username) {
        Long uid = usernameIndex.get(username);
        return uid == null ? Optional.empty() : Optional.ofNullable(users.get(uid));
    }

    public Optional<User> findById(long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    /** 将用户存入内存（不自动落盘，调用方需自行调用 flushUsers()） */
    public void saveUser(User user) {
        users.put(user.getId(), user);
        usernameIndex.put(user.getUsername(), user.getId());
    }

    public Optional<SkinConfig> findSkinById(int skinId) {
        return skinCatalog.stream().filter(s -> s.getSkinId() == skinId).findFirst();
    }

    // -------- 私有工具 --------

    private File usersFile() {
        return new File(dataDir, "users.json");
    }

    private File friendRequestsFile() {
        return new File(dataDir, "friend_requests.json");
    }
}

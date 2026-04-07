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
        skinCatalog.add(new SkinConfig(0, "默认战机", "系统配发的经典战机外观", 0, "plane_skin", true, "plane_default"));
        skinCatalog.add(new SkinConfig(1, "苍穹游隼", "轻型高速蓝银机体，适合作为基础换装。", 280, "plane_skin", true, "item_01"));
        skinCatalog.add(new SkinConfig(2, "沙暴掠影", "沙黄色机身与深色座舱，偏侦察风格。", 360, "plane_skin", true, "item_02"));
        skinCatalog.add(new SkinConfig(3, "猩红矛隼", "机鼻修长，红白对比明显，攻击感更强。", 460, "plane_skin", true, "item_03"));
        skinCatalog.add(new SkinConfig(4, "夜幕渡鸦", "暗色系舰载外观，强调压迫感和隐蔽感。", 560, "plane_skin", true, "item_04"));
        skinCatalog.add(new SkinConfig(5, "雷霆裁决", "厚重机翼与黄黑警戒涂装，更偏重装风格。", 720, "plane_skin", true, "item_05"));
        skinCatalog.add(new SkinConfig(6, "极昼枪骑", "亮色机身与尖锐前翼，视觉最醒目。", 880, "plane_skin", true, "item_06"));
        skinCatalog.add(new SkinConfig(7, "霜原巡航者", "冷白与深蓝拼接，强调寒区作战感。", 1020, "plane_skin", true, "item_07"));
        skinCatalog.add(new SkinConfig(8, "王冠试作型", "收藏级主力机体，作为高阶终端皮肤。", 1280, "plane_skin", true, "item_08"));
        skinCatalog.add(new SkinConfig(9, "纪念徽章一号", "仅可在仓库中陈列的纪念藏品。", 180, "memorabilia", false, "item_09"));
        skinCatalog.add(new SkinConfig(10, "纪念徽章二号", "舰队列装纪念款展示图。", 220, "memorabilia", false, "item_10"));
        skinCatalog.add(new SkinConfig(11, "战役纪念章", "大型战役主题纪念藏品。", 260, "memorabilia", false, "item_11"));
        skinCatalog.add(new SkinConfig(12, "巡航纪念章", "巡航编队留存纪念图。", 300, "memorabilia", false, "item_12"));
        skinCatalog.add(new SkinConfig(13, "工业纪念章", "后勤生产线主题纪念图。", 340, "memorabilia", false, "item_13"));
        skinCatalog.add(new SkinConfig(14, "战区纪念章", "特殊战区识别系列纪念图。", 400, "memorabilia", false, "item_14"));
        skinCatalog.add(new SkinConfig(15, "军械纪念章", "重工军械主题藏品。", 460, "memorabilia", false, "item_15"));
        skinCatalog.add(new SkinConfig(16, "终章纪念章", "终局演习留档的收藏品。", 520, "memorabilia", false, "item_16"));
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
                if (u.getOwnedSkins() == null) {
                    u.setOwnedSkins(new HashSet<>());
                }
                u.getOwnedSkins().add(0);
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

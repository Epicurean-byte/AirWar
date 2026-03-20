package com.planewar.server.controller;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.ApiResponse;
import com.planewar.server.model.entity.FriendRequest;
import com.planewar.server.model.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户相关 HTTP 接口：注册、登录、搜索玩家、好友申请管理。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final InMemoryDataStore store;

    public UserController(InMemoryDataStore store) {
        this.store = store;
    }

    // -------- 注册 --------
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.fail("用户名或密码不能为空");
        }
        if (store.usernameIndex.containsKey(username)) {
            return ApiResponse.fail("用户名已存在");
        }
        User user = new User(username, password, nickname);
        store.saveUser(user);
        store.flushUsers();
        return ApiResponse.ok(userInfo(user));
    }

    // -------- 登录 --------
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        return store.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .map(u -> {
                    u.setOnline(true);
                    return ApiResponse.ok(userInfo(u));
                })
                .orElse(ApiResponse.fail("用户名或密码错误"));
    }

    // -------- 登出 --------
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestParam long userId) {
        store.findById(userId).ifPresent(u -> u.setOnline(false));
        return ApiResponse.ok();
    }

    // -------- 搜索玩家 --------
    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestParam String keyword) {
        List<Map<String, Object>> result = store.users.values().stream()
                .filter(u -> u.getUsername().contains(keyword) || u.getNickname().contains(keyword))
                .map(this::userInfo)
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    // -------- 获取好友列表 --------
    @GetMapping("/friends")
    public ApiResponse<List<Map<String, Object>>> friends(@RequestParam long userId) {
        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        List<Map<String, Object>> list = opt.get().getFriendIds().stream()
                .map(store::findById)
                .filter(Optional::isPresent)
                .map(o -> userInfo(o.get()))
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    // -------- 发送好友申请 --------
    @PostMapping("/friend/request")
    public ApiResponse<Void> sendFriendRequest(@RequestBody Map<String, Long> body) {
        long fromId = body.get("fromUserId");
        long toId = body.get("toUserId");

        if (store.findById(fromId).isEmpty() || store.findById(toId).isEmpty()) {
            return ApiResponse.fail("用户不存在");
        }
        User from = store.users.get(fromId);
        if (from.getFriendIds().contains(toId)) {
            return ApiResponse.fail("已经是好友了");
        }
        // 检查是否已有 PENDING 申请
        boolean exists = store.friendRequests.values().stream()
                .anyMatch(r -> r.getFromUserId() == fromId && r.getToUserId() == toId
                        && r.getStatus() == FriendRequest.Status.PENDING);
        if (exists) return ApiResponse.fail("已发送过申请，等待对方处理");

        FriendRequest req = new FriendRequest(fromId, toId);
        store.friendRequests.put(req.getId(), req);
        store.flushFriendRequests();
        return ApiResponse.ok();
    }

    // -------- 获取收到的好友申请 --------
    @GetMapping("/friend/requests")
    public ApiResponse<List<Map<String, Object>>> getFriendRequests(@RequestParam long userId) {
        List<Map<String, Object>> list = store.friendRequests.values().stream()
                .filter(r -> r.getToUserId() == userId && r.getStatus() == FriendRequest.Status.PENDING)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("requestId", r.getId());
                    m.put("fromUserId", r.getFromUserId());
                    store.findById(r.getFromUserId()).ifPresent(u -> {
                        m.put("fromNickname", u.getNickname());
                        m.put("fromUsername", u.getUsername());
                    });
                    return m;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    // -------- 处理好友申请（接受/拒绝） --------
    @PostMapping("/friend/respond")
    public ApiResponse<Void> respondFriendRequest(@RequestBody Map<String, Object> body) {
        long requestId = ((Number) body.get("requestId")).longValue();
        boolean accept = (boolean) body.get("accept");

        FriendRequest req = store.friendRequests.get(requestId);
        if (req == null || req.getStatus() != FriendRequest.Status.PENDING) {
            return ApiResponse.fail("申请不存在或已处理");
        }
        if (accept) {
            req.setStatus(FriendRequest.Status.ACCEPTED);
            store.findById(req.getFromUserId()).ifPresent(u -> u.getFriendIds().add(req.getToUserId()));
            store.findById(req.getToUserId()).ifPresent(u -> u.getFriendIds().add(req.getFromUserId()));
            store.flushUsers();
        } else {
            req.setStatus(FriendRequest.Status.REJECTED);
        }
        store.flushFriendRequests();
        return ApiResponse.ok();
    }

    // -------- 获取用户信息 --------
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info(@RequestParam long userId) {
        return store.findById(userId)
                .map(u -> ApiResponse.ok(userInfo(u)))
                .orElse(ApiResponse.fail("用户不存在"));
    }

    // -------- 内部工具：将 User 转为展示 Map --------
    private Map<String, Object> userInfo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("online", u.isOnline());
        m.put("highScore", u.getHighScore());
        m.put("coins", u.getCoins());
        m.put("equippedSkinId", u.getEquippedSkinId());
        return m;
    }
}

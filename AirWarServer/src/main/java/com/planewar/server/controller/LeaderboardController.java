package com.planewar.server.controller;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.ApiResponse;
import com.planewar.server.model.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排行榜 HTTP 接口：分数榜、金币榜。
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private static final int TOP_N = 50;
    private static final String BOARD_SCORE = "score";
    private static final String BOARD_COINS = "coins";
    private static final String BOARD_ALL = "all";

    private final InMemoryDataStore store;

    public LeaderboardController(InMemoryDataStore store) {
        this.store = store;
    }

    /** 分数排行榜（按历史最高分降序） */
    @GetMapping("/score")
    public ApiResponse<List<Map<String, Object>>> scoreBoard() {
        List<Map<String, Object>> list = store.users.values().stream()
                .filter(u -> u.getHighScore() > 0L)
                .sorted(Comparator.comparingLong(User::getHighScore).reversed())
                .limit(TOP_N)
                .map(u -> entry(u, u.getHighScore()))
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    /** 金币排行榜（按累计金币总数降序） */
    @GetMapping("/coins")
    public ApiResponse<List<Map<String, Object>>> coinsBoard() {
        List<Map<String, Object>> list = store.users.values().stream()
                .filter(u -> u.getCoins() > 0L)
                .sorted(Comparator.comparingLong(User::getCoins).reversed())
                .limit(TOP_N)
                .map(u -> entry(u, u.getCoins()))
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    /**
     * 删除指定用户在排行榜上的数据。
     * <p>
     * 当前排行榜没有独立记录表，而是由 User.highScore / User.coins 实时计算：
     * score -> 清零 highScore；coins -> 清零 coins；all -> 两者都清零。
     */
    @PostMapping("/delete")
    public ApiResponse<Map<String, Object>> deleteEntry(@RequestBody Map<String, Object> body) {
        long targetUserId = numberValue(body.get("targetUserId"));
        String boardType = String.valueOf(body.getOrDefault("boardType", BOARD_ALL)).toLowerCase(Locale.ROOT);
        if (targetUserId <= 0L) {
            return ApiResponse.fail("targetUserId 无效");
        }
        if (!BOARD_SCORE.equals(boardType) && !BOARD_COINS.equals(boardType) && !BOARD_ALL.equals(boardType)) {
            return ApiResponse.fail("boardType 必须是 score、coins 或 all");
        }

        Optional<User> opt = store.findById(targetUserId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        synchronized (user) {
            if (BOARD_SCORE.equals(boardType) || BOARD_ALL.equals(boardType)) {
                user.setHighScore(0L);
            }
            if (BOARD_COINS.equals(boardType) || BOARD_ALL.equals(boardType)) {
                user.setCoins(0L);
            }
        }
        store.flushUsers();

        Map<String, Object> resp = new HashMap<>();
        resp.put("targetUserId", user.getId());
        resp.put("boardType", boardType);
        resp.put("highScore", user.getHighScore());
        resp.put("coins", user.getCoins());
        return ApiResponse.ok(resp);
    }

    private Map<String, Object> entry(User u, long value) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getId());
        m.put("nickname", u.getNickname());
        m.put("value", value);
        m.put("equippedSkinId", u.getEquippedSkinId());
        return m;
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }
}

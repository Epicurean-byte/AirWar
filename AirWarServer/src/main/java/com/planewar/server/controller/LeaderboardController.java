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

    private final InMemoryDataStore store;

    public LeaderboardController(InMemoryDataStore store) {
        this.store = store;
    }

    /** 分数排行榜（按历史最高分降序） */
    @GetMapping("/score")
    public ApiResponse<List<Map<String, Object>>> scoreBoard() {
        List<Map<String, Object>> list = store.users.values().stream()
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
                .sorted(Comparator.comparingLong(User::getCoins).reversed())
                .limit(TOP_N)
                .map(u -> entry(u, u.getCoins()))
                .collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

    private Map<String, Object> entry(User u, long value) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getId());
        m.put("nickname", u.getNickname());
        m.put("value", value);
        m.put("equippedSkinId", u.getEquippedSkinId());
        return m;
    }
}

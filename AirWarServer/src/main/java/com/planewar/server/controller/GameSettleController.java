package com.planewar.server.controller;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.ApiResponse;
import com.planewar.server.model.entity.Room;
import com.planewar.server.model.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 联机对战结束后的结算上报接口。
 * <p>
 * 胜负判定公式：最终成绩 = 10% * 分数 + 90% * 金币
 * 赢家获得额外金币奖励，本局成绩同步更新至全局排行榜。
 */
@RestController
@RequestMapping("/api/game")
public class GameSettleController {

    private static final long WINNER_BONUS = 200L;

    private final InMemoryDataStore store;

    public GameSettleController(InMemoryDataStore store) {
        this.store = store;
    }

    /**
     * 单人模式结算（上报得分与金币）。
     */
    @PostMapping("/settle/single")
    public ApiResponse<Map<String, Object>> settleSingle(@RequestBody Map<String, Object> body) {
        long userId = ((Number) body.get("userId")).longValue();
        long score = ((Number) body.get("score")).longValue();
        long coinsEarned = ((Number) body.get("coins")).longValue();

        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        synchronized (user) {
            if (score > user.getHighScore()) user.setHighScore(score);
            user.setCoins(user.getCoins() + coinsEarned);
        }

        store.flushUsers();
        Map<String, Object> resp = new HashMap<>();
        resp.put("highScore", user.getHighScore());
        resp.put("coins", user.getCoins());
        return ApiResponse.ok(resp);
    }

    /**
     * 联机对战结算（双方同时上报，服务端判定胜负并派发奖励）。
     */
    @PostMapping("/settle/pvp")
    public ApiResponse<Map<String, Object>> settlePvp(@RequestBody Map<String, Object> body) {
        long roomId = ((Number) body.get("roomId")).longValue();
        long userId = ((Number) body.get("userId")).longValue();

        Room room = store.rooms.get(roomId);
        if (room == null) return ApiResponse.fail("房间不存在");
        if (!room.containsPlayer(userId)) return ApiResponse.fail("用户不属于该房间");
        if (room.getState() != Room.State.FINISHED) return ApiResponse.fail("房间尚未结束，不能结算");

        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        Room.PlayerBattleResult result = room.getBattleResults().get(userId);
        if (result == null) return ApiResponse.fail("未找到该玩家的服务端战斗结果");

        long winnerUserId = 0;
        double bestRating = Double.NEGATIVE_INFINITY;
        for (Room.PlayerBattleResult candidate : room.getBattleResults().values()) {
            if (candidate.getRating() > bestRating) {
                bestRating = candidate.getRating();
                winnerUserId = candidate.getUserId();
            }
        }

        long bonus = winnerUserId == userId ? WINNER_BONUS : 0L;
        boolean alreadySettled;
        synchronized (room) {
            alreadySettled = !room.getSettledUserIds().add(userId);
            if (!alreadySettled) {
                synchronized (user) {
                    if (result.getScore() > user.getHighScore()) user.setHighScore(result.getScore());
                    user.setCoins(user.getCoins() + result.getCoins() + bonus);
                }
                store.flushUsers();
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("alreadySettled", alreadySettled);
        resp.put("score", result.getScore());
        resp.put("earnedCoins", result.getCoins());
        resp.put("winnerBonus", alreadySettled ? 0L : bonus);
        resp.put("myRating", result.getRating());
        resp.put("winnerUserId", winnerUserId);
        resp.put("highScore", user.getHighScore());
        resp.put("coins", user.getCoins());
        return ApiResponse.ok(resp);
    }
}

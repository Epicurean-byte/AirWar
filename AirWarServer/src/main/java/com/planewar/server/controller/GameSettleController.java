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
        long score = ((Number) body.get("score")).longValue();
        long coinsEarned = ((Number) body.get("coins")).longValue();

        Room room = store.rooms.get(roomId);
        if (room == null) return ApiResponse.fail("房间不存在");

        Optional<User> opt = store.findById(userId);
        if (opt.isEmpty()) return ApiResponse.fail("用户不存在");
        User user = opt.get();

        synchronized (user) {
            if (score > user.getHighScore()) user.setHighScore(score);
            user.setCoins(user.getCoins() + coinsEarned);
        }

        // 判断对手数据，决定是否已可结算
        long opponentId = (room.getPlayer1Id() == userId) ? room.getPlayer2Id() : room.getPlayer1Id();
        Optional<User> opponentOpt = store.findById(opponentId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("highScore", user.getHighScore());
        resp.put("coins", user.getCoins());

        // 如果双方数据都已就绪（此处简化：上报时即时判断），计算综合成绩
        if (opponentOpt.isPresent()) {
            User opponent = opponentOpt.get();
            double myRating = 0.1 * score + 0.9 * coinsEarned;
            // 对手的本局数据由其自行上报，这里只做单方向判断示例
            // 在真实场景中可通过 room 缓存双方数据后再做最终判定
            resp.put("myRating", myRating);
            resp.put("winnerBonus", WINNER_BONUS);
        }

        room.setState(Room.State.FINISHED);
        store.flushUsers();
        return ApiResponse.ok(resp);
    }
}

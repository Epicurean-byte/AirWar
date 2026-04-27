package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.entity.Room;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class BattleService {
    private static final int TICK_MS = 50;

    private final InMemoryDataStore store;
    private final MatchManager matchManager;
    private final WsMessageSender sender;
    private final BattleEngine engine;
    private final ConcurrentHashMap<Long, BattleRoomState> battles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "battle-loop");
        thread.setDaemon(true);
        return thread;
    });

    public BattleService(InMemoryDataStore store,
                         MatchManager matchManager,
                         WsMessageSender sender,
                         BattleEngine engine) {
        this.store = store;
        this.matchManager = matchManager;
        this.sender = sender;
        this.engine = engine;
        scheduler.scheduleAtFixedRate(this::tickBattles, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void startBattle(Room room) {
        battles.put(room.getId(), engine.create(room));
        sender.broadcastToRoom(room.getId(), WsMessageFactory.gameStart(room));
    }

    public void move(long roomId, long userId, JSONObject payload) {
        BattleRoomState battle = battles.get(roomId);
        if (battle == null || battle.finished) {
            return;
        }
        synchronized (battle) {
            if (!battle.finished) {
                engine.move(battle, userId, payload);
            }
        }
    }

    public void fire(long roomId, long userId) {
        BattleRoomState battle = battles.get(roomId);
        if (battle == null || battle.finished) {
            return;
        }
        synchronized (battle) {
            if (!battle.finished) {
                engine.fire(battle, userId);
            }
        }
    }

    public void finishBattle(long roomId, String reason, long sourceUserId) {
        BattleRoomState battle = battles.get(roomId);
        if (battle == null) {
            return;
        }

        String payload;
        synchronized (battle) {
            if (battle.finished) {
                return;
            }
            battle.finished = true;
            payload = engine.buildGameOverPayload(battle, store.rooms.get(roomId), reason, sourceUserId);
            battles.remove(roomId);
        }

        sender.broadcastToRoom(roomId, WsMessageFactory.gameOver(roomId, payload));
        matchManager.finishRoom(roomId);
    }

    private void tickBattles() {
        for (BattleRoomState battle : battles.values()) {
            if (battle.finished) {
                continue;
            }

            BattleEngine.TickResult result;
            synchronized (battle) {
                if (battle.finished) {
                    continue;
                }
                result = engine.tick(battle);
            }

            sender.broadcastToRoom(battle.roomId, WsMessageFactory.battleState(battle.roomId, result.getStatePayload()));
            if (result.shouldFinish()) {
                finishBattle(battle.roomId, "BATTLE_FINISHED", 0L);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

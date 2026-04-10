package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.WsMessageDto;
import com.planewar.server.model.entity.GameMode;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private static final int WORLD_WIDTH = 512;
    private static final int WORLD_HEIGHT = 768;
    private static final int TICK_MS = 50;
    private static final int MAX_ENEMIES = 12;

    private final MatchManager matchManager;
    private final InMemoryDataStore store;

    private final ConcurrentHashMap<Long, BattleRoomState> battles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService battleScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "battle-loop");
        t.setDaemon(true);
        return t;
    });

    public GameWebSocketHandler(MatchManager matchManager, InMemoryDataStore store) {
        this.matchManager = matchManager;
        this.store = store;
        battleScheduler.scheduleAtFixedRate(this::tickBattles, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessageDto dto;
        try {
            dto = JSON.parseObject(message.getPayload(), WsMessageDto.class);
        } catch (Exception e) {
            sendError(session, "invalid message json");
            return;
        }

        if (dto.getType() == null) {
            sendError(session, "missing type");
            return;
        }

        switch (dto.getType()) {
            case "AUTH" -> handleAuth(session, dto);
            case "MATCH_RANDOM" -> handleMatchRandom(session);
            case "CREATE_ROOM" -> handleCreateRoom(session, dto);
            case "JOIN_ROOM" -> handleJoinRoom(session, dto.getRoomId(), dto);
            case "START_GAME" -> handleStartGame(session, dto.getRoomId());
            case "MOVE" -> handleMove(session, dto);
            case "FIRE" -> handleFire(session);
            case "PICKUP" -> {
                // Reserved for compatibility in server-authoritative mode.
            }
            case "GAME_OVER" -> handleGameOver(session);
            default -> sendError(session, "unknown type: " + dto.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId != 0L) {
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                finishBattle(roomId, "PLAYER_DISCONNECTED", userId);
            }
            store.findById(userId).ifPresent(u -> u.setOnline(false));
        }
        matchManager.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: {}", exception.getMessage());
    }

    private void handleAuth(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = dto.getUserId();
        if (store.findById(userId).isEmpty()) {
            sendError(session, "user not found");
            return;
        }
        matchManager.register(session, userId);
        store.findById(userId).ifPresent(u -> u.setOnline(true));
        send(session, buildMsg("AUTH_OK", 0, 0, "{\"userId\":" + userId + "}"));
    }

    private void handleMatchRandom(WebSocketSession session) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;

        Optional<Room> roomOpt = matchManager.randomMatch(userId);
        if (roomOpt.isEmpty()) {
            send(session, buildMsg("MATCH_WAITING", 0, 0, "{}"));
            return;
        }

        Room room = roomOpt.get();
        String payload = roomPayload(room);
        broadcastToRoomAll(room.getId(), buildMsg("MATCH_SUCCESS", room.getId(), 0, payload));
    }

    private void handleCreateRoom(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;

        Room room = matchManager.createRoom(userId);
        
        // 从消息中获取游戏模式和皮肤ID
        JSONObject payload = safePayload(dto.getPayload());
        String modeStr = payload.getString("gameMode");
        if (modeStr != null && !modeStr.isBlank()) {
            try {
                room.setGameMode(GameMode.valueOf(modeStr));
            } catch (IllegalArgumentException e) {
                room.setGameMode(GameMode.COOP); // 默认合作模式
            }
        }
        room.setPlayer1SkinId(payload.getIntValue("skinId", 0));
        
        send(session, buildMsg("ROOM_CREATED", room.getId(), 0,
                "{\"roomId\":" + room.getId() + ",\"hostId\":" + userId + 
                ",\"gameMode\":\"" + room.getGameMode() + "\"}"));
    }

    private void handleJoinRoom(WebSocketSession session, long roomId, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;

        Optional<Room> roomOpt = matchManager.joinRoom(roomId, userId);
        if (roomOpt.isEmpty()) {
            sendError(session, "room not found or full");
            return;
        }

        Room room = roomOpt.get();
        
        // 保存玩家2的皮肤ID
        JSONObject payload = safePayload(dto.getPayload());
        room.setPlayer2SkinId(payload.getIntValue("skinId", 0));
        
        String payloadStr = roomPayload(room);
        broadcastToRoomAll(room.getId(), buildMsg("ROOM_JOINED", room.getId(), 0, payloadStr));
    }

    private void handleStartGame(WebSocketSession session, long roomId) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;

        Optional<Room> roomOpt = matchManager.startGame(roomId, userId);
        if (roomOpt.isEmpty()) {
            sendError(session, "cannot start game");
            return;
        }

        Room room = roomOpt.get();
        BattleRoomState battle = new BattleRoomState(room);
        battles.put(room.getId(), battle);

        String initPayload = String.format(
                "{\"roomId\":%d,\"seed\":%d,\"player1Id\":%d,\"player2Id\":%d," +
                        "\"gameMode\":\"%s\",\"player1SkinId\":%d,\"player2SkinId\":%d," +
                        "\"difficulty\":{\"enemyHpMultiplier\":1.5,\"spawnRate\":1.3,\"bulletDensity\":1.2}}",
                room.getId(), room.getGameSeed(), room.getPlayer1Id(), room.getPlayer2Id(),
                room.getGameMode(), room.getPlayer1SkinId(), room.getPlayer2SkinId());
        broadcastToRoomAll(room.getId(), buildMsg("GAME_START", room.getId(), 0, initPayload));
    }

    private void handleMove(WebSocketSession session, WsMessageDto dto) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0L) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;

        BattleRoomState battle = battles.get(roomId);
        if (battle == null || battle.finished) return;

        PlayerState player = battle.players.get(userId);
        if (player == null) return;

        JSONObject payload = safePayload(dto.getPayload());
        float x = clamp((float) payload.getDoubleValue("x"), 0, WORLD_WIDTH);
        float y = (float) payload.getDoubleValue("y");
        
        // 根据游戏模式限制玩家移动区域
        if (battle.gameMode == GameMode.PVP) {
            // PVP模式：玩家1在上半场，玩家2在下半场
            if (userId == battle.player1Id) {
                y = clamp(y, 0, WORLD_HEIGHT / 2 - 20); // 上半场
            } else {
                y = clamp(y, WORLD_HEIGHT / 2 + 20, WORLD_HEIGHT); // 下半场
            }
        } else {
            // COOP模式：玩家可以在整个屏幕移动
            y = clamp(y, 0, WORLD_HEIGHT);
        }
        
        player.x = x;
        player.y = y;
    }

    private void handleFire(WebSocketSession session) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0L) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;

        BattleRoomState battle = battles.get(roomId);
        if (battle == null || battle.finished) return;

        synchronized (battle) {
            PlayerState player = battle.players.get(userId);
            if (player == null || player.hp <= 0) return;

            // 创建子弹
            int bulletId = battle.nextBulletId++;
            float bulletX = player.x;
            float bulletY = player.y - 30; // 从飞机前方发射
            float bulletSpeed = -10f; // 向上飞行（y减小）
            
            battle.bullets.put(bulletId, new BulletState(bulletId, userId, bulletX, bulletY, bulletSpeed));
            log.debug("User {} fired bullet {} at ({}, {}) in room {}", userId, bulletId, bulletX, bulletY, roomId);
        }
    }

    private void handleGameOver(WebSocketSession session) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0L) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;
        finishBattle(roomId, "PLAYER_QUIT", userId);
    }

    private void tickBattles() {
        for (BattleRoomState battle : battles.values()) {
            if (battle.finished) continue;
            synchronized (battle) {
                battle.tickCount++;

                if (battle.tickCount % 16 == 0 && battle.enemies.size() < MAX_ENEMIES) {
                    spawnEnemy(battle);
                }

                Iterator<EnemyState> it = battle.enemies.values().iterator();
                while (it.hasNext()) {
                    EnemyState enemy = it.next();
                    enemy.y += enemy.speedY;
                    if (enemy.y > WORLD_HEIGHT + 40) {
                        it.remove();
                        continue;
                    }

                    // 碰撞检测：根据飞机和敌机的实际大小调整碰撞半径
                    // 玩家飞机约76像素，敌机约52-88像素，使用更精确的碰撞半径
                    for (PlayerState player : battle.players.values()) {
                        if (player.hp <= 0) continue;
                        double collisionRadius = 35.0; // 调整碰撞半径为更合理的值
                        if (distance(enemy.x, enemy.y, player.x, player.y) < collisionRadius) {
                            player.hp -= 15; // 增加碰撞伤害，使游戏更有挑战性
                            it.remove();
                            break;
                        }
                    }
                }
                
                // 更新子弹位置并检测碰撞
                Iterator<BulletState> bulletIt = battle.bullets.values().iterator();
                while (bulletIt.hasNext()) {
                    BulletState bullet = bulletIt.next();
                    bullet.y += bullet.speedY;
                    
                    // 子弹飞出屏幕，移除
                    if (bullet.y < -40 || bullet.y > WORLD_HEIGHT + 40) {
                        bulletIt.remove();
                        continue;
                    }
                    
                    // 检测子弹与敌机的碰撞
                    boolean hit = false;
                    for (EnemyState enemy : battle.enemies.values()) {
                        if (distance(bullet.x, bullet.y, enemy.x, enemy.y) < 25.0) {
                            // 子弹击中敌机
                            enemy.hp -= 40;
                            bulletIt.remove();
                            hit = true;
                            
                            if (enemy.hp <= 0) {
                                battle.enemies.remove(enemy.id);
                                // 给发射子弹的玩家加分
                                PlayerState shooter = battle.players.get(bullet.ownerId);
                                if (shooter != null) {
                                    long gainedScore = enemy.scoreValue;
                                    long gainedCoins = 5 + battle.random.nextInt(16);
                                    shooter.score += gainedScore;
                                    shooter.coins += gainedCoins;
                                    log.debug("Enemy {} destroyed by bullet from user {} in room {}", 
                                        enemy.id, bullet.ownerId, battle.roomId);
                                }
                            }
                            break;
                        }
                    }
                }

                boolean anyAlive = false;
                for (PlayerState player : battle.players.values()) {
                    if (player.hp > 0) {
                        anyAlive = true;
                        break;
                    }
                }

                broadcastBattleState(battle);

                if (!anyAlive || battle.tickCount > 20 * 120) {
                    finishBattle(battle.roomId, "BATTLE_FINISHED", 0L);
                }
            }
        }
    }

    private void spawnEnemy(BattleRoomState battle) {
        int id = battle.nextEnemyId++;
        float x = 30 + battle.random.nextInt(WORLD_WIDTH - 60);
        float y = -20;
        int type = battle.random.nextInt(3);
        int hp = switch (type) {
            case 0 -> 50;
            case 1 -> 80;
            default -> 120;
        };
        float speed = switch (type) {
            case 0 -> 3.2f;
            case 1 -> 2.4f;
            default -> 1.8f;
        };
        long scoreValue = switch (type) {
            case 0 -> 10;
            case 1 -> 20;
            default -> 50;
        };
        battle.enemies.put(id, new EnemyState(id, type, x, y, hp, speed, scoreValue));
        log.debug("Spawned enemy {} at ({}, {}) in room {}, mode: {}", id, x, y, battle.roomId, battle.gameMode);
    }

    private void broadcastBattleState(BattleRoomState battle) {
        JSONObject payload = new JSONObject();
        payload.put("tick", battle.tickCount);
        payload.put("roomId", battle.roomId);

        JSONArray players = new JSONArray();
        for (PlayerState p : battle.players.values()) {
            JSONObject item = new JSONObject();
            item.put("userId", p.userId);
            item.put("x", p.x);
            item.put("y", p.y);
            item.put("hp", Math.max(0, p.hp));
            item.put("score", p.score);
            item.put("coins", p.coins);
            players.add(item);
        }
        payload.put("players", players);

        JSONArray enemies = new JSONArray();
        for (EnemyState e : battle.enemies.values()) {
            JSONObject item = new JSONObject();
            item.put("id", e.id);
            item.put("type", e.type);
            item.put("x", e.x);
            item.put("y", e.y);
            item.put("hp", e.hp);
            enemies.add(item);
        }
        payload.put("enemies", enemies);
        
        // 添加子弹状态
        JSONArray bullets = new JSONArray();
        for (BulletState b : battle.bullets.values()) {
            JSONObject item = new JSONObject();
            item.put("id", b.id);
            item.put("ownerId", b.ownerId);
            item.put("x", b.x);
            item.put("y", b.y);
            bullets.add(item);
        }
        payload.put("bullets", bullets);
        
        if (battle.tickCount % 20 == 0) {
            log.debug("Room {} broadcasting: {} enemies, {} bullets", battle.roomId, enemies.size(), bullets.size());
        }

        broadcastToRoomAll(battle.roomId, buildMsg("BATTLE_STATE", battle.roomId, 0, payload.toJSONString()));
    }

    private void finishBattle(long roomId, String reason, long sourceUserId) {
        BattleRoomState battle = battles.get(roomId);
        if (battle == null) return;

        synchronized (battle) {
            if (battle.finished) return;
            battle.finished = true;

            JSONObject payload = new JSONObject();
            payload.put("roomId", roomId);
            payload.put("reason", reason);
            payload.put("sourceUserId", sourceUserId);

            JSONArray players = new JSONArray();
            long winnerUserId = 0;
            double bestRating = Double.NEGATIVE_INFINITY;
            for (PlayerState p : battle.players.values()) {
                double rating = 0.1 * p.score + 0.9 * p.coins;
                if (rating > bestRating) {
                    bestRating = rating;
                    winnerUserId = p.userId;
                }
                JSONObject item = new JSONObject();
                item.put("userId", p.userId);
                item.put("hp", Math.max(0, p.hp));
                item.put("score", p.score);
                item.put("coins", p.coins);
                item.put("rating", rating);
                players.add(item);
            }
            payload.put("players", players);
            payload.put("winnerUserId", winnerUserId);

            Room room = store.rooms.get(roomId);
            if (room != null) {
                room.setState(Room.State.FINISHED);
            }

            broadcastToRoomAll(roomId, buildMsg("GAME_OVER", roomId, 0, payload.toJSONString()));
            battles.remove(roomId);
        }
    }

    private JSONObject safePayload(String payload) {
        if (payload == null || payload.isBlank()) return new JSONObject();
        try {
            return JSON.parseObject(payload);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private long requireAuth(WebSocketSession session) throws IOException {
        Long userId = matchManager.getUserIdBySession(session);
        if (userId == null) {
            sendError(session, "please auth first");
            return -1;
        }
        return userId;
    }

    private void broadcastToRoomAll(long roomId, WsMessageDto msg) {
        Room room = store.rooms.get(roomId);
        if (room == null) return;
        sendToUser(room.getPlayer1Id(), msg);
        if (room.getPlayer2Id() != 0) {
            sendToUser(room.getPlayer2Id(), msg);
        }
    }

    private void sendToUser(long userId, WsMessageDto msg) {
        WebSocketSession s = matchManager.getSessionByUserId(userId);
        if (s != null && s.isOpen()) {
            try {
                send(s, msg);
            } catch (IOException e) {
                log.warn("send to user {} failed: {}", userId, e.getMessage());
            }
        }
    }

    private void send(WebSocketSession session, WsMessageDto msg) throws IOException {
        session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
    }

    private void sendError(WebSocketSession session, String reason) throws IOException {
        send(session, buildMsg("ERROR", 0, 0, "{\"reason\":\"" + reason + "\"}"));
    }

    private WsMessageDto buildMsg(String type, long roomId, long userId, String payload) {
        WsMessageDto msg = new WsMessageDto();
        msg.setType(type);
        msg.setRoomId(roomId);
        msg.setUserId(userId);
        msg.setPayload(payload);
        return msg;
    }

    private String roomPayload(Room room) {
        return String.format("{\"roomId\":%d,\"player1Id\":%d,\"player2Id\":%d,\"state\":\"%s\",\"gameMode\":\"%s\"}",
                room.getId(), room.getPlayer1Id(), room.getPlayer2Id(), room.getState(), room.getGameMode());
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double distance(float x1, float y1, float x2, float y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static final class BattleRoomState {
        private final long roomId;
        private final long player1Id;
        private final long player2Id;
        private final GameMode gameMode;
        private final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, EnemyState> enemies = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Integer, BulletState> bullets = new ConcurrentHashMap<>();
        private final Random random;
        private volatile boolean finished = false;
        private int tickCount = 0;
        private int nextEnemyId = 1;
        private int nextBulletId = 1;

        private BattleRoomState(Room room) {
            this.roomId = room.getId();
            this.player1Id = room.getPlayer1Id();
            this.player2Id = room.getPlayer2Id();
            this.gameMode = room.getGameMode();
            this.random = new Random(room.getGameSeed());

            // 根据游戏模式设置玩家初始位置
            if (gameMode == GameMode.PVP) {
                // PVP模式：玩家1在上方，玩家2在下方，相向而行
                players.put(room.getPlayer1Id(), new PlayerState(room.getPlayer1Id(), 256, 100));
                players.put(room.getPlayer2Id(), new PlayerState(room.getPlayer2Id(), 256, 660));
            } else {
                // COOP模式：两个玩家并排在下方
                players.put(room.getPlayer1Id(), new PlayerState(room.getPlayer1Id(), 180, 660));
                players.put(room.getPlayer2Id(), new PlayerState(room.getPlayer2Id(), 330, 660));
            }
        }
    }

    private static final class PlayerState {
        private final long userId;
        private float x;
        private float y;
        private int hp = 100;
        private long score = 0;
        private long coins = 0;

        private PlayerState(long userId, float x, float y) {
            this.userId = userId;
            this.x = x;
            this.y = y;
        }
    }

    private static final class EnemyState {
        private final int id;
        private final int type;
        private final float x;
        private float y;
        private int hp;
        private final float speedY;
        private final long scoreValue;

        private EnemyState(int id, int type, float x, float y, int hp, float speedY, long scoreValue) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.speedY = speedY;
            this.scoreValue = scoreValue;
        }
    }
    
    private static final class BulletState {
        private final int id;
        private final long ownerId; // 发射子弹的玩家ID
        private float x;
        private float y;
        private final float speedY;
        
        private BulletState(int id, long ownerId, float x, float y, float speedY) {
            this.id = id;
            this.ownerId = ownerId;
            this.x = x;
            this.y = y;
            this.speedY = speedY;
        }
    }
}

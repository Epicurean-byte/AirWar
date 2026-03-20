package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.WsMessageDto;
import com.planewar.server.model.entity.Room;
import com.planewar.server.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Optional;

/**
 * WebSocket 消息处理器，负责：
 * - 连接/断开管理
 * - 随机匹配与好友房间创建/加入
 * - 游戏内坐标移动、子弹/敌机同步、伤害事件转发
 *
 * 消息协议（JSON）：
 * {
 *   "type"   : "MATCH_RANDOM | CREATE_ROOM | JOIN_ROOM | START_GAME |
 *               MOVE | FIRE | PICKUP | ENEMY_SPAWN | ENEMY_MOVE |
 *               DAMAGE | SCORE_UPDATE | GAME_OVER",
 *   "roomId" : 0,
 *   "userId" : 12345,
 *   "payload": "{...}"
 * }
 *
 * 客户端连接时须在首条消息中携带 userId（type=AUTH）。
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final MatchManager matchManager;
    private final InMemoryDataStore store;

    public GameWebSocketHandler(MatchManager matchManager, InMemoryDataStore store) {
        this.matchManager = matchManager;
        this.store = store;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("新 WebSocket 连接：{}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessageDto dto;
        try {
            dto = JSON.parseObject(message.getPayload(), WsMessageDto.class);
        } catch (Exception e) {
            sendError(session, "消息格式错误");
            return;
        }

        String type = dto.getType();
        if (type == null) {
            sendError(session, "缺少 type 字段");
            return;
        }

        switch (type) {
            case "AUTH" -> handleAuth(session, dto);
            case "MATCH_RANDOM" -> handleMatchRandom(session, dto);
            case "CREATE_ROOM" -> handleCreateRoom(session, dto);
            case "JOIN_ROOM" -> handleJoinRoom(session, dto);
            case "START_GAME" -> handleStartGame(session, dto);
            // 游戏内实时消息：直接转发给同房间的对手
            case "MOVE", "FIRE", "PICKUP" -> forwardToOpponent(session, dto);
            // 服务端权威消息：敌机生成/移动、伤害、分数更新
            case "ENEMY_SPAWN", "ENEMY_MOVE", "COIN_DROP", "DAMAGE", "SCORE_UPDATE" ->
                    broadcastToRoom(session, dto);
            case "GAME_OVER" -> handleGameOver(session, dto);
            default -> sendError(session, "未知消息类型: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId != 0) {
            // 通知同房间对手
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                WsMessageDto notice = new WsMessageDto();
                notice.setType("OPPONENT_DISCONNECTED");
                notice.setRoomId(roomId);
                notice.setUserId(0);
                notice.setPayload("{\"disconnectedUserId\":" + userId + "}");
                broadcastToRoomExcept(roomId, userId, notice);
            }
            store.findById(userId).ifPresent(u -> u.setOnline(false));
        }
        matchManager.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误：{}", exception.getMessage());
    }

    // -------- 处理器 --------

    private void handleAuth(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = dto.getUserId();
        if (store.findById(userId).isEmpty()) {
            sendError(session, "用户不存在");
            return;
        }
        matchManager.register(session, userId);
        store.findById(userId).ifPresent(u -> u.setOnline(true));
        send(session, buildMsg("AUTH_OK", 0, 0, "{\"userId\":" + userId + "}"));
    }

    private void handleMatchRandom(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;
        Optional<Room> roomOpt = matchManager.randomMatch(userId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            String payload = roomPayload(room);
            send(session, buildMsg("MATCH_SUCCESS", room.getId(), 0, payload));
            // 通知对手
            WebSocketSession opponentSession = matchManager.getSessionByUserId(room.getPlayer1Id());
            if (opponentSession != null) {
                send(opponentSession, buildMsg("MATCH_SUCCESS", room.getId(), 0, payload));
            }
        } else {
            send(session, buildMsg("MATCH_WAITING", 0, 0, "{}"));
        }
    }

    private void handleCreateRoom(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;
        Room room = matchManager.createRoom(userId);
        send(session, buildMsg("ROOM_CREATED", room.getId(), 0,
                "{\"roomId\":" + room.getId() + ",\"hostId\":" + userId + "}"));
    }

    private void handleJoinRoom(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;
        long roomId = dto.getRoomId();
        Optional<Room> roomOpt = matchManager.joinRoom(roomId, userId);
        if (roomOpt.isEmpty()) {
            sendError(session, "房间不存在或已满");
            return;
        }
        Room room = roomOpt.get();
        String payload = roomPayload(room);
        send(session, buildMsg("ROOM_JOINED", room.getId(), 0, payload));
        // 通知房主
        WebSocketSession hostSession = matchManager.getSessionByUserId(room.getPlayer1Id());
        if (hostSession != null) {
            send(hostSession, buildMsg("ROOM_JOINED", room.getId(), 0, payload));
        }
    }

    private void handleStartGame(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) return;
        long roomId = dto.getRoomId();
        Optional<Room> roomOpt = matchManager.startGame(roomId, userId);
        if (roomOpt.isEmpty()) {
            sendError(session, "无法开始游戏，请检查房间状态");
            return;
        }
        Room room = roomOpt.get();
        // 构建初始化指令，含随机种子和难度配置
        String initPayload = String.format(
                "{\"roomId\":%d,\"seed\":%d,\"player1Id\":%d,\"player2Id\":%d," +
                "\"difficulty\":{\"enemyHpMultiplier\":1.5,\"spawnRate\":1.3,\"bulletDensity\":1.2}}",
                room.getId(), room.getGameSeed(), room.getPlayer1Id(), room.getPlayer2Id());
        broadcastToRoomAll(room.getId(), buildMsg("GAME_START", room.getId(), 0, initPayload));
    }

    private void forwardToOpponent(WebSocketSession session, WsMessageDto dto) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;
        dto.setRoomId(roomId);
        dto.setUserId(userId);
        broadcastToRoomExcept(roomId, userId, dto);
    }

    private void broadcastToRoom(WebSocketSession session, WsMessageDto dto) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;
        dto.setRoomId(roomId);
        dto.setUserId(userId);
        broadcastToRoomAll(roomId, dto);
    }

    private void handleGameOver(WebSocketSession session, WsMessageDto dto) {
        long userId = Optional.ofNullable(matchManager.getUserIdBySession(session)).orElse(0L);
        if (userId == 0) return;
        Long roomId = matchManager.getRoomIdByUserId(userId);
        if (roomId == null) return;
        // 通知对手游戏结束
        broadcastToRoomExcept(roomId, userId, dto);
    }

    // -------- 工具方法 --------

    private long requireAuth(WebSocketSession session) throws IOException {
        Long userId = matchManager.getUserIdBySession(session);
        if (userId == null) {
            sendError(session, "请先发送 AUTH 消息完成认证");
            return -1;
        }
        return userId;
    }

    private void broadcastToRoomAll(long roomId, WsMessageDto msg) {
        Room room = store.rooms.get(roomId);
        if (room == null) return;
        sendToUser(room.getPlayer1Id(), msg);
        if (room.getPlayer2Id() != 0) sendToUser(room.getPlayer2Id(), msg);
    }

    private void broadcastToRoomExcept(long roomId, long exceptUserId, WsMessageDto msg) {
        Room room = store.rooms.get(roomId);
        if (room == null) return;
        if (room.getPlayer1Id() != exceptUserId) sendToUser(room.getPlayer1Id(), msg);
        if (room.getPlayer2Id() != 0 && room.getPlayer2Id() != exceptUserId) sendToUser(room.getPlayer2Id(), msg);
    }

    private void sendToUser(long userId, WsMessageDto msg) {
        WebSocketSession s = matchManager.getSessionByUserId(userId);
        if (s != null && s.isOpen()) {
            try {
                send(s, msg);
            } catch (IOException e) {
                log.warn("发送消息至用户 {} 失败：{}", userId, e.getMessage());
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
        return String.format("{\"roomId\":%d,\"player1Id\":%d,\"player2Id\":%d,\"state\":\"%s\"}",
                room.getId(), room.getPlayer1Id(), room.getPlayer2Id(), room.getState());
    }
}

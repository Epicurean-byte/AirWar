package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSON;
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
import java.util.Optional;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final InMemoryDataStore store;
    private final SessionRegistry sessions;
    private final MatchManager matchManager;
    private final BattleService battleService;
    private final WsMessageSender sender;

    public GameWebSocketHandler(InMemoryDataStore store,
                                SessionRegistry sessions,
                                MatchManager matchManager,
                                BattleService battleService,
                                WsMessageSender sender) {
        this.store = store;
        this.sessions = sessions;
        this.matchManager = matchManager;
        this.battleService = battleService;
        this.sender = sender;
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
            sender.sendError(session, "invalid message json");
            return;
        }

        if (dto.getType() == null) {
            sender.sendError(session, "missing type");
            return;
        }

        switch (dto.getType()) {
            case "AUTH" -> handleAuth(session, dto);
            case "MATCH_RANDOM" -> handleMatchRandom(session);
            case "CREATE_ROOM" -> handleCreateRoom(session, dto);
            case "JOIN_ROOM" -> handleJoinRoom(session, dto.getRoomId());
            case "START_GAME" -> handleStartGame(session, dto.getRoomId());
            case "MOVE" -> handleMove(session, dto);
            case "FIRE" -> handleFire(session);
            case "PICKUP" -> {
                // Reserved for compatibility in server-authoritative mode.
            }
            case "GAME_OVER" -> handleGameOver(session);
            default -> sender.sendError(session, "unknown type: " + dto.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.unregister(session).ifPresent(userId -> {
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                battleService.finishBattle(roomId, "PLAYER_DISCONNECTED", userId);
            }
            store.findById(userId).ifPresent(user -> user.setOnline(false));
            matchManager.handleDisconnect(userId);
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: {}", exception.getMessage());
    }

    private void handleAuth(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = dto.getUserId();
        if (store.findById(userId).isEmpty()) {
            sender.sendError(session, "user not found");
            return;
        }
        sessions.register(session, userId);
        store.findById(userId).ifPresent(user -> user.setOnline(true));
        sender.send(session, WsMessageFactory.authOk(userId));
    }

    private void handleMatchRandom(WebSocketSession session) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) {
            return;
        }

        Optional<Room> roomOpt = matchManager.randomMatch(userId);
        if (roomOpt.isEmpty()) {
            sender.send(session, WsMessageFactory.matchWaiting());
            return;
        }

        Room room = roomOpt.get();
        sender.broadcastToRoom(room.getId(), WsMessageFactory.matchSuccess(room));
    }

    private void handleCreateRoom(WebSocketSession session, WsMessageDto dto) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) {
            return;
        }

        Optional<Room> roomOpt = matchManager.createRoom(userId);
        if (roomOpt.isEmpty()) {
            sender.sendError(session, "cannot create room while already in an active room");
            return;
        }

        Room room = roomOpt.get();
        room.setGameMode(resolveGameMode(dto.getPayload()));
        sender.send(session, WsMessageFactory.roomCreated(room, userId));
    }

    private void handleJoinRoom(WebSocketSession session, long roomId) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) {
            return;
        }

        Optional<Room> roomOpt = matchManager.joinRoom(roomId, userId);
        if (roomOpt.isEmpty()) {
            sender.sendError(session, "room not found, already full, or you are already waiting in another room");
            return;
        }

        Room room = roomOpt.get();
        sender.broadcastToRoom(room.getId(), WsMessageFactory.roomJoined(room));
    }

    private void handleStartGame(WebSocketSession session, long roomId) throws IOException {
        long userId = requireAuth(session);
        if (userId < 0) {
            return;
        }

        Optional<Room> roomOpt = matchManager.startGame(roomId, userId);
        if (roomOpt.isEmpty()) {
            sender.sendError(session, "cannot start game: room must be waiting, full, and started by host");
            return;
        }

        battleService.startBattle(roomOpt.get());
    }

    private void handleMove(WebSocketSession session, WsMessageDto dto) {
        sessions.userIdOf(session).ifPresent(userId -> {
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                battleService.move(roomId, userId, WsMessageFactory.safePayload(dto.getPayload()));
            }
        });
    }

    private void handleFire(WebSocketSession session) {
        sessions.userIdOf(session).ifPresent(userId -> {
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                battleService.fire(roomId, userId);
            }
        });
    }

    private void handleGameOver(WebSocketSession session) {
        sessions.userIdOf(session).ifPresent(userId -> {
            Long roomId = matchManager.getRoomIdByUserId(userId);
            if (roomId != null) {
                battleService.finishBattle(roomId, "PLAYER_QUIT", userId);
            }
        });
    }

    private long requireAuth(WebSocketSession session) throws IOException {
        Optional<Long> userId = sessions.userIdOf(session);
        if (userId.isEmpty()) {
            sender.sendError(session, "please auth first");
            return -1;
        }
        return userId.get();
    }

    private static GameMode resolveGameMode(String payloadText) {
        JSONObject payload = WsMessageFactory.safePayload(payloadText);
        String mode = payload.getString("gameMode");
        if (mode == null || mode.isBlank()) {
            return GameMode.COOP;
        }
        try {
            return GameMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return GameMode.COOP;
        }
    }
}

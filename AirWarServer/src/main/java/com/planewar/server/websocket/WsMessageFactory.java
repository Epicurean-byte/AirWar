package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.planewar.server.model.dto.WsMessageDto;
import com.planewar.server.model.entity.Room;

public final class WsMessageFactory {
    private WsMessageFactory() {
    }

    public static WsMessageDto authOk(long userId) {
        JSONObject payload = new JSONObject();
        payload.put("userId", userId);
        return message("AUTH_OK", 0, 0, payload.toJSONString());
    }

    public static WsMessageDto matchWaiting() {
        return message("MATCH_WAITING", 0, 0, "{}");
    }

    public static WsMessageDto matchSuccess(Room room) {
        return message("MATCH_SUCCESS", room.getId(), 0, roomPayload(room));
    }

    public static WsMessageDto roomCreated(Room room, long hostId) {
        JSONObject payload = new JSONObject();
        payload.put("roomId", room.getId());
        payload.put("hostId", hostId);
        payload.put("gameMode", room.getGameMode().name());
        return message("ROOM_CREATED", room.getId(), 0, payload.toJSONString());
    }

    public static WsMessageDto roomJoined(Room room) {
        return message("ROOM_JOINED", room.getId(), 0, roomPayload(room));
    }

    public static WsMessageDto gameStart(Room room) {
        JSONObject difficulty = new JSONObject();
        difficulty.put("enemyHpMultiplier", 1.5);
        difficulty.put("spawnRate", 1.3);
        difficulty.put("bulletDensity", 1.2);

        JSONObject payload = new JSONObject();
        payload.put("roomId", room.getId());
        payload.put("seed", room.getGameSeed());
        payload.put("player1Id", room.getPlayer1Id());
        payload.put("player2Id", room.getPlayer2Id());
        payload.put("gameMode", room.getGameMode().name());
        payload.put("player1SkinId", room.getPlayer1SkinId());
        payload.put("player2SkinId", room.getPlayer2SkinId());
        payload.put("difficulty", difficulty);
        return message("GAME_START", room.getId(), 0, payload.toJSONString());
    }

    public static WsMessageDto battleState(long roomId, String payload) {
        return message("BATTLE_STATE", roomId, 0, payload);
    }

    public static WsMessageDto gameOver(long roomId, String payload) {
        return message("GAME_OVER", roomId, 0, payload);
    }

    public static WsMessageDto error(String reason) {
        JSONObject payload = new JSONObject();
        payload.put("reason", reason);
        return message("ERROR", 0, 0, payload.toJSONString());
    }

    public static WsMessageDto message(String type, long roomId, long userId, String payload) {
        WsMessageDto msg = new WsMessageDto();
        msg.setType(type);
        msg.setRoomId(roomId);
        msg.setUserId(userId);
        msg.setPayload(payload);
        return msg;
    }

    public static JSONObject safePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(payload);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String roomPayload(Room room) {
        JSONObject payload = new JSONObject();
        payload.put("roomId", room.getId());
        payload.put("player1Id", room.getPlayer1Id());
        payload.put("player2Id", room.getPlayer2Id());
        payload.put("state", room.getState().name());
        payload.put("gameMode", room.getGameMode().name());
        return payload.toJSONString();
    }
}

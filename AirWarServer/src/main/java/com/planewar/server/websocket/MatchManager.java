package com.planewar.server.websocket;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 匹配池管理：随机匹配与好友邀请房间的调度逻辑。
 */
@Component
public class MatchManager {

    private static final Logger log = LoggerFactory.getLogger(MatchManager.class);

    private final InMemoryDataStore store;

    /** sessionId -> userId，记录每个 WS 连接对应的用户 */
    public final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /** userId -> WebSocketSession */
    public final ConcurrentHashMap<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    /** userId -> roomId */
    public final ConcurrentHashMap<Long, Long> userRoomMap = new ConcurrentHashMap<>();

    public MatchManager(InMemoryDataStore store) {
        this.store = store;
    }

    /**
     * 注册用户 WebSocket 会话。
     */
    public void register(WebSocketSession session, long userId) {
        sessionUserMap.put(session.getId(), userId);
        userSessionMap.put(userId, session);
        log.debug("用户 {} 已连接 WebSocket", userId);
    }

    /**
     * 移除用户 WebSocket 会话，清理匹配池和房间。
     */
    public void unregister(WebSocketSession session) {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId == null) return;
        userSessionMap.remove(userId);
        // 从随机匹配池中移除
        synchronized (store.matchPool) {
            store.matchPool.remove(userId);
        }
        // 离开房间
        Long roomId = userRoomMap.remove(userId);
        if (roomId != null) {
            Room room = store.rooms.get(roomId);
            if (room != null) {
                room.setState(Room.State.FINISHED);
            }
        }
        log.debug("用户 {} 已断开 WebSocket", userId);
    }

    /**
     * 随机匹配：加入等待池，若已有等待者则立即成房。
     * @return 新建或匹配到的 Room，若仍在等待则返回 empty
     */
    public Optional<Room> randomMatch(long userId) {
        synchronized (store.matchPool) {
            if (!store.matchPool.isEmpty()) {
                long opponentId = store.matchPool.poll();
                if (opponentId == userId) {
                    // 不能自己匹配自己
                    store.matchPool.offer(opponentId);
                    return Optional.empty();
                }
                Room room = new Room(opponentId);
                room.setPlayer2Id(userId);
                store.rooms.put(room.getId(), room);
                userRoomMap.put(opponentId, room.getId());
                userRoomMap.put(userId, room.getId());
                log.info("随机匹配成功，房间 {}，玩家 {} vs {}", room.getId(), opponentId, userId);
                return Optional.of(room);
            } else {
                store.matchPool.offer(userId);
                log.info("用户 {} 加入随机匹配等待池", userId);
                return Optional.empty();
            }
        }
    }

    /**
     * 好友邀请：创建房间（房主调用）。
     */
    public Room createRoom(long hostId) {
        Room room = new Room(hostId);
        store.rooms.put(room.getId(), room);
        userRoomMap.put(hostId, room.getId());
        log.info("用户 {} 创建房间 {}", hostId, room.getId());
        return room;
    }

    /**
     * 好友加入房间。
     */
    public Optional<Room> joinRoom(long roomId, long userId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.isFull() || room.getState() != Room.State.WAITING) {
            return Optional.empty();
        }
        room.setPlayer2Id(userId);
        userRoomMap.put(userId, roomId);
        log.info("用户 {} 加入房间 {}", userId, roomId);
        return Optional.of(room);
    }

    /**
     * 房主开始游戏，生成随机种子并切换房间状态。
     */
    public Optional<Room> startGame(long roomId, long hostId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.getPlayer1Id() != hostId || !room.isFull()) {
            return Optional.empty();
        }
        room.setGameSeed(System.currentTimeMillis());
        room.setState(Room.State.IN_GAME);
        log.info("房间 {} 游戏开始，种子 {}", roomId, room.getGameSeed());
        return Optional.of(room);
    }

    public Long getUserIdBySession(WebSocketSession session) {
        return sessionUserMap.get(session.getId());
    }

    public WebSocketSession getSessionByUserId(long userId) {
        return userSessionMap.get(userId);
    }

    public Long getRoomIdByUserId(long userId) {
        return userRoomMap.get(userId);
    }
}

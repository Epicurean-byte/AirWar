package com.planewar.server.websocket;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MatchManager {

    private static final Logger log = LoggerFactory.getLogger(MatchManager.class);

    private final InMemoryDataStore store;

    // sessionId -> userId
    public final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    // userId -> session
    public final ConcurrentHashMap<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();
    // userId -> roomId
    public final ConcurrentHashMap<Long, Long> userRoomMap = new ConcurrentHashMap<>();

    public MatchManager(InMemoryDataStore store) {
        this.store = store;
    }

    public void register(WebSocketSession session, long userId) {
        sessionUserMap.put(session.getId(), userId);
        userSessionMap.put(userId, session);
        log.debug("user {} websocket connected", userId);
    }

    public void unregister(WebSocketSession session) {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId == null) return;

        userSessionMap.remove(userId);
        synchronized (store.matchPool) {
            store.matchPool.remove(userId);
        }

        Long roomId = userRoomMap.remove(userId);
        if (roomId != null) {
            Room room = store.rooms.get(roomId);
            if (room != null) {
                room.setState(Room.State.FINISHED);
                userRoomMap.remove(room.getPlayer1Id());
                if (room.getPlayer2Id() != 0) {
                    userRoomMap.remove(room.getPlayer2Id());
                }
            }
        }
        log.debug("user {} websocket disconnected", userId);
    }

    public Optional<Room> randomMatch(long userId) {
        synchronized (store.matchPool) {
            // Ensure no duplicates while user retries matching.
            store.matchPool.remove(userId);

            if (!store.matchPool.isEmpty()) {
                long opponentId = store.matchPool.poll();
                if (opponentId == userId) {
                    return Optional.empty();
                }
                Room room = new Room(opponentId);
                room.setPlayer2Id(userId);
                store.rooms.put(room.getId(), room);
                userRoomMap.put(opponentId, room.getId());
                userRoomMap.put(userId, room.getId());
                log.info("random match success room {}: {} vs {}", room.getId(), opponentId, userId);
                return Optional.of(room);
            }

            store.matchPool.offer(userId);
            log.info("user {} joined random match queue", userId);
            return Optional.empty();
        }
    }

    public Room createRoom(long hostId) {
        Room room = new Room(hostId);
        store.rooms.put(room.getId(), room);
        userRoomMap.put(hostId, room.getId());
        log.info("user {} created room {}", hostId, room.getId());
        return room;
    }

    public Optional<Room> joinRoom(long roomId, long userId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.isFull() || room.getState() != Room.State.WAITING) {
            return Optional.empty();
        }
        room.setPlayer2Id(userId);
        userRoomMap.put(userId, roomId);
        log.info("user {} joined room {}", userId, roomId);
        return Optional.of(room);
    }

    public Optional<Room> startGame(long roomId, long hostId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.getPlayer1Id() != hostId || !room.isFull()) {
            return Optional.empty();
        }
        room.setGameSeed(System.currentTimeMillis());
        room.setState(Room.State.IN_GAME);
        log.info("room {} started with seed {}", roomId, room.getGameSeed());
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

package com.planewar.server.websocket;

import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MatchManager {

    private static final Logger log = LoggerFactory.getLogger(MatchManager.class);

    private final InMemoryDataStore store;
    private final ConcurrentHashMap<Long, Long> userRoomMap = new ConcurrentHashMap<>();

    public MatchManager(InMemoryDataStore store) {
        this.store = store;
    }

    public void handleDisconnect(long userId) {
        synchronized (store.matchPool) {
            store.matchPool.remove(userId);
        }
        Long roomId = userRoomMap.remove(userId);
        if (roomId != null) {
            Room room = store.rooms.get(roomId);
            if (room != null) {
                if (room.getState() == Room.State.WAITING) {
                    cleanupWaitingRoom(room, userId);
                } else {
                    finishRoom(roomId);
                }
            }
        }
    }

    public Optional<Room> randomMatch(long userId) {
        synchronized (store.matchPool) {
            if (!leaveWaitingRoomOrRejectActive(userId)) {
                return Optional.empty();
            }
            // Ensure no duplicates while user retries matching.
            store.matchPool.remove(userId);

            if (!store.matchPool.isEmpty()) {
                long opponentId = store.matchPool.poll();
                if (opponentId == userId) {
                    return Optional.empty();
                }
                Room room = new Room(opponentId);
                room.setPlayer2Id(userId);
                room.setPlayer1SkinId(equippedSkinId(opponentId));
                room.setPlayer2SkinId(equippedSkinId(userId));
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

    public Optional<Room> createRoom(long hostId) {
        if (!leaveWaitingRoomOrRejectActive(hostId)) {
            return Optional.empty();
        }
        Room room = new Room(hostId);
        room.setPlayer1SkinId(equippedSkinId(hostId));
        store.rooms.put(room.getId(), room);
        userRoomMap.put(hostId, room.getId());
        log.info("user {} created room {}", hostId, room.getId());
        return Optional.of(room);
    }

    public Optional<Room> joinRoom(long roomId, long userId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.getState() != Room.State.WAITING) {
            return Optional.empty();
        }
        if (room.getPlayer1Id() == userId || room.getPlayer2Id() == userId) {
            return Optional.of(room);
        }
        if (room.isFull()) {
            return Optional.empty();
        }

        if (!leaveWaitingRoomOrRejectActive(userId, roomId)) {
            return Optional.empty();
        }

        if (room.getPlayer1Id() == 0 || room.getPlayer1Id() == userId) {
            return Optional.empty();
        }
        room.setPlayer2Id(userId);
        room.setPlayer2SkinId(equippedSkinId(userId));
        userRoomMap.put(userId, roomId);
        log.info("user {} joined room {}", userId, roomId);
        return Optional.of(room);
    }

    private boolean leaveWaitingRoomOrRejectActive(long userId) {
        return leaveWaitingRoomOrRejectActive(userId, 0);
    }

    private boolean leaveWaitingRoomOrRejectActive(long userId, long targetRoomId) {
        Long existingRoomId = userRoomMap.get(userId);
        if (existingRoomId == null || existingRoomId == targetRoomId) {
            return true;
        }

        Room existingRoom = store.rooms.get(existingRoomId);
        if (existingRoom == null || existingRoom.getState() == Room.State.FINISHED) {
            userRoomMap.remove(userId);
            return true;
        }
        if (existingRoom.getState() == Room.State.WAITING) {
            cleanupWaitingRoom(existingRoom, userId);
            return true;
        }
        log.warn("user {} cannot leave active room {} for room {}", userId, existingRoomId, targetRoomId);
        return false;
    }

    private void cleanupWaitingRoom(Room room, long userId) {
        long roomId = room.getId();
        if (room.getState() != Room.State.WAITING) {
            return;
        }
        if (room.getPlayer1Id() == userId) {
            if (room.getPlayer2Id() == 0) {
                store.rooms.remove(roomId);
            } else {
                room.setPlayer1Id(room.getPlayer2Id());
                room.setPlayer1SkinId(room.getPlayer2SkinId());
                room.setPlayer2Id(0);
                room.setPlayer2SkinId(0);
                userRoomMap.put(room.getPlayer1Id(), roomId);
            }
            userRoomMap.remove(userId);
            log.info("user {} left waiting room {} as host", userId, roomId);
            return;
        }
        if (room.getPlayer2Id() == userId) {
            room.setPlayer2Id(0);
            room.setPlayer2SkinId(0);
            userRoomMap.remove(userId);
            log.info("user {} left waiting room {} as guest", userId, roomId);
        }
    }

    private int equippedSkinId(long userId) {
        return store.findById(userId)
                .map(user -> user.getEquippedSkinId())
                .orElse(0);
    }

    public Optional<Room> startGame(long roomId, long hostId) {
        Room room = store.rooms.get(roomId);
        if (room == null || room.getPlayer1Id() != hostId || !room.isFull() || room.getState() != Room.State.WAITING) {
            return Optional.empty();
        }
        room.setGameSeed(System.currentTimeMillis());
        room.setState(Room.State.IN_GAME);
        log.info("room {} started with seed {}", roomId, room.getGameSeed());
        return Optional.of(room);
    }

    public void finishRoom(long roomId) {
        Room room = store.rooms.get(roomId);
        if (room == null) {
            return;
        }
        room.setState(Room.State.FINISHED);
        userRoomMap.remove(room.getPlayer1Id());
        if (room.getPlayer2Id() != 0) {
            userRoomMap.remove(room.getPlayer2Id());
        }
        synchronized (store.matchPool) {
            store.matchPool.remove(room.getPlayer1Id());
            if (room.getPlayer2Id() != 0) {
                store.matchPool.remove(room.getPlayer2Id());
            }
        }
        log.info("room {} finished and user mappings cleared", roomId);
    }

    public Long getRoomIdByUserId(long userId) {
        return userRoomMap.get(userId);
    }
}

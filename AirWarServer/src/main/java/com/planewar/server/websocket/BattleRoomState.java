package com.planewar.server.websocket;

import com.planewar.server.model.entity.GameMode;
import com.planewar.server.model.entity.Room;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class BattleRoomState {
    final long roomId;
    final long player1Id;
    final long player2Id;
    final GameMode gameMode;
    final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, EnemyState> enemies = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, BulletState> bullets = new ConcurrentHashMap<>();
    final Random random;
    volatile boolean finished = false;
    int tickCount = 0;
    int nextEnemyId = 1;
    int nextBulletId = 1;

    public BattleRoomState(Room room) {
        this.roomId = room.getId();
        this.player1Id = room.getPlayer1Id();
        this.player2Id = room.getPlayer2Id();
        this.gameMode = room.getGameMode();
        this.random = new Random(room.getGameSeed());

        if (gameMode == GameMode.PVP) {
            players.put(room.getPlayer1Id(), new PlayerState(room.getPlayer1Id(), 256, 100));
            players.put(room.getPlayer2Id(), new PlayerState(room.getPlayer2Id(), 256, 660));
        } else {
            players.put(room.getPlayer1Id(), new PlayerState(room.getPlayer1Id(), 180, 660));
            players.put(room.getPlayer2Id(), new PlayerState(room.getPlayer2Id(), 330, 660));
        }
    }

    static final class PlayerState {
        final long userId;
        float x;
        float y;
        int hp = 100;
        long score = 0;
        long coins = 0;

        PlayerState(long userId, float x, float y) {
            this.userId = userId;
            this.x = x;
            this.y = y;
        }
    }

    static final class EnemyState {
        final int id;
        final int type;
        final float x;
        float y;
        int hp;
        final float speedY;
        final long scoreValue;

        EnemyState(int id, int type, float x, float y, int hp, float speedY, long scoreValue) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.speedY = speedY;
            this.scoreValue = scoreValue;
        }
    }

    static final class BulletState {
        final int id;
        final long ownerId;
        float x;
        float y;
        final float speedY;

        BulletState(int id, long ownerId, float x, float y, float speedY) {
            this.id = id;
            this.ownerId = ownerId;
            this.x = x;
            this.y = y;
            this.speedY = speedY;
        }
    }
}

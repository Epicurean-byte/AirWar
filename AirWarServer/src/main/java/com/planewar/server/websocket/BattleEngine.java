package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.planewar.server.model.entity.GameMode;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class BattleEngine {
    private static final Logger log = LoggerFactory.getLogger(BattleEngine.class);

    private static final int WORLD_WIDTH = 512;
    private static final int WORLD_HEIGHT = 768;
    private static final int MAX_ENEMIES = 12;
    private static final int MAX_TICKS = 20 * 120;

    public BattleRoomState create(Room room) {
        return new BattleRoomState(room);
    }

    public void move(BattleRoomState battle, long userId, JSONObject payload) {
        BattleRoomState.PlayerState player = battle.players.get(userId);
        if (player == null || player.hp <= 0) {
            return;
        }

        float x = clamp((float) payload.getDoubleValue("x"), 0, WORLD_WIDTH);
        float y = (float) payload.getDoubleValue("y");
        if (battle.gameMode == GameMode.PVP) {
            if (userId == battle.player1Id) {
                y = clamp(y, 0, WORLD_HEIGHT / 2f - 20);
            } else {
                y = clamp(y, WORLD_HEIGHT / 2f + 20, WORLD_HEIGHT);
            }
        } else {
            y = clamp(y, 0, WORLD_HEIGHT);
        }

        player.x = x;
        player.y = y;
    }

    public void fire(BattleRoomState battle, long userId) {
        BattleRoomState.PlayerState player = battle.players.get(userId);
        if (player == null || player.hp <= 0) {
            return;
        }

        int bulletId = battle.nextBulletId++;
        battle.bullets.put(bulletId, new BattleRoomState.BulletState(
                bulletId,
                userId,
                player.x,
                player.y - 30,
                -10f
        ));
        log.debug("user {} fired bullet {} in room {}", userId, bulletId, battle.roomId);
    }

    public TickResult tick(BattleRoomState battle) {
        battle.tickCount++;

        if (battle.tickCount % 16 == 0 && battle.enemies.size() < MAX_ENEMIES) {
            spawnEnemy(battle);
        }

        moveEnemiesAndDetectCollisions(battle);
        moveBulletsAndDetectHits(battle);

        boolean anyAlive = battle.players.values().stream().anyMatch(player -> player.hp > 0);
        return new TickResult(buildStatePayload(battle), !anyAlive || battle.tickCount > MAX_TICKS);
    }

    public String buildGameOverPayload(BattleRoomState battle, Room room, String reason, long sourceUserId) {
        JSONObject payload = new JSONObject();
        payload.put("roomId", battle.roomId);
        payload.put("reason", reason);
        payload.put("sourceUserId", sourceUserId);

        JSONArray players = new JSONArray();
        long winnerUserId = 0;
        double bestRating = Double.NEGATIVE_INFINITY;
        if (room != null) {
            room.getBattleResults().clear();
            room.getSettledUserIds().clear();
        }

        for (BattleRoomState.PlayerState player : battle.players.values()) {
            double rating = 0.1 * player.score + 0.9 * player.coins;
            if (rating > bestRating) {
                bestRating = rating;
                winnerUserId = player.userId;
            }

            int hp = Math.max(0, player.hp);
            JSONObject item = new JSONObject();
            item.put("userId", player.userId);
            item.put("hp", hp);
            item.put("score", player.score);
            item.put("coins", player.coins);
            item.put("rating", rating);
            players.add(item);

            if (room != null) {
                room.getBattleResults().put(
                        player.userId,
                        new Room.PlayerBattleResult(player.userId, hp, player.score, player.coins, rating)
                );
            }
        }

        payload.put("players", players);
        payload.put("winnerUserId", winnerUserId);
        return payload.toJSONString();
    }

    private void moveEnemiesAndDetectCollisions(BattleRoomState battle) {
        Iterator<BattleRoomState.EnemyState> it = battle.enemies.values().iterator();
        while (it.hasNext()) {
            BattleRoomState.EnemyState enemy = it.next();
            enemy.y += enemy.speedY;
            if (enemy.y > WORLD_HEIGHT + 40) {
                it.remove();
                continue;
            }

            for (BattleRoomState.PlayerState player : battle.players.values()) {
                if (player.hp <= 0) {
                    continue;
                }
                if (distance(enemy.x, enemy.y, player.x, player.y) < 35.0) {
                    player.hp -= 15;
                    it.remove();
                    break;
                }
            }
        }
    }

    private void moveBulletsAndDetectHits(BattleRoomState battle) {
        Iterator<BattleRoomState.BulletState> bulletIt = battle.bullets.values().iterator();
        while (bulletIt.hasNext()) {
            BattleRoomState.BulletState bullet = bulletIt.next();
            bullet.y += bullet.speedY;

            if (bullet.y < -40 || bullet.y > WORLD_HEIGHT + 40) {
                bulletIt.remove();
                continue;
            }

            for (BattleRoomState.EnemyState enemy : battle.enemies.values()) {
                if (distance(bullet.x, bullet.y, enemy.x, enemy.y) < 25.0) {
                    enemy.hp -= 40;
                    bulletIt.remove();

                    if (enemy.hp <= 0) {
                        battle.enemies.remove(enemy.id);
                        BattleRoomState.PlayerState shooter = battle.players.get(bullet.ownerId);
                        if (shooter != null) {
                            shooter.score += enemy.scoreValue;
                            shooter.coins += 5 + battle.random.nextInt(16);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void spawnEnemy(BattleRoomState battle) {
        int id = battle.nextEnemyId++;
        float x = 30 + battle.random.nextInt(WORLD_WIDTH - 60);
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
        battle.enemies.put(id, new BattleRoomState.EnemyState(id, type, x, -20, hp, speed, scoreValue));
    }

    private String buildStatePayload(BattleRoomState battle) {
        JSONObject payload = new JSONObject();
        payload.put("tick", battle.tickCount);
        payload.put("roomId", battle.roomId);

        JSONArray players = new JSONArray();
        for (BattleRoomState.PlayerState player : battle.players.values()) {
            JSONObject item = new JSONObject();
            item.put("userId", player.userId);
            item.put("x", player.x);
            item.put("y", player.y);
            item.put("hp", Math.max(0, player.hp));
            item.put("score", player.score);
            item.put("coins", player.coins);
            players.add(item);
        }
        payload.put("players", players);

        JSONArray enemies = new JSONArray();
        for (BattleRoomState.EnemyState enemy : battle.enemies.values()) {
            JSONObject item = new JSONObject();
            item.put("id", enemy.id);
            item.put("type", enemy.type);
            item.put("x", enemy.x);
            item.put("y", enemy.y);
            item.put("hp", enemy.hp);
            enemies.add(item);
        }
        payload.put("enemies", enemies);

        JSONArray bullets = new JSONArray();
        for (BattleRoomState.BulletState bullet : battle.bullets.values()) {
            JSONObject item = new JSONObject();
            item.put("id", bullet.id);
            item.put("ownerId", bullet.ownerId);
            item.put("x", bullet.x);
            item.put("y", bullet.y);
            bullets.add(item);
        }
        payload.put("bullets", bullets);
        return payload.toJSONString();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double distance(float x1, float y1, float x2, float y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static final class TickResult {
        private final String statePayload;
        private final boolean shouldFinish;

        public TickResult(String statePayload, boolean shouldFinish) {
            this.statePayload = statePayload;
            this.shouldFinish = shouldFinish;
        }

        public String getStatePayload() {
            return statePayload;
        }

        public boolean shouldFinish() {
            return shouldFinish;
        }
    }
}

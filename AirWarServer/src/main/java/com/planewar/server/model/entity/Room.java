package com.planewar.server.model.entity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Room {

    public enum State { WAITING, IN_GAME, FINISHED }

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private long id;
    private long player1Id;
    private long player2Id;   // 0 表示空位
    private State state;
    /** 随机种子，开始游戏时由服务端生成并下发 */
    private long gameSeed;
    /** 游戏模式：PVP对战或COOP合作 */
    private GameMode gameMode = GameMode.COOP; // 默认合作模式
    /** 玩家1的皮肤ID */
    private int player1SkinId = 0;
    /** 玩家2的皮肤ID */
    private int player2SkinId = 0;
    /** 服务端权威战斗结果，仅内存保存，用于 PVP 结算校验。 */
    private final Map<Long, PlayerBattleResult> battleResults = new ConcurrentHashMap<>();
    /** 已领取 PVP 结算的玩家，保证结算接口幂等。 */
    private final Set<Long> settledUserIds = ConcurrentHashMap.newKeySet();

    public Room() {
    }

    public Room(long player1Id) {
        this.id = ID_GEN.getAndIncrement();
        this.player1Id = player1Id;
        this.state = State.WAITING;
    }

    public boolean isFull() {
        return player2Id != 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(long player1Id) {
        this.player1Id = player1Id;
    }

    public long getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(long player2Id) {
        this.player2Id = player2Id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getGameSeed() {
        return gameSeed;
    }

    public void setGameSeed(long gameSeed) {
        this.gameSeed = gameSeed;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public int getPlayer1SkinId() {
        return player1SkinId;
    }

    public void setPlayer1SkinId(int player1SkinId) {
        this.player1SkinId = player1SkinId;
    }

    public int getPlayer2SkinId() {
        return player2SkinId;
    }

    public void setPlayer2SkinId(int player2SkinId) {
        this.player2SkinId = player2SkinId;
    }

    public boolean containsPlayer(long userId) {
        return player1Id == userId || player2Id == userId;
    }

    public Map<Long, PlayerBattleResult> getBattleResults() {
        return battleResults;
    }

    public Set<Long> getSettledUserIds() {
        return settledUserIds;
    }

    public static final class PlayerBattleResult {
        private long userId;
        private int hp;
        private long score;
        private long coins;
        private double rating;

        public PlayerBattleResult() {
        }

        public PlayerBattleResult(long userId, int hp, long score, long coins, double rating) {
            this.userId = userId;
            this.hp = hp;
            this.score = score;
            this.coins = coins;
            this.rating = rating;
        }

        public long getUserId() {
            return userId;
        }

        public int getHp() {
            return hp;
        }

        public long getScore() {
            return score;
        }

        public long getCoins() {
            return coins;
        }

        public double getRating() {
            return rating;
        }
    }
}

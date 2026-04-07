package com.planewar.server.model.entity;

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
}

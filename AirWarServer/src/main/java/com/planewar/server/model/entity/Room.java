package com.planewar.server.model.entity;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class Room {

    public enum State { WAITING, IN_GAME, FINISHED }

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    private long id;
    private long player1Id;
    private long player2Id;   // 0 表示空位
    private State state;
    /** 随机种子，开始游戏时由服务端生成并下发 */
    private long gameSeed;

    public Room(long player1Id) {
        this.id = ID_GEN.getAndIncrement();
        this.player1Id = player1Id;
        this.state = State.WAITING;
    }

    public boolean isFull() {
        return player2Id != 0;
    }
}

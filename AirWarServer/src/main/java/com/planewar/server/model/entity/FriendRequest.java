package com.planewar.server.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@Data
@NoArgsConstructor
public class FriendRequest {

    public enum Status { PENDING, ACCEPTED, REJECTED }

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    public static void initIdGen(long nextId) {
        ID_GEN.updateAndGet(cur -> Math.max(cur, nextId));
    }

    private long id;
    private long fromUserId;
    private long toUserId;
    private Status status;

    public FriendRequest(long fromUserId, long toUserId) {
        this.id = ID_GEN.getAndIncrement();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = Status.PENDING;
    }
}

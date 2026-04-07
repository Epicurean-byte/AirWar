package com.planewar.server.model.entity;

import java.util.concurrent.atomic.AtomicLong;

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

    public FriendRequest() {
    }

    public FriendRequest(long fromUserId, long toUserId) {
        this.id = ID_GEN.getAndIncrement();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = Status.PENDING;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public long getToUserId() {
        return toUserId;
    }

    public void setToUserId(long toUserId) {
        this.toUserId = toUserId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

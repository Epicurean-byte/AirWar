package edu.hitsz.aircraftwar.android.network.model;

public final class FriendRequestItem {
    private final long requestId;
    private final long fromUserId;
    private final String fromNickname;
    private final String fromUsername;

    public FriendRequestItem(long requestId, long fromUserId, String fromNickname, String fromUsername) {
        this.requestId = requestId;
        this.fromUserId = fromUserId;
        this.fromNickname = fromNickname;
        this.fromUsername = fromUsername;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getFromUserId() {
        return fromUserId;
    }

    public String getFromNickname() {
        return fromNickname;
    }

    public String getFromUsername() {
        return fromUsername;
    }
}

package edu.hitsz.aircraftwar.android.network;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class WsGameClient {

    public interface Listener {
        void onOpen();

        void onMessage(String type, long roomId, long userId, @Nullable JSONObject payload);

        void onError(String message);

        void onClosed();
    }

    private final OkHttpClient client = new OkHttpClient();
    private WebSocket ws;
    private long userId;
    private Listener listener;

    public void connect(long userId, Listener listener) {
        this.userId = userId;
        this.listener = listener;
        Request req = new Request.Builder().url(ServerConfig.WS_GAME_URL).build();
        ws = client.newWebSocket(req, new InnerListener());
    }

    public void close() {
        if (ws != null) {
            ws.close(1000, "bye");
            ws = null;
        }
    }

    public void send(String type, long roomId, @Nullable JSONObject payload) {
        if (ws == null) {
            return;
        }
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", type);
            msg.put("roomId", roomId);
            msg.put("userId", userId);
            msg.put("payload", payload == null ? "{}" : payload.toString());
            ws.send(msg.toString());
        } catch (Exception e) {
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }

    private final class InnerListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            send("AUTH", 0, null);
            if (listener != null) {
                listener.onOpen();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (listener == null) {
                return;
            }
            try {
                JSONObject root = new JSONObject(text);
                String type = root.optString("type", "");
                long roomId = root.optLong("roomId", 0L);
                long fromUserId = root.optLong("userId", 0L);
                JSONObject payload = null;
                String payloadRaw = root.optString("payload", "{}");
                if (payloadRaw != null && !payloadRaw.isBlank()) {
                    payload = new JSONObject(payloadRaw);
                }
                listener.onMessage(type, roomId, fromUserId, payload);
            } catch (Exception e) {
                listener.onError("WS parse error: " + e.getMessage());
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            if (listener != null) {
                listener.onError(t.getMessage() == null ? "WebSocket failed" : t.getMessage());
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            if (listener != null) {
                listener.onClosed();
            }
        }
    }
}

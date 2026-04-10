package edu.hitsz.aircraftwar.android.network;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class WsGameClient implements ServerConfigManager.ServerAddressChangeListener {

    public interface Listener {
        void onOpen();

        void onMessage(String type, long roomId, long userId, @Nullable JSONObject payload);

        void onError(String message);

        void onClosed();
    }

    private final OkHttpClient client = new OkHttpClient();
    private final ServerConfigManager configManager = ServerConfigManager.getInstance();
    private WebSocket ws;
    private long userId;
    private Listener listener;

    public WsGameClient() {
        // Register to listen for server address changes
        configManager.addServerAddressChangeListener(this);
    }

    public void connect(long userId, Listener listener) {
        this.userId = userId;
        this.listener = listener;
        String wsUrl = configManager.getWsGameUrl();
        Request req = new Request.Builder().url(wsUrl).build();
        ws = client.newWebSocket(req, new InnerListener(wsUrl));
    }

    public void close() {
        if (ws != null) {
            ws.close(1000, "bye");
            ws = null;
        }
    }
    
    /**
     * Cleanup method to unregister from server address change notifications.
     * Should be called when this client is no longer needed.
     */
    public void cleanup() {
        close();
        configManager.removeServerAddressChangeListener(this);
    }
    
    @Override
    public void onServerAddressChanged() {
        // Disconnect existing WebSocket connection when server address changes
        // The client will need to reconnect manually with the new address
        if (ws != null) {
            close();
            if (listener != null) {
                listener.onError("Server address changed, connection closed");
            }
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
        private final String serverUrl;
        
        InnerListener(String serverUrl) {
            this.serverUrl = serverUrl;
        }
        
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
                String errorMsg = buildConnectionErrorMessage(t);
                listener.onError(errorMsg);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            if (listener != null) {
                listener.onClosed();
            }
        }
        
        private String buildConnectionErrorMessage(Throwable t) {
            String baseMsg = t.getMessage() != null ? t.getMessage() : "WebSocket connection failed";
            
            // Check for common connection issues
            if (t instanceof java.net.SocketTimeoutException) {
                return "Connection timeout to " + serverUrl + ". Please check if the server is running.";
            } else if (t instanceof java.net.ConnectException) {
                return "Cannot connect to " + serverUrl + ". Please verify the server address and network connection.";
            } else if (t instanceof java.net.UnknownHostException) {
                return "Unknown host: " + serverUrl + ". Please check the server IP address.";
            } else if (baseMsg.toLowerCase().contains("timeout")) {
                return "Connection timeout to " + serverUrl + ". Please check if the server is running.";
            } else if (baseMsg.toLowerCase().contains("refused")) {
                return "Connection refused by " + serverUrl + ". Please verify the server is running and the port is correct.";
            }
            
            // Default error message with server address
            return "Failed to connect to " + serverUrl + ": " + baseMsg;
        }
    }
}

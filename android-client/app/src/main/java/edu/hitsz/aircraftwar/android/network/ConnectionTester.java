package edu.hitsz.aircraftwar.android.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Tests connectivity to the configured game server.
 * Provides health check functionality with detailed error reporting.
 */
public class ConnectionTester {
    
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    
    /**
     * Result of a connection test.
     */
    public static class ConnectionResult {
        private final boolean success;
        private final String message;
        private final ErrorType errorType;
        
        public ConnectionResult(boolean success, String message, ErrorType errorType) {
            this.success = success;
            this.message = message;
            this.errorType = errorType;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public ErrorType getErrorType() {
            return errorType;
        }
        
        @NonNull
        @Override
        public String toString() {
            return "ConnectionResult{success=" + success + ", message='" + message + "', errorType=" + errorType + "}";
        }
    }
    
    /**
     * Types of connection errors.
     */
    public enum ErrorType {
        NONE,
        NETWORK_UNREACHABLE,
        CONNECTION_TIMEOUT,
        WRONG_PORT,
        UNKNOWN
    }
    
    /**
     * Callback interface for async connection test results.
     */
    public interface ConnectionTestCallback {
        void onResult(ConnectionResult result);
    }
    
    private final OkHttpClient client;
    
    public ConnectionTester() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
    }
    
    /**
     * Tests connection to the server asynchronously.
     * Uses the current server configuration from ServerConfigManager.
     * 
     * @param callback callback to receive the test result
     */
    public void testConnection(ConnectionTestCallback callback) {
        String serverIp = ServerConfigManager.getInstance().getServerIp();
        String serverPort = ServerConfigManager.getInstance().getServerPort();
        testConnection(serverIp, serverPort, callback);
    }
    
    /**
     * Tests connection to a specific server address asynchronously.
     * 
     * @param serverIp the server IP address to test
     * @param serverPort the server port to test
     * @param callback callback to receive the test result
     */
    public void testConnection(String serverIp, String serverPort, ConnectionTestCallback callback) {
        NetworkExecutor.run(() -> {
            ConnectionResult result = testConnectionSync(serverIp, serverPort);
            callback.onResult(result);
        });
    }
    
    /**
     * Tests connection to the server synchronously.
     * This method blocks until the test completes.
     * 
     * @param serverIp the server IP address to test
     * @param serverPort the server port to test
     * @return the connection test result
     */
    public ConnectionResult testConnectionSync(String serverIp, String serverPort) {
        String url = "http://" + serverIp + ":" + serverPort + "/api/leaderboard/score";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return new ConnectionResult(
                        true,
                        "Connection successful",
                        ErrorType.NONE
                );
            } else {
                // Server responded but with an error code
                return new ConnectionResult(
                        false,
                        "Server port is incorrect",
                        ErrorType.WRONG_PORT
                );
            }
        } catch (UnknownHostException e) {
            return new ConnectionResult(
                    false,
                    "Cannot connect to server, please check IP address and network",
                    ErrorType.NETWORK_UNREACHABLE
            );
        } catch (SocketTimeoutException e) {
            return new ConnectionResult(
                    false,
                    "Connection timeout, please check server address and network",
                    ErrorType.CONNECTION_TIMEOUT
            );
        } catch (ConnectException e) {
            // Connection refused - typically means wrong port or server not running
            String message = e.getMessage();
            if (message != null && (message.contains("Connection refused") || message.contains("failed to connect"))) {
                return new ConnectionResult(
                        false,
                        "Server port is incorrect",
                        ErrorType.WRONG_PORT
                );
            }
            return new ConnectionResult(
                    false,
                    "Cannot connect to server, please check IP address and network",
                    ErrorType.NETWORK_UNREACHABLE
            );
        } catch (IOException e) {
            return new ConnectionResult(
                    false,
                    "Network error: " + e.getMessage(),
                    ErrorType.UNKNOWN
            );
        } catch (Exception e) {
            return new ConnectionResult(
                    false,
                    "Unknown error: " + e.getMessage(),
                    ErrorType.UNKNOWN
            );
        }
    }
}

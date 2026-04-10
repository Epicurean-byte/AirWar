package edu.hitsz.aircraftwar.android.network;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class for managing dynamic server configuration.
 * Replaces the static ServerConfig to allow runtime server address changes.
 * 
 * This class provides methods to get/set server IP and port, and builds
 * HTTP and WebSocket URLs dynamically from the stored values.
 * Configuration is persisted using ServerConfigStorage.
 */
public class ServerConfigManager {
    
    /**
     * Listener interface for server address changes.
     */
    public interface ServerAddressChangeListener {
        /**
         * Called when the server address (IP or port) changes.
         */
        void onServerAddressChanged();
    }
    
    private static ServerConfigManager instance;
    
    // Default values for Android emulator connecting to host machine
    private static final String DEFAULT_SERVER_IP = "10.0.2.2";
    private static final String DEFAULT_SERVER_PORT = "18080";
    
    private String serverIp;
    private String serverPort;
    private ServerConfigStorage storage;
    private final List<ServerAddressChangeListener> listeners = new ArrayList<>();
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private ServerConfigManager() {
        this.serverIp = DEFAULT_SERVER_IP;
        this.serverPort = DEFAULT_SERVER_PORT;
    }
    
    /**
     * Initializes the ServerConfigManager with Android context.
     * Loads saved configuration from storage if available.
     * This method should be called once during application startup.
     * 
     * @param context the Android context for accessing storage
     */
    public void initialize(Context context) {
        if (storage == null) {
            storage = new ServerConfigStorage(context);
            // Load saved configuration
            this.serverIp = storage.loadServerIp();
            this.serverPort = storage.loadServerPort();
        }
    }
    
    /**
     * Gets the singleton instance of ServerConfigManager.
     * 
     * @return the singleton instance
     */
    public static synchronized ServerConfigManager getInstance() {
        if (instance == null) {
            instance = new ServerConfigManager();
        }
        return instance;
    }
    
    /**
     * Gets the current server IP address.
     * 
     * @return the server IP address
     */
    public String getServerIp() {
        return serverIp;
    }
    
    /**
     * Sets the server IP address.
     * Saves to persistent storage and notifies listeners if the address actually changed.
     * 
     * @param serverIp the new server IP address
     */
    public void setServerIp(String serverIp) {
        if (serverIp != null && !serverIp.trim().isEmpty()) {
            String newIp = serverIp.trim();
            if (!newIp.equals(this.serverIp)) {
                this.serverIp = newIp;
                if (storage != null) {
                    storage.saveServerIp(newIp);
                }
                notifyListeners();
            }
        }
    }
    
    /**
     * Gets the current server port.
     * 
     * @return the server port as a string
     */
    public String getServerPort() {
        return serverPort;
    }
    
    /**
     * Sets the server port.
     * Saves to persistent storage and notifies listeners if the port actually changed.
     * 
     * @param serverPort the new server port as a string
     */
    public void setServerPort(String serverPort) {
        if (serverPort != null && !serverPort.trim().isEmpty()) {
            String newPort = serverPort.trim();
            if (!newPort.equals(this.serverPort)) {
                this.serverPort = newPort;
                if (storage != null) {
                    storage.saveServerPort(newPort);
                }
                notifyListeners();
            }
        }
    }
    
    /**
     * Sets both server IP and port in a single call.
     * Saves to persistent storage and notifies listeners only once if either value changed.
     * 
     * @param serverIp the new server IP address
     * @param serverPort the new server port as a string
     */
    public void setServerAddress(String serverIp, String serverPort) {
        boolean changed = false;
        
        if (serverIp != null && !serverIp.trim().isEmpty()) {
            String newIp = serverIp.trim();
            if (!newIp.equals(this.serverIp)) {
                this.serverIp = newIp;
                changed = true;
            }
        }
        
        if (serverPort != null && !serverPort.trim().isEmpty()) {
            String newPort = serverPort.trim();
            if (!newPort.equals(this.serverPort)) {
                this.serverPort = newPort;
                changed = true;
            }
        }
        
        if (changed) {
            if (storage != null) {
                storage.saveServerAddress(this.serverIp, this.serverPort);
            }
            notifyListeners();
        }
    }
    
    /**
     * Builds and returns the HTTP base URL using current server configuration.
     * Format: http://[ip]:[port]
     * 
     * @return the HTTP base URL
     */
    public String getHttpBaseUrl() {
        return "http://" + serverIp + ":" + serverPort;
    }
    
    /**
     * Builds and returns the WebSocket game URL using current server configuration.
     * Format: ws://[ip]:[port]/ws/game
     * 
     * @return the WebSocket game URL
     */
    public String getWsGameUrl() {
        return "ws://" + serverIp + ":" + serverPort + "/ws/game";
    }
    
    /**
     * Resets the server configuration to default values.
     */
    public void resetToDefaults() {
        this.serverIp = DEFAULT_SERVER_IP;
        this.serverPort = DEFAULT_SERVER_PORT;
    }
    
    /**
     * Gets the default server IP address.
     * 
     * @return the default server IP
     */
    public static String getDefaultServerIp() {
        return DEFAULT_SERVER_IP;
    }
    
    /**
     * Gets the default server port.
     * 
     * @return the default server port
     */
    public static String getDefaultServerPort() {
        return DEFAULT_SERVER_PORT;
    }
    
    /**
     * Adds a listener to be notified when the server address changes.
     * 
     * @param listener the listener to add
     */
    public void addServerAddressChangeListener(ServerAddressChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a previously added listener.
     * 
     * @param listener the listener to remove
     */
    public void removeServerAddressChangeListener(ServerAddressChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all registered listeners that the server address has changed.
     */
    private void notifyListeners() {
        for (ServerAddressChangeListener listener : listeners) {
            listener.onServerAddressChanged();
        }
    }
}

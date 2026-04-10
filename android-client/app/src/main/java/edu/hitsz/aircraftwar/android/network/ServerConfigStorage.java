package edu.hitsz.aircraftwar.android.network;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Storage layer for server configuration using Android SharedPreferences.
 * Provides persistent storage for server IP address and port.
 * 
 * This class handles saving and loading server configuration separately
 * for IP and port, with default values for Android emulator connecting
 * to host machine.
 */
public class ServerConfigStorage {
    
    private static final String PREFS_NAME = "server_config";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    
    // Default values for Android emulator connecting to host machine
    private static final String DEFAULT_SERVER_IP = "10.0.2.2";
    private static final String DEFAULT_SERVER_PORT = "18080";
    
    private final SharedPreferences preferences;
    
    /**
     * Creates a new ServerConfigStorage instance.
     * 
     * @param context the Android context used to access SharedPreferences
     */
    public ServerConfigStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Saves the server IP address to persistent storage.
     * 
     * @param serverIp the server IP address to save
     */
    public void saveServerIp(String serverIp) {
        preferences.edit()
                .putString(KEY_SERVER_IP, serverIp)
                .apply();
    }
    
    /**
     * Saves the server port to persistent storage.
     * 
     * @param serverPort the server port to save
     */
    public void saveServerPort(String serverPort) {
        preferences.edit()
                .putString(KEY_SERVER_PORT, serverPort)
                .apply();
    }
    
    /**
     * Saves both server IP and port to persistent storage in a single transaction.
     * 
     * @param serverIp the server IP address to save
     * @param serverPort the server port to save
     */
    public void saveServerAddress(String serverIp, String serverPort) {
        preferences.edit()
                .putString(KEY_SERVER_IP, serverIp)
                .putString(KEY_SERVER_PORT, serverPort)
                .apply();
    }
    
    /**
     * Loads the server IP address from persistent storage.
     * Returns the default value if no saved configuration exists.
     * 
     * @return the saved server IP address, or default value "10.0.2.2"
     */
    public String loadServerIp() {
        return preferences.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP);
    }
    
    /**
     * Loads the server port from persistent storage.
     * Returns the default value if no saved configuration exists.
     * 
     * @return the saved server port, or default value "18080"
     */
    public String loadServerPort() {
        return preferences.getString(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
    }
    
    /**
     * Checks if server configuration exists in persistent storage.
     * 
     * @return true if either IP or port has been saved, false otherwise
     */
    public boolean hasStoredConfig() {
        return preferences.contains(KEY_SERVER_IP) || preferences.contains(KEY_SERVER_PORT);
    }
    
    /**
     * Clears all saved server configuration from persistent storage.
     */
    public void clearConfig() {
        preferences.edit()
                .remove(KEY_SERVER_IP)
                .remove(KEY_SERVER_PORT)
                .apply();
    }
    
    /**
     * Gets the default server IP address.
     * 
     * @return the default server IP "10.0.2.2"
     */
    public static String getDefaultServerIp() {
        return DEFAULT_SERVER_IP;
    }
    
    /**
     * Gets the default server port.
     * 
     * @return the default server port "18080"
     */
    public static String getDefaultServerPort() {
        return DEFAULT_SERVER_PORT;
    }
}

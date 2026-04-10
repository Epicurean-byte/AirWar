package edu.hitsz.aircraftwar.android.network;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ServerConfigManager.
 */
public class ServerConfigManagerTest {
    
    private ServerConfigManager configManager;
    
    @Before
    public void setUp() {
        configManager = ServerConfigManager.getInstance();
        // Reset to defaults before each test
        configManager.resetToDefaults();
    }
    
    @Test
    public void testSingletonInstance() {
        ServerConfigManager instance1 = ServerConfigManager.getInstance();
        ServerConfigManager instance2 = ServerConfigManager.getInstance();
        assertSame("Should return the same instance", instance1, instance2);
    }
    
    @Test
    public void testDefaultValues() {
        assertEquals("Default IP should be 10.0.2.2", "10.0.2.2", configManager.getServerIp());
        assertEquals("Default port should be 18080", "18080", configManager.getServerPort());
    }
    
    @Test
    public void testSetServerIp() {
        configManager.setServerIp("192.168.1.100");
        assertEquals("Server IP should be updated", "192.168.1.100", configManager.getServerIp());
    }
    
    @Test
    public void testSetServerPort() {
        configManager.setServerPort("8080");
        assertEquals("Server port should be updated", "8080", configManager.getServerPort());
    }
    
    @Test
    public void testSetServerAddress() {
        configManager.setServerAddress("192.168.1.50", "9090");
        assertEquals("Server IP should be updated", "192.168.1.50", configManager.getServerIp());
        assertEquals("Server port should be updated", "9090", configManager.getServerPort());
    }
    
    @Test
    public void testSetServerIpWithWhitespace() {
        configManager.setServerIp("  192.168.1.100  ");
        assertEquals("Server IP should be trimmed", "192.168.1.100", configManager.getServerIp());
    }
    
    @Test
    public void testSetServerPortWithWhitespace() {
        configManager.setServerPort("  8080  ");
        assertEquals("Server port should be trimmed", "8080", configManager.getServerPort());
    }
    
    @Test
    public void testSetServerIpWithNull() {
        String originalIp = configManager.getServerIp();
        configManager.setServerIp(null);
        assertEquals("Server IP should not change when null is provided", originalIp, configManager.getServerIp());
    }
    
    @Test
    public void testSetServerIpWithEmptyString() {
        String originalIp = configManager.getServerIp();
        configManager.setServerIp("");
        assertEquals("Server IP should not change when empty string is provided", originalIp, configManager.getServerIp());
    }
    
    @Test
    public void testSetServerPortWithNull() {
        String originalPort = configManager.getServerPort();
        configManager.setServerPort(null);
        assertEquals("Server port should not change when null is provided", originalPort, configManager.getServerPort());
    }
    
    @Test
    public void testSetServerPortWithEmptyString() {
        String originalPort = configManager.getServerPort();
        configManager.setServerPort("");
        assertEquals("Server port should not change when empty string is provided", originalPort, configManager.getServerPort());
    }
    
    @Test
    public void testGetHttpBaseUrl() {
        configManager.setServerAddress("192.168.1.100", "8080");
        assertEquals("HTTP base URL should be correctly formatted", 
                     "http://192.168.1.100:8080", 
                     configManager.getHttpBaseUrl());
    }
    
    @Test
    public void testGetHttpBaseUrlWithDefaults() {
        assertEquals("HTTP base URL with defaults should be correct", 
                     "http://10.0.2.2:18080", 
                     configManager.getHttpBaseUrl());
    }
    
    @Test
    public void testGetWsGameUrl() {
        configManager.setServerAddress("192.168.1.100", "8080");
        assertEquals("WebSocket game URL should be correctly formatted", 
                     "ws://192.168.1.100:8080/ws/game", 
                     configManager.getWsGameUrl());
    }
    
    @Test
    public void testGetWsGameUrlWithDefaults() {
        assertEquals("WebSocket game URL with defaults should be correct", 
                     "ws://10.0.2.2:18080/ws/game", 
                     configManager.getWsGameUrl());
    }
    
    @Test
    public void testResetToDefaults() {
        configManager.setServerAddress("192.168.1.100", "8080");
        configManager.resetToDefaults();
        assertEquals("Server IP should be reset to default", "10.0.2.2", configManager.getServerIp());
        assertEquals("Server port should be reset to default", "18080", configManager.getServerPort());
    }
    
    @Test
    public void testGetDefaultServerIp() {
        assertEquals("Default server IP should be 10.0.2.2", "10.0.2.2", ServerConfigManager.getDefaultServerIp());
    }
    
    @Test
    public void testGetDefaultServerPort() {
        assertEquals("Default server port should be 18080", "18080", ServerConfigManager.getDefaultServerPort());
    }
    
    @Test
    public void testUrlsUpdateDynamically() {
        // Set initial values
        configManager.setServerAddress("192.168.1.100", "8080");
        String httpUrl1 = configManager.getHttpBaseUrl();
        String wsUrl1 = configManager.getWsGameUrl();
        
        // Change values
        configManager.setServerAddress("192.168.1.200", "9090");
        String httpUrl2 = configManager.getHttpBaseUrl();
        String wsUrl2 = configManager.getWsGameUrl();
        
        // Verify URLs are different
        assertNotEquals("HTTP URL should change when server address changes", httpUrl1, httpUrl2);
        assertNotEquals("WebSocket URL should change when server address changes", wsUrl1, wsUrl2);
        
        // Verify new URLs are correct
        assertEquals("New HTTP URL should be correct", "http://192.168.1.200:9090", httpUrl2);
        assertEquals("New WebSocket URL should be correct", "ws://192.168.1.200:9090/ws/game", wsUrl2);
    }
    
    @Test
    public void testListenerNotifiedOnIpChange() {
        final boolean[] notified = {false};
        ServerConfigManager.ServerAddressChangeListener listener = () -> notified[0] = true;
        
        configManager.addServerAddressChangeListener(listener);
        configManager.setServerIp("192.168.1.100");
        
        assertTrue("Listener should be notified when IP changes", notified[0]);
        configManager.removeServerAddressChangeListener(listener);
    }
    
    @Test
    public void testListenerNotifiedOnPortChange() {
        final boolean[] notified = {false};
        ServerConfigManager.ServerAddressChangeListener listener = () -> notified[0] = true;
        
        configManager.addServerAddressChangeListener(listener);
        configManager.setServerPort("8080");
        
        assertTrue("Listener should be notified when port changes", notified[0]);
        configManager.removeServerAddressChangeListener(listener);
    }
    
    @Test
    public void testListenerNotifiedOnAddressChange() {
        final int[] notificationCount = {0};
        ServerConfigManager.ServerAddressChangeListener listener = () -> notificationCount[0]++;
        
        configManager.addServerAddressChangeListener(listener);
        configManager.setServerAddress("192.168.1.100", "8080");
        
        assertEquals("Listener should be notified once when both IP and port change", 1, notificationCount[0]);
        configManager.removeServerAddressChangeListener(listener);
    }
    
    @Test
    public void testListenerNotNotifiedWhenValueUnchanged() {
        final boolean[] notified = {false};
        ServerConfigManager.ServerAddressChangeListener listener = () -> notified[0] = true;
        
        String currentIp = configManager.getServerIp();
        configManager.addServerAddressChangeListener(listener);
        configManager.setServerIp(currentIp);
        
        assertFalse("Listener should not be notified when IP doesn't change", notified[0]);
        configManager.removeServerAddressChangeListener(listener);
    }
    
    @Test
    public void testMultipleListeners() {
        final int[] count1 = {0};
        final int[] count2 = {0};
        
        ServerConfigManager.ServerAddressChangeListener listener1 = () -> count1[0]++;
        ServerConfigManager.ServerAddressChangeListener listener2 = () -> count2[0]++;
        
        configManager.addServerAddressChangeListener(listener1);
        configManager.addServerAddressChangeListener(listener2);
        
        configManager.setServerIp("192.168.1.100");
        
        assertEquals("First listener should be notified", 1, count1[0]);
        assertEquals("Second listener should be notified", 1, count2[0]);
        
        configManager.removeServerAddressChangeListener(listener1);
        configManager.removeServerAddressChangeListener(listener2);
    }
    
    @Test
    public void testRemoveListener() {
        final boolean[] notified = {false};
        ServerConfigManager.ServerAddressChangeListener listener = () -> notified[0] = true;
        
        configManager.addServerAddressChangeListener(listener);
        configManager.removeServerAddressChangeListener(listener);
        configManager.setServerIp("192.168.1.100");
        
        assertFalse("Removed listener should not be notified", notified[0]);
    }
    
    @Test
    public void testAddNullListener() {
        // Should not throw exception
        configManager.addServerAddressChangeListener(null);
        configManager.setServerIp("192.168.1.100");
        // If we get here without exception, test passes
    }
    
    @Test
    public void testAddDuplicateListener() {
        final int[] count = {0};
        ServerConfigManager.ServerAddressChangeListener listener = () -> count[0]++;
        
        configManager.addServerAddressChangeListener(listener);
        configManager.addServerAddressChangeListener(listener); // Add same listener twice
        
        configManager.setServerIp("192.168.1.100");
        
        assertEquals("Duplicate listener should only be notified once", 1, count[0]);
        configManager.removeServerAddressChangeListener(listener);
    }
}

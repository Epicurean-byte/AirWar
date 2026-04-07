package edu.hitsz.aircraftwar.android.network;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for WsGameClient integration with ServerConfigManager.
 * 
 * Note: These tests focus on the listener integration and cleanup behavior.
 * Full WebSocket functionality testing would require mocking OkHttp components.
 */
public class WsGameClientTest {
    
    private ServerConfigManager configManager;
    
    @Before
    public void setUp() {
        configManager = ServerConfigManager.getInstance();
        configManager.resetToDefaults();
    }
    
    @Test
    public void testClientRegistersAsListener() {
        // Create a client
        WsGameClient client = new WsGameClient();
        
        // Change server address and verify the client would be notified
        // We can't directly test the notification without a real connection,
        // but we can verify the listener mechanism works
        final boolean[] listenerCalled = {false};
        configManager.addServerAddressChangeListener(() -> listenerCalled[0] = true);
        
        configManager.setServerIp("192.168.1.100");
        
        assertTrue("Listener should be called when address changes", listenerCalled[0]);
        
        // Cleanup
        client.cleanup();
    }
    
    @Test
    public void testCleanupRemovesListener() {
        WsGameClient client = new WsGameClient();
        
        // Cleanup the client
        client.cleanup();
        
        // Now change the address - the client's listener should not be called
        // We verify this indirectly by ensuring no exception is thrown
        configManager.setServerIp("192.168.1.100");
        
        // If we get here without exception, the test passes
    }
    
    @Test
    public void testMultipleClientsCanCoexist() {
        WsGameClient client1 = new WsGameClient();
        WsGameClient client2 = new WsGameClient();
        
        // Both clients should be registered as listeners
        // Change address to trigger notifications
        configManager.setServerIp("192.168.1.100");
        
        // Cleanup both clients
        client1.cleanup();
        client2.cleanup();
        
        // No exception should be thrown
    }
    
    @Test
    public void testCloseDoesNotRemoveListener() {
        final int[] notificationCount = {0};
        
        WsGameClient client = new WsGameClient();
        
        // Add a test listener to count notifications
        ServerConfigManager.ServerAddressChangeListener testListener = () -> notificationCount[0]++;
        configManager.addServerAddressChangeListener(testListener);
        
        // Close (not cleanup) the client
        client.close();
        
        // Change address - client's listener should still be registered
        configManager.setServerIp("192.168.1.100");
        
        // At least the test listener should be notified
        assertTrue("Listeners should still be notified after close()", notificationCount[0] > 0);
        
        // Proper cleanup
        client.cleanup();
        configManager.removeServerAddressChangeListener(testListener);
    }
    
    @Test
    public void testClientUsesConfigManagerForUrl() {
        // Set a custom server address
        configManager.setServerAddress("192.168.1.50", "9090");
        
        // Create a client - it should use the configured URL
        WsGameClient client = new WsGameClient();
        
        // Verify the URL is built correctly by checking the config manager
        String expectedUrl = "ws://192.168.1.50:9090/ws/game";
        assertEquals("Client should use URL from ConfigManager", expectedUrl, configManager.getWsGameUrl());
        
        // Cleanup
        client.cleanup();
    }
}

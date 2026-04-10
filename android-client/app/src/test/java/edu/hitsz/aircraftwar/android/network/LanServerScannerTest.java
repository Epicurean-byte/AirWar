package edu.hitsz.aircraftwar.android.network;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for LanServerScanner.
 * Tests subnet calculation and basic functionality.
 */
public class LanServerScannerTest {
    
    private LanServerScanner scanner;
    
    @Before
    public void setUp() {
        scanner = new LanServerScanner();
    }
    
    @Test
    public void testDiscoveredServerCreation() {
        LanServerScanner.DiscoveredServer server = 
            new LanServerScanner.DiscoveredServer("192.168.1.100", 50);
        
        assertEquals("IP address should match", "192.168.1.100", server.getIpAddress());
        assertEquals("Response time should match", 50, server.getResponseTimeMs());
    }
    
    @Test
    public void testDiscoveredServerToString() {
        LanServerScanner.DiscoveredServer server = 
            new LanServerScanner.DiscoveredServer("192.168.1.100", 50);
        
        String result = server.toString();
        assertTrue("toString should contain IP", result.contains("192.168.1.100"));
        assertTrue("toString should contain response time", result.contains("50"));
        assertTrue("toString should contain 'ms'", result.contains("ms"));
    }
    
    @Test
    public void testGetSubnetReflection() throws Exception {
        // Use reflection to test private getSubnet method
        Method getSubnetMethod = LanServerScanner.class.getDeclaredMethod("getSubnet", String.class);
        getSubnetMethod.setAccessible(true);
        
        // Test valid IP addresses
        assertEquals("Should extract subnet from 192.168.1.100", 
                     "192.168.1", 
                     getSubnetMethod.invoke(scanner, "192.168.1.100"));
        
        assertEquals("Should extract subnet from 10.0.2.2", 
                     "10.0.2", 
                     getSubnetMethod.invoke(scanner, "10.0.2.2"));
        
        assertEquals("Should extract subnet from 172.16.0.1", 
                     "172.16.0", 
                     getSubnetMethod.invoke(scanner, "172.16.0.1"));
    }
    
    @Test
    public void testGetSubnetInvalidInput() throws Exception {
        // Use reflection to test private getSubnet method
        Method getSubnetMethod = LanServerScanner.class.getDeclaredMethod("getSubnet", String.class);
        getSubnetMethod.setAccessible(true);
        
        // Test invalid inputs
        assertNull("Should return null for IP without dots", 
                   getSubnetMethod.invoke(scanner, "192168100"));
        
        assertNull("Should return null for single number", 
                   getSubnetMethod.invoke(scanner, "192"));
    }
    
    @Test
    public void testScanCallbackInterface() {
        // Test that callback interface can be implemented
        final boolean[] progressCalled = {false};
        final boolean[] completeCalled = {false};
        
        LanServerScanner.ScanCallback callback = new LanServerScanner.ScanCallback() {
            @Override
            public void onProgress(int scannedCount, int totalCount) {
                progressCalled[0] = true;
                assertTrue("Scanned count should be positive", scannedCount >= 0);
                assertTrue("Total count should be positive", totalCount > 0);
                assertTrue("Scanned count should not exceed total", scannedCount <= totalCount);
            }
            
            @Override
            public void onComplete(List<LanServerScanner.DiscoveredServer> servers) {
                completeCalled[0] = true;
                assertNotNull("Server list should not be null", servers);
            }
        };
        
        // Simulate callback calls
        callback.onProgress(10, 254);
        callback.onComplete(new java.util.ArrayList<>());
        
        assertTrue("Progress callback should be called", progressCalled[0]);
        assertTrue("Complete callback should be called", completeCalled[0]);
    }
}

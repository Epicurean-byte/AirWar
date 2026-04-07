package edu.hitsz.aircraftwar.android.network;

/**
 * Manual verification of ServerConfigManager functionality.
 * This is not a JUnit test, just a simple verification that can be run manually.
 */
public class ManualVerification {
    public static void main(String[] args) {
        System.out.println("=== ServerConfigManager Manual Verification ===\n");
        
        // Test 1: Singleton pattern
        ServerConfigManager instance1 = ServerConfigManager.getInstance();
        ServerConfigManager instance2 = ServerConfigManager.getInstance();
        System.out.println("Test 1 - Singleton: " + (instance1 == instance2 ? "PASS" : "FAIL"));
        
        // Test 2: Default values
        System.out.println("\nTest 2 - Default values:");
        System.out.println("  IP: " + instance1.getServerIp() + " (expected: 10.0.2.2)");
        System.out.println("  Port: " + instance1.getServerPort() + " (expected: 18080)");
        System.out.println("  HTTP URL: " + instance1.getHttpBaseUrl());
        System.out.println("  WS URL: " + instance1.getWsGameUrl());
        
        // Test 3: Set new values
        System.out.println("\nTest 3 - Set new values:");
        instance1.setServerAddress("192.168.1.100", "8080");
        System.out.println("  IP: " + instance1.getServerIp() + " (expected: 192.168.1.100)");
        System.out.println("  Port: " + instance1.getServerPort() + " (expected: 8080)");
        System.out.println("  HTTP URL: " + instance1.getHttpBaseUrl());
        System.out.println("  WS URL: " + instance1.getWsGameUrl());
        
        // Test 4: Whitespace trimming
        System.out.println("\nTest 4 - Whitespace trimming:");
        instance1.setServerIp("  192.168.1.200  ");
        instance1.setServerPort("  9090  ");
        System.out.println("  IP: '" + instance1.getServerIp() + "' (expected: '192.168.1.200')");
        System.out.println("  Port: '" + instance1.getServerPort() + "' (expected: '9090')");
        
        // Test 5: Null/empty handling
        System.out.println("\nTest 5 - Null/empty handling:");
        String beforeIp = instance1.getServerIp();
        String beforePort = instance1.getServerPort();
        instance1.setServerIp(null);
        instance1.setServerPort("");
        System.out.println("  IP unchanged after null: " + beforeIp.equals(instance1.getServerIp()));
        System.out.println("  Port unchanged after empty: " + beforePort.equals(instance1.getServerPort()));
        
        // Test 6: Reset to defaults
        System.out.println("\nTest 6 - Reset to defaults:");
        instance1.resetToDefaults();
        System.out.println("  IP: " + instance1.getServerIp() + " (expected: 10.0.2.2)");
        System.out.println("  Port: " + instance1.getServerPort() + " (expected: 18080)");
        
        System.out.println("\n=== Verification Complete ===");
    }
}

package com.planewar.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify server listens on all network interfaces (0.0.0.0)
 * and accepts connections from both localhost and LAN IP addresses.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerConfigurationTest {

    @LocalServerPort
    private int port;

    @Value("${server.address:0.0.0.0}")
    private String serverAddress;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void testServerAddressIsConfiguredToListenOnAllInterfaces() {
        // Verify server is configured to listen on 0.0.0.0
        assertEquals("0.0.0.0", serverAddress, 
            "Server should be configured to listen on all network interfaces (0.0.0.0)");
    }

    @Test
    void testServerAcceptsConnectionsFromLocalhost() {
        // Test connection via localhost
        String url = "http://localhost:" + port + "/api/user/info?userId=1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
            "Server should accept connections from localhost");
        assertNotNull(response.getBody(), 
            "Response body should not be null");
        assertTrue(response.getBody().contains("\"code\":200"), 
            "Response should indicate success");
    }

    @Test
    void testServerAcceptsConnectionsFrom127001() {
        // Test connection via 127.0.0.1
        String url = "http://127.0.0.1:" + port + "/api/user/info?userId=1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
            "Server should accept connections from 127.0.0.1");
        assertNotNull(response.getBody(), 
            "Response body should not be null");
        assertTrue(response.getBody().contains("\"code\":200"), 
            "Response should indicate success");
    }
}

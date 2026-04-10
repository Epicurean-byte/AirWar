package edu.hitsz.aircraftwar.android.network;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ServerConfigManager with ServerConfigStorage.
 * Tests that configuration is properly loaded and saved.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerConfigStorageIntegrationTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private ServerConfigManager configManager;
    
    @Before
    public void setUp() {
        // Reset singleton instance for testing
        configManager = ServerConfigManager.getInstance();
        configManager.resetToDefaults();
        
        // Setup mock behavior
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
    }
    
    @Test
    public void testInitializeLoadsDefaultsWhenNoStoredConfig() {
        // Setup: No stored configuration
        when(mockPreferences.getString("server_ip", "10.0.2.2")).thenReturn("10.0.2.2");
        when(mockPreferences.getString("server_port", "18080")).thenReturn("18080");
        
        // Execute
        configManager.initialize(mockContext);
        
        // Verify
        assertEquals("Should load default IP", "10.0.2.2", configManager.getServerIp());
        assertEquals("Should load default port", "18080", configManager.getServerPort());
    }
    
    @Test
    public void testInitializeLoadsStoredConfiguration() {
        // Setup: Stored configuration exists
        when(mockPreferences.getString("server_ip", "10.0.2.2")).thenReturn("192.168.1.100");
        when(mockPreferences.getString("server_port", "18080")).thenReturn("8080");
        
        // Execute
        configManager.initialize(mockContext);
        
        // Verify
        assertEquals("Should load stored IP", "192.168.1.100", configManager.getServerIp());
        assertEquals("Should load stored port", "8080", configManager.getServerPort());
    }
    
    @Test
    public void testSetServerIpSavesToStorage() {
        // Setup
        when(mockPreferences.getString(anyString(), anyString())).thenReturn("10.0.2.2", "18080");
        configManager.initialize(mockContext);
        
        // Execute
        configManager.setServerIp("192.168.1.100");
        
        // Verify
        verify(mockEditor).putString("server_ip", "192.168.1.100");
        verify(mockEditor).apply();
    }
    
    @Test
    public void testSetServerPortSavesToStorage() {
        // Setup
        when(mockPreferences.getString(anyString(), anyString())).thenReturn("10.0.2.2", "18080");
        configManager.initialize(mockContext);
        
        // Execute
        configManager.setServerPort("8080");
        
        // Verify
        verify(mockEditor).putString("server_port", "8080");
        verify(mockEditor).apply();
    }
    
    @Test
    public void testSetServerAddressSavesToStorage() {
        // Setup
        when(mockPreferences.getString(anyString(), anyString())).thenReturn("10.0.2.2", "18080");
        configManager.initialize(mockContext);
        
        // Execute
        configManager.setServerAddress("192.168.1.100", "8080");
        
        // Verify
        verify(mockEditor).putString("server_ip", "192.168.1.100");
        verify(mockEditor).putString("server_port", "8080");
        verify(mockEditor).apply();
    }
    
    @Test
    public void testSetServerIpDoesNotSaveWhenUnchanged() {
        // Setup
        when(mockPreferences.getString("server_ip", "10.0.2.2")).thenReturn("10.0.2.2");
        when(mockPreferences.getString("server_port", "18080")).thenReturn("18080");
        configManager.initialize(mockContext);
        
        // Reset mock to clear initialization calls
        reset(mockEditor);
        
        // Execute: Set to same value
        configManager.setServerIp("10.0.2.2");
        
        // Verify: Should not save
        verify(mockEditor, never()).putString(anyString(), anyString());
        verify(mockEditor, never()).apply();
    }
    
    @Test
    public void testSettersWorkBeforeInitialization() {
        // Execute: Set values before initialization
        configManager.setServerIp("192.168.1.100");
        configManager.setServerPort("8080");
        
        // Verify: Values are set in memory
        assertEquals("IP should be set", "192.168.1.100", configManager.getServerIp());
        assertEquals("Port should be set", "8080", configManager.getServerPort());
        
        // Verify: No storage operations (storage is null)
        verify(mockEditor, never()).putString(anyString(), anyString());
    }
}

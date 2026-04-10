package edu.hitsz.aircraftwar.android.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Scans the local area network for available game servers.
 * Discovers servers by attempting connections to all IPs in the current C-class subnet.
 */
public class LanServerScanner {
    
    private static final int SCAN_PORT = 18080;
    private static final int SCAN_TIMEOUT_MS = 500;
    private static final String HEALTH_CHECK_PATH = "/api/leaderboard/score";
    
    /**
     * Represents a discovered server with its IP address and response time.
     */
    public static class DiscoveredServer {
        private final String ipAddress;
        private final long responseTimeMs;
        
        public DiscoveredServer(String ipAddress, long responseTimeMs) {
            this.ipAddress = ipAddress;
            this.responseTimeMs = responseTimeMs;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public long getResponseTimeMs() {
            return responseTimeMs;
        }
        
        @NonNull
        @Override
        public String toString() {
            return ipAddress + " (" + responseTimeMs + "ms)";
        }
    }
    
    /**
     * Callback interface for scan progress and results.
     */
    public interface ScanCallback {
        /**
         * Called when scan progress updates.
         * 
         * @param scannedCount number of IPs scanned so far
         * @param totalCount total number of IPs to scan
         */
        void onProgress(int scannedCount, int totalCount);
        
        /**
         * Called when scan completes.
         * 
         * @param servers list of discovered servers (may be empty)
         */
        void onComplete(List<DiscoveredServer> servers);
    }
    
    private final OkHttpClient client;
    
    public LanServerScanner() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
    }
    
    /**
     * Scans the current C-class subnet for available game servers.
     * This method runs asynchronously and reports progress via callback.
     * 
     * @param callback callback to receive scan progress and results
     */
    public void scanLan(ScanCallback callback) {
        NetworkExecutor.run(() -> {
            try {
                // Get current device's IP address
                String localIp = getLocalIpAddress();
                if (localIp == null) {
                    callback.onComplete(Collections.emptyList());
                    return;
                }
                
                // Calculate subnet to scan (C-class: xxx.xxx.xxx.0/24)
                String subnet = getSubnet(localIp);
                if (subnet == null) {
                    callback.onComplete(Collections.emptyList());
                    return;
                }
                
                // Scan all IPs in the subnet (1-254)
                List<DiscoveredServer> servers = scanSubnet(subnet, callback);
                
                // Sort by response time (fastest first)
                Collections.sort(servers, (a, b) -> Long.compare(a.responseTimeMs, b.responseTimeMs));
                
                callback.onComplete(servers);
            } catch (Exception e) {
                callback.onComplete(Collections.emptyList());
            }
        });
    }
    
    /**
     * Gets the local IP address of the device.
     * 
     * @return the local IP address, or null if not found
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // We want IPv4 addresses only
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // Failed to get network interfaces
        }
        return null;
    }
    
    /**
     * Extracts the C-class subnet prefix from an IP address.
     * For example: "192.168.1.100" -> "192.168.1"
     * 
     * @param ipAddress the full IP address
     * @return the subnet prefix, or null if invalid
     */
    private String getSubnet(String ipAddress) {
        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot > 0) {
            return ipAddress.substring(0, lastDot);
        }
        return null;
    }
    
    /**
     * Scans all IPs in the given subnet for game servers.
     * 
     * @param subnet the subnet prefix (e.g., "192.168.1")
     * @param callback callback for progress updates
     * @return list of discovered servers
     */
    private List<DiscoveredServer> scanSubnet(String subnet, ScanCallback callback) {
        List<DiscoveredServer> servers = Collections.synchronizedList(new ArrayList<>());
        int totalIps = 254; // Scan from .1 to .254
        AtomicInteger scannedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalIps);
        
        // Scan each IP in parallel
        for (int i = 1; i <= 254; i++) {
            final String ip = subnet + "." + i;
            
            NetworkExecutor.run(() -> {
                try {
                    DiscoveredServer server = checkServer(ip);
                    if (server != null) {
                        servers.add(server);
                    }
                } finally {
                    int scanned = scannedCount.incrementAndGet();
                    callback.onProgress(scanned, totalIps);
                    latch.countDown();
                }
            });
        }
        
        // Wait for all scans to complete (with timeout)
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return servers;
    }
    
    /**
     * Checks if a server is running at the given IP address.
     * 
     * @param ip the IP address to check
     * @return DiscoveredServer if server found, null otherwise
     */
    private DiscoveredServer checkServer(String ip) {
        String url = "http://" + ip + ":" + SCAN_PORT + HEALTH_CHECK_PATH;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        long startTime = System.currentTimeMillis();
        
        try (Response response = client.newCall(request).execute()) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.isSuccessful()) {
                return new DiscoveredServer(ip, responseTime);
            }
        } catch (IOException e) {
            // Server not found or not responding
        }
        
        return null;
    }
}

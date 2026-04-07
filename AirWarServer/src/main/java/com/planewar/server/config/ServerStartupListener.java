package com.planewar.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class ServerStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStartupListener.class);
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${server.address:0.0.0.0}")
    private String serverAddress;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("=".repeat(60));
        logger.info("Server started successfully!");
        logger.info("Listening on: {}:{}", serverAddress, serverPort);
        logger.info("=".repeat(60));
        
        if ("0.0.0.0".equals(serverAddress)) {
            logger.info("Server is listening on ALL network interfaces:");
            logger.info("  - Localhost: http://localhost:{}", serverPort);
            logger.info("  - Localhost: http://127.0.0.1:{}", serverPort);
            
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();
                            if (address.getAddress().length == 4) { // IPv4 only
                                logger.info("  - LAN: http://{}:{}", address.getHostAddress(), serverPort);
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                logger.warn("Could not enumerate network interfaces: {}", e.getMessage());
            }
            
            logger.info("=".repeat(60));
        } else {
            logger.info("Server is listening on specific address: http://{}:{}", serverAddress, serverPort);
            logger.info("=".repeat(60));
        }
    }
}

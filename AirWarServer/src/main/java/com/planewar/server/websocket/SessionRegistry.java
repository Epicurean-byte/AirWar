package com.planewar.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {
    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, long userId) {
        WebSocketSession oldSession = userSessionMap.put(userId, session);
        if (oldSession != null && !oldSession.getId().equals(session.getId())) {
            sessionUserMap.remove(oldSession.getId());
            try {
                oldSession.close(CloseStatus.NORMAL.withReason("duplicate login"));
            } catch (IOException e) {
                log.debug("failed to close old websocket for user {}: {}", userId, e.getMessage());
            }
        }
        sessionUserMap.put(session.getId(), userId);
        log.debug("user {} websocket authenticated", userId);
    }

    public Optional<Long> unregister(WebSocketSession session) {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId == null) {
            return Optional.empty();
        }

        WebSocketSession current = userSessionMap.get(userId);
        if (current != null && !current.getId().equals(session.getId())) {
            log.debug("ignore stale websocket disconnect for user {}", userId);
            return Optional.empty();
        }

        userSessionMap.remove(userId);
        log.debug("user {} websocket disconnected", userId);
        return Optional.of(userId);
    }

    public Optional<Long> userIdOf(WebSocketSession session) {
        return Optional.ofNullable(sessionUserMap.get(session.getId()));
    }

    public WebSocketSession sessionOf(long userId) {
        return userSessionMap.get(userId);
    }
}

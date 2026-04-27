package com.planewar.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.planewar.server.datastore.InMemoryDataStore;
import com.planewar.server.model.dto.WsMessageDto;
import com.planewar.server.model.entity.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class WsMessageSender {
    private static final Logger log = LoggerFactory.getLogger(WsMessageSender.class);

    private final SessionRegistry sessions;
    private final InMemoryDataStore store;

    public WsMessageSender(SessionRegistry sessions, InMemoryDataStore store) {
        this.sessions = sessions;
        this.store = store;
    }

    public void send(WebSocketSession session, WsMessageDto msg) throws IOException {
        session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
    }

    public void sendError(WebSocketSession session, String reason) throws IOException {
        send(session, WsMessageFactory.error(reason));
    }

    public void sendToUser(long userId, WsMessageDto msg) {
        WebSocketSession session = sessions.sessionOf(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            send(session, msg);
        } catch (IOException e) {
            log.warn("send to user {} failed: {}", userId, e.getMessage());
        }
    }

    public void broadcastToRoom(long roomId, WsMessageDto msg) {
        Room room = store.rooms.get(roomId);
        if (room == null) {
            return;
        }
        sendToUser(room.getPlayer1Id(), msg);
        if (room.getPlayer2Id() != 0) {
            sendToUser(room.getPlayer2Id(), msg);
        }
    }
}

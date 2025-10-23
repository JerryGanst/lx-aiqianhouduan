package org.example.ai_api.WS;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionHub {
    private final Map<String, WebSocketSession> sessionIdToSession = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        if (session == null) return;
        sessionIdToSession.put(session.getId(), session);
    }

    public void unregister(WebSocketSession session) {
        if (session == null) return;
        sessionIdToSession.remove(session.getId());
    }

    public void broadcastText(String text) {
        if (text == null) return;
        for (WebSocketSession session : sessionIdToSession.values()) {
            if (session == null || !session.isOpen()) continue;
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(text));
                } catch (Exception ignored) {
                }
            }
        }
    }
} 
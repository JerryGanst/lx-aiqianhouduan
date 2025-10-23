package org.example.ai_api.Config;

import org.example.ai_api.WS.HeartbeatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

//WebSocket配置类
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final HeartbeatWebSocketHandler handler;

    public WebSocketConfig(HeartbeatWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins("*"); // 允许跨域访问
    }
}
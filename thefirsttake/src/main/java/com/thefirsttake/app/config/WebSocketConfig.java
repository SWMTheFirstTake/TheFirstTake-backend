package com.thefirsttake.app.config;

import com.thefirsttake.app.chat.handler.ChatHandler;
import com.thefirsttake.app.chat.service.ChatCurationGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatCurationGeneratorService chatCurationGeneratorService;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler(), "/api/chat").setAllowedOrigins("*");
    }
    @Bean
    public ChatHandler chatWebSocketHandler() {
        return new ChatHandler(chatCurationGeneratorService);
    }
}


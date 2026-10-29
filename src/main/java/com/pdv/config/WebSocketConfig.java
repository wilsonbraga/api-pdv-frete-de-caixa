package com.pdv.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PdvProperties pdvProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixo para mensagens enviadas ao servidor
        registry.setApplicationDestinationPrefixes("/app");
        // Prefixo para tópicos de subscrição
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefixo para mensagens privadas (usuário específico)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = pdvProperties.getCors().getAllowedOrigins().split(",");
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}

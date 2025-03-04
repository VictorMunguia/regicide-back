package com.panda.regicide.websocket;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        // Usar el nombre completo para la clase netty-socketio:
        com.corundumstudio.socketio.Configuration socketConfig
                = new com.corundumstudio.socketio.Configuration();

        socketConfig.setHostname("0.0.0.0");
        socketConfig.setPort(8080);
        socketConfig.setOrigin("*");

        return new SocketIOServer(socketConfig);
    }
}

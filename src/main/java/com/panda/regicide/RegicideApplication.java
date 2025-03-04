package com.panda.regicide;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RegicideApplication implements CommandLineRunner {

	@Autowired
	private SocketIOServer socketIOServer;

	public static void main(String[] args) {
		SpringApplication.run(RegicideApplication.class, args);
	}

	@Override
	public void run(String... args) {
		// Iniciar el servidor socket.io
		socketIOServer.start();
		System.out.println(">>> Socket.IO server levantado en el puerto configurado");
	}

	@PreDestroy
	public void onStop() {
		// Detenerlo al cerrar la app
		socketIOServer.stop();
		System.out.println(">>> Socket.IO server detenido");
	}

}

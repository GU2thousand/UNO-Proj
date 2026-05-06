package server;

import common.NetworkDefaults;
import persistence.DatabaseManager;
import persistence.MatchRepository;
import persistence.PlayerRepository;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : NetworkDefaults.DEFAULT_PORT;
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();
        PlayerRepository playerRepository = new PlayerRepository(databaseManager);
        MatchRepository matchRepository = new MatchRepository(databaseManager, playerRepository);
        RoomManager roomManager = new RoomManager(playerRepository, matchRepository);
        ExecutorService executor = Executors.newCachedThreadPool();

        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("UNO server listening on port " + port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    executor.submit(new ClientHandler(socket, roomManager));
                }
            }
        } catch (BindException exception) {
            System.err.println("Port " + port + " is already in use. Start the server with another port, for example: java -cp target/classes server.ServerMain 5051");
            return;
        } finally {
            executor.shutdownNow();
        }
    }
}

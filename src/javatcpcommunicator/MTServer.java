package javatcpcommunicator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MTServer {
    public static void main(String[] args) {
        final int serverPort = 7890;
        System.out.println("Multi-Threaded Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Server is listening on port " + serverPort);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket);

                    new Thread(clientHandler).start();

                } catch (IOException ioEx) {
                    System.err.println("Error accepting client connection: " + ioEx.getMessage());
                }
            }
        } catch (IOException ioEx) {
            System.err.println("Could not start server on port " + serverPort + ": " + ioEx.getMessage());
        }
    }
}

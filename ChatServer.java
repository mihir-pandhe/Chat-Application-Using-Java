import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("Server started. Waiting for clients to connect...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private final PrintWriter out;
        private final BufferedReader in;
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                out.println("Enter your username:");
                username = in.readLine();
                System.out.println(username + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/exit")) {
                        out.println("You have exited the chat.");
                        break;
                    }

                    broadcastMessage(username, message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                    System.out.println(username + " has left the chat.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String username, String message) {
            String formattedMessage = "[" + sdf.format(new Date()) + "] " + username + ": " + message;
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.out.println(formattedMessage);
                    }
                }
            }
        }
    }
}

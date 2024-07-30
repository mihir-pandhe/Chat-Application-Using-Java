import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, List<ClientHandler>> chatRooms = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Server started. Waiting for clients to connect...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                // Create and start a new thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;
        private String currentRoom = null;
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                // Get client username
                out.println("Enter your username:");
                username = in.readLine();
                synchronized (clients) {
                    clients.put(username, this);
                }
                System.out.println(username + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/exit")) {
                        out.println("You have exited the chat.");
                        break;
                    } else if (message.startsWith("/pm ")) {
                        sendPrivateMessage(message);
                    } else if (message.startsWith("/join ")) {
                        joinChatRoom(message);
                    } else if (message.startsWith("/create ")) {
                        createChatRoom(message);
                    } else if (message.startsWith("/rooms")) {
                        listChatRooms();
                    } else if (message.startsWith("/roommsg ")) {
                        sendRoomMessage(message);
                    } else {
                        broadcastMessage(username, message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    synchronized (clients) {
                        clients.remove(username);
                    }
                    System.out.println(username + " has left the chat.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendPrivateMessage(String message) {
            int firstSpace = message.indexOf(' ');
            int secondSpace = message.indexOf(' ', firstSpace + 1);
            if (secondSpace != -1) {
                String recipient = message.substring(firstSpace + 1, secondSpace);
                String privateMessage = message.substring(secondSpace + 1);
                synchronized (clients) {
                    ClientHandler recipientHandler = clients.get(recipient);
                    if (recipientHandler != null) {
                        recipientHandler.out.println("[PM from " + username + "] " + privateMessage);
                        out.println("[PM to " + recipient + "] " + privateMessage);
                    } else {
                        out.println("User " + recipient + " not found.");
                    }
                }
            }
        }

        private void joinChatRoom(String message) {
            String roomName = message.split(" ", 2)[1];
            synchronized (chatRooms) {
                chatRooms.computeIfAbsent(roomName, k -> new ArrayList<>()).add(this);
            }
            currentRoom = roomName;
            out.println("Joined room: " + roomName);
        }

        private void createChatRoom(String message) {
            String roomName = message.split(" ", 2)[1];
            synchronized (chatRooms) {
                if (!chatRooms.containsKey(roomName)) {
                    chatRooms.put(roomName, new ArrayList<>());
                    out.println("Created and joined room: " + roomName);
                    currentRoom = roomName;
                    chatRooms.get(roomName).add(this);
                } else {
                    out.println("Room " + roomName + " already exists.");
                }
            }
        }

        private void listChatRooms() {
            synchronized (chatRooms) {
                out.println("Available chat rooms: " + chatRooms.keySet());
            }
        }

        private void sendRoomMessage(String message) {
            if (currentRoom == null) {
                out.println("You are not in any chat room.");
                return;
            }
            String roomMessage = message.split(" ", 2)[1];
            synchronized (chatRooms) {
                List<ClientHandler> roomClients = chatRooms.get(currentRoom);
                if (roomClients != null) {
                    for (ClientHandler client : roomClients) {
                        if (client != this) {
                            client.out.println("[Room " + currentRoom + "] " + username + ": " + roomMessage);
                        }
                    }
                }
            }
        }

        private void broadcastMessage(String username, String message) {
            String formattedMessage = "[" + sdf.format(new Date()) + "] " + username + ": " + message;
            synchronized (clients) {
                for (ClientHandler client : clients.values()) {
                    if (client != this) {
                        client.out.println(formattedMessage);
                    }
                }
            }
        }
    }
}

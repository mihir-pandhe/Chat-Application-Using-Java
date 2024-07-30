import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, List<ClientHandler>> chatRooms = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> userCredentials = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, List<String>> messageHistory = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Server started. Waiting for clients to connect...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

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
                authenticateUser();

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Error with client " + username + ": " + e.getMessage());
            } finally {
                disconnectClient();
            }
        }

        private void authenticateUser() throws IOException {
            while (true) {
                out.println(

                        "Enter /login <username> <password> to login or /register <username> <password> to register:");
                String command = in.readLine();
                if (command == null) {
                    break;
                }
                if (command.startsWith("/login ")) {
                    String[] parts = command.split(" ");

                    if (parts.length == 3 && userCredentials.containsKey(parts[1])
                            && userCredentials.get(parts[1]).equals(parts[2])) {
                        username = parts[1];
                        synchronized (clients) {
                            clients.put(username, this);
                        }
                        System.out.println(username + " has logged in.");
                        out.println("Login successful. Welcome, " + username + "!");
                        break;
                    } else {
                        out.println("Invalid username or password.");
                    }
                } else if (command.startsWith("/register ")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 3 && !userCredentials.containsKey(parts[1])) {
                        userCredentials.put(parts[1], parts[2]);
                        username = parts[1];
                        synchronized (clients) {
                            clients.put(username, this);
                        }
                        System.out.println(username + " has registered and logged in.");
                        out.println("Registration successful. Welcome, " + username + "!");
                        break;
                    } else {
                        out.println("Username already exists or invalid command.");
                    }
                } else {
                    out.println("Invalid command. Please use /login or /register.");
                }
            }
        }

        private void handleMessage(String message) {
            try {
                if (message.startsWith("/exit")) {
                    out.println("You have exited the chat.");
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
                } else if (message.startsWith("/users")) {
                    listOnlineUsers();
                } else {
                    out.println("Unknown command. Please try again.");
                }
            } catch (Exception e) {
                out.println("An error occurred: " + e.getMessage());
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
            } else {
                out.println("Invalid command format. Use /pm <username> <message>");
            }
        }

        private void joinChatRoom(String message) {
            String roomName = message.split(" ", 2)[1];
            synchronized (chatRooms) {
                chatRooms.computeIfAbsent(roomName, k -> new ArrayList<>()).add(this);
            }
            currentRoom = roomName;
            out.println("Joined room: " + roomName);
            sendRoomHistory(roomName);
        }

        private void createChatRoom(String message) {
            String roomName = message.split(" ", 2)[1];
            synchronized (chatRooms) {
                if (!chatRooms.containsKey(roomName)) {
                    chatRooms.put(roomName, new ArrayList<>());
                    out.println("Created and joined room: " + roomName);
                    currentRoom = roomName;
                    chatRooms.get(roomName).add(this);
                    synchronized (messageHistory) {
                        messageHistory.put(roomName, new ArrayList<>());
                    }
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
                    String formattedMessage = "[Room " + currentRoom + "] " + username + ": " + roomMessage;
                    synchronized (messageHistory) {
                        messageHistory.get(currentRoom).add(formattedMessage);
                    }
                    for (ClientHandler client : roomClients) {
                        if (client != this) {
                            client.out.println(formattedMessage);
                        }
                    }
                }
            }
        }

        private void listOnlineUsers() {
            synchronized (clients) {
                out.println("Online users: " + clients.keySet());
            }
        }

        private void disconnectClient() {
            try {
                if (username != null) {
                    synchronized (clients) {
                        clients.remove(username);
                    }
                    System.out.println(username + " has disconnected.");
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendRoomHistory(String roomName) {
            synchronized (messageHistory) {
                List<String> history = messageHistory.get(roomName);
                if (history != null) {
                    for (String message : history) {
                        out.println(message);
                    }
                }
            }
        }
    }
}

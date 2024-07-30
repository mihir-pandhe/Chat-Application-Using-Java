import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatServer {
    public static void main(String[] args) {
        int port = 12345;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Waiting for clients to connect...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected!");

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            out.println("Enter your username:");
            String username = in.readLine();
            System.out.println(username + " has joined the chat.");

            String messageFromClient;
            String messageToClient;

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            while ((messageFromClient = in.readLine()) != null) {
                if (messageFromClient.equalsIgnoreCase("/exit")) {
                    System.out.println("Client has left the chat.");
                    break;
                }

                System.out.println("[" + sdf.format(new Date()) + "] " + username + ": " + messageFromClient);

                System.out.print("Server: ");
                messageToClient = userInput.readLine();
                out.println(messageToClient);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

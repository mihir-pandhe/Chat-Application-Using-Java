import java.io.*;
import java.net.*;

public class ChatClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connected to server.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Enter your username: ");
            String username = userInput.readLine();
            out.println(username);

            String messageToServer;
            String messageFromServer;

            while (true) {
                messageFromServer = in.readLine();
                if (messageFromServer != null) {
                    System.out.println("Server: " + messageFromServer);
                }

                System.out.print("Client: ");
                messageToServer = userInput.readLine();
                if (messageToServer.equalsIgnoreCase("/exit")) {
                    out.println("/exit");
                    break;
                }
                out.println(messageToServer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

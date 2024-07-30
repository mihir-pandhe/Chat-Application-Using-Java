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

            String messageToServer;
            String messageFromServer;

            while (true) {
                System.out.print("Client: ");
                messageToServer = userInput.readLine();
                out.println(messageToServer);

                messageFromServer = in.readLine();
                if (messageFromServer != null) {
                    System.out.println("Server: " + messageFromServer);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

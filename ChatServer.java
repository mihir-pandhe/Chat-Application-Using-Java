import java.io.*;
import java.net.*;

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

            String messageFromClient;
            String messageToClient;

            while ((messageFromClient = in.readLine()) != null) {
                System.out.println("Client: " + messageFromClient);

                System.out.print("Server: ");
                messageToClient = userInput.readLine();
                out.println(messageToClient);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

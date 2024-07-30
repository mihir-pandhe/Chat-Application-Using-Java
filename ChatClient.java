import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            System.out.println("Connected to server.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String serverMessage = in.readLine();
                System.out.println(serverMessage);
                if (serverMessage.startsWith("Login successful")
                        || serverMessage.startsWith("Registration successful")) {
                    break;
                }
                String command = userInput.readLine();
                out.println(command);
            }

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            String messageToSend;
            while (true) {
                System.out.print("Client: ");
                messageToSend = userInput.readLine();
                if (messageToSend.equalsIgnoreCase("/exit")) {
                    out.println("/exit");
                    break;
                }
                out.println(messageToSend);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package javatcpcommunicator;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) {
        final String hostName = "localhost";
        final int port = 7890;
        
        System.out.println("Starting TCP Communicator Client");
        
        try(
            Socket socket = new Socket(hostName, port);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            BufferedReader sysBuffReader = new BufferedReader(new InputStreamReader(System.in));
        ) {
            String clientWelcomeMessage = ReceiveMessage(inputStream);
            System.out.println("Server: " + clientWelcomeMessage);
            System.out.println("Connected. Type 'bye' to exit.");
            
            String userMessage;
            while ((userMessage = sysBuffReader.readLine()) != null) {
                // Send message to server using framing
                SendMessage(outputStream, userMessage);

                if ("bye".equalsIgnoreCase(userMessage)) {
                    break;
                }
                
                String serverResponse = ReceiveMessage(inputStream);
                System.out.println("Server: " + serverResponse);
            }
        } 
        catch (IOException ioEx) {
            System.err.println("Connection error: " + ioEx.getMessage());
        }
        System.out.println("Client is closing.");
    }
    
    private static void SendMessage(DataOutputStream outStream, String clMessage) throws IOException {
        byte[] messageBytes = clMessage.getBytes("UTF-8");
        outStream.writeInt(messageBytes.length);
        outStream.write(messageBytes);
        outStream.flush();
    }

    private static String ReceiveMessage(DataInputStream inStream) throws IOException {
        int messageLength = inStream.readInt();
        if (messageLength > 0) {
            byte[] messageBytes = new byte[messageLength];
            inStream.readFully(messageBytes, 0, messageLength);
            return new String(messageBytes, "UTF-8");
        }
        return "";
    }
}

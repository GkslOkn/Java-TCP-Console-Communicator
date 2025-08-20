package javatcpcommunicator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable{
    private final Socket clientSocket;
    private static final Pattern completeHTMLPattern = Pattern.compile("(?s)^<([a-z][a-z0-9]*)\\b[^>]*>.*</\\1>$", Pattern.CASE_INSENSITIVE);
    private static final Pattern partialHTMLPattern = Pattern.compile("<([a-z][a-z0-9]*)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try{
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
            StringBuilder messageBuffer = new StringBuilder();

            SendMessage(outputStream, "<html><body><h1>Welcome!</h1> <p>You are connected to the multi-threaded server.</p></body></html>");

            while(true){
                String clientMessage = ReceiveMessage(inputStream);

                if (clientMessage == null){
                    break;
                } 
                
                System.out.println("Received from " + clientSocket.getInetAddress() + ": \"" + clientMessage + "\"");

                if("bye".equalsIgnoreCase(clientMessage.trim())){
                    break;
                }

                ProcessMessage(clientMessage, messageBuffer, outputStream);
            }
        }
        catch(IOException e){
            System.out.println("Client " + clientSocket.getInetAddress() + " disconnected.");
        }
        finally{
            try {
                clientSocket.close();
            }
            catch (IOException e) {}
        }
        System.out.println("Thread for client " + clientSocket.getInetAddress() + " has finished.");
    }

    private void ProcessMessage(String clientMessage, StringBuilder messageBuffer, DataOutputStream outStream) throws IOException {
        String serverResponse;
        if (messageBuffer.length() == 0) {
            if (IsPartialHtml(clientMessage)) {
                messageBuffer.append(clientMessage);
            } else {
                serverResponse = "<html><body style='color:orange;'>Discarded: Message contains no HTML tags.</body></html>";
                SendMessage(outStream, serverResponse);
                return;
            }
        } else {
            messageBuffer.append(clientMessage);
        }

        if (IsCompleteHtml(messageBuffer.toString())) {
            System.out.println("Complete HTML from " + clientSocket.getInetAddress() + ": " + messageBuffer.toString());
            serverResponse = "<html><body>Server processed your complete HTML: " + messageBuffer.toString() + "</body></html>";
            SendMessage(outStream, serverResponse);
            messageBuffer.setLength(0);
        } else {
            serverResponse = "<html><body><i>Received fragment, waiting for more...</i></body></html>";
            SendMessage(outStream, serverResponse);
        }
    }

    private static boolean IsCompleteHtml(String clMessage) {
        return completeHTMLPattern.matcher(clMessage.trim()).matches();
    }

    private static boolean IsPartialHtml(String clMessage) {
        return partialHTMLPattern.matcher(clMessage.trim()).find();
    }
    
    private static void SendMessage(DataOutputStream outStream, String clMessage) throws IOException {
        byte[] messageBytes = clMessage.getBytes("UTF-8");
        outStream.writeInt(messageBytes.length);
        outStream.write(messageBytes);
        outStream.flush();
    }

    private static String ReceiveMessage(DataInputStream inStream) throws IOException {
        try {
            int messageLength = inStream.readInt();
            if (messageLength > 0) {
                byte[] messageBytes = new byte[messageLength];
                inStream.readFully(messageBytes, 0, messageLength);
                return new String(messageBytes, "UTF-8");
            }
            return "";
        } catch(EOFException eofEx) {
            return null;
        }
    }
}

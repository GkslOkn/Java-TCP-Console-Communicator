package javatcpcommunicator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable{
    private final Socket clientSocket;
    private static final int maxBuffer = 65536;

    private static final Pattern extractHTMLRegex = Pattern.compile("(?s)(<([a-z][a-z0-9]*)\\b[^>]*>.*?</\\2>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern partialHTMLRegex = Pattern.compile("<([a-z][a-z0-9]*)\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream())) {
            
            StringBuilder messageBuffer = new StringBuilder();
            String welcomeMessage = "<html><body><h1> WELCOME TO THE TCP COMMUNICATOR. </h1></body></html>";
            SendMessage(outputStream, welcomeMessage);

            while (true) {
                String clientMessage = ReceiveMessage(inputStream);
                if (clientMessage == null || "bye".equalsIgnoreCase(clientMessage.trim())) {
                    break;
                }
                
                if (messageBuffer.length() + clientMessage.length() > maxBuffer) {
                    SendMessage(outputStream, "<html><body> Error: Disconnecting due to buffer overflow. </body></html>");
                    System.err.println("Buffer overflow for client " + clientSocket.getInetAddress() + ". Closing connection.");
                    break;
                }
                
                messageBuffer.append(clientMessage);
                processBuffer(messageBuffer, outputStream);
            }
        }
        catch (IOException ioEx){
            System.out.println("Client " + clientSocket.getInetAddress() + " has disconnected.");
        }
        finally{
            try{
                clientSocket.close();
            }
            catch(IOException ioEx) {}
        }
        System.out.println("Thread for client " + clientSocket.getInetAddress() + " has finished.");
    }

    private void processBuffer(StringBuilder messageBuffer, DataOutputStream outStream) throws IOException {
        StringBuilder serverResponseBuilder = new StringBuilder();
        Matcher regexMatcher = extractHTMLRegex.matcher(messageBuffer.toString());

        while (regexMatcher.find()) {
            String completeMessage = regexMatcher.group(1);
            serverResponseBuilder.append("<p>Processed: ").append(EscapeHtml(completeMessage)).append("</p>");
            
            messageBuffer.delete(0, regexMatcher.end());
            regexMatcher = extractHTMLRegex.matcher(messageBuffer.toString());
        }

        if (messageBuffer.length() > 0 && !IsPartialHtml(messageBuffer.toString())){
            serverResponseBuilder.append("<p>Discarded: ").append(EscapeHtml(messageBuffer.toString())).append("</p>");
            messageBuffer.setLength(0);
        }

        if (serverResponseBuilder.length() > 0){
            SendMessage(outStream, "<html><body>" + serverResponseBuilder.toString() + "</body></html>");
        }
        
        else if (messageBuffer.length() > 0){
            SendMessage(outStream, "<html><body><p>Received fragment, waiting for more fragments.</p></body></html>");
        }
    }
    
    private String EscapeHtml(String clMessage) {
        return clMessage.replace("<", "").replace(">", "");
    }
    
    private static boolean IsPartialHtml(String clMessage) {
        return partialHTMLRegex.matcher(clMessage.trim()).find();
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
        } catch (EOFException eofEx) {
            return null;
        }
    }
}

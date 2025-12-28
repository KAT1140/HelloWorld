import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoom;

    public ClientHandler(Socket socket) {
        try {
            this.clientSocket = socket;
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String room) {
        this.currentRoom = room;
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processMessage(inputLine);
            }
        } catch (Exception e) {
            System.out.println("❌ " + (username != null ? username : "Client") + " đã ngắt kết nối.");
        } finally {
            Server.removeClient(this);
            closeConnection();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // XỬ LÝ TIN NHẮN VÀ COMMANDS
    // ════════════════════════════════════════════════════════════════════
    private void processMessage(String message) {
        System.out.println("📩 " + (username != null ? username : "?") + ": " + message);

        // Parse commands
        if (message.startsWith("/")) {
            handleCommand(message);
        }
        // Set username (first message)
        else if (message.startsWith("USERNAME:")) {
            this.username = message.substring(9);
            System.out.println("👤 User registered: " + username);

            // Send room list and online users
            sendMessage(Server.getRoomList());
            sendMessage(Server.getOnlineUsers());
            Server.broadcastToAll("USER_JOINED:" + username);
            Server.broadcastToAll(Server.getOnlineUsers());
        }
        // Regular chat message -> broadcast to current room
        else if (currentRoom != null) {
            String fullMessage = "MSG:" + currentRoom + ":" + username + ":" + message;
            Server.broadcastToRoom(currentRoom, fullMessage);
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/join":
                if (parts.length >= 2) {
                    String room = parts[1];
                    Server.joinRoom(this, room);
                    sendMessage("JOINED:" + room);
                    sendMessage(Server.getOnlineUsers());
                }
                break;

            case "/leave":
                if (currentRoom != null) {
                    String leftRoom = currentRoom;
                    Server.leaveCurrentRoom(this);
                    sendMessage("LEFT:" + leftRoom);
                    currentRoom = null;
                }
                break;

            case "/create":
                if (parts.length >= 2) {
                    String newRoom = parts[1];
                    Server.createRoom(newRoom);
                    sendMessage(Server.getRoomList());
                }
                break;

            case "/pm":
                // /pm username message
                if (parts.length >= 3) {
                    String targetUser = parts[1];
                    String pmMessage = parts[2];
                    sendPrivateMessage(targetUser, pmMessage);
                }
                break;

            case "/rooms":
                sendMessage(Server.getRoomList());
                break;

            case "/online":
                sendMessage(Server.getOnlineUsers());
                break;

            case "/file":
                // /file filename:size:base64data
                if (parts.length >= 2 && currentRoom != null) {
                    String fileData = parts[1];
                    if (parts.length >= 3) {
                        fileData = parts[1] + " " + parts[2];
                    }
                    // Broadcast file to room
                    String fileMsg = "FILE:" + currentRoom + ":" + username + ":" + fileData;
                    Server.broadcastToRoom(currentRoom, fileMsg);
                    System.out.println("[FILE] " + username + " shared a file in " + currentRoom);
                }
                break;

            default:
                sendMessage("SYSTEM:Unknown command: " + cmd);
        }
    }

    private void sendPrivateMessage(String targetUsername, String message) {
        synchronized (Server.allClients) {
            for (ClientHandler client : Server.allClients) {
                if (client.getUsername() != null && client.getUsername().equals(targetUsername)) {
                    client.sendMessage("PM:" + this.username + ":" + message);
                    this.sendMessage("PM_SENT:" + targetUsername + ":" + message);
                    return;
                }
            }
        }
        sendMessage("SYSTEM:User not found: " + targetUsername);
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void closeConnection() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
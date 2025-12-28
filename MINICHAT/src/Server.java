import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 9999;

    // ════════════════════════════════════════════════════════════════════
    // QUẢN LÝ ROOMS VÀ USERS
    // ════════════════════════════════════════════════════════════════════
    public static Map<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    public static Set<ClientHandler> allClients = Collections.synchronizedSet(new HashSet<>());

    // Default rooms
    static {
        rooms.put("General", Collections.synchronizedSet(new HashSet<>()));
        rooms.put("Gaming", Collections.synchronizedSet(new HashSet<>()));
        rooms.put("Music", Collections.synchronizedSet(new HashSet<>()));
        rooms.put("Random", Collections.synchronizedSet(new HashSet<>()));
    }

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("   💬 MiniChat Server - Liquid Glass Edition");
        System.out.println("═══════════════════════════════════════════");

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Server đang chạy tại cổng " + PORT);
            System.out.println("📌 Rooms available: " + rooms.keySet());
            System.out.println("⏳ Đang chờ clients kết nối...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client mới kết nối: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                allClients.add(clientHandler);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("❌ Lỗi Server: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ROOM MANAGEMENT
    // ════════════════════════════════════════════════════════════════════
    public static void createRoom(String roomName) {
        if (!rooms.containsKey(roomName)) {
            rooms.put(roomName, Collections.synchronizedSet(new HashSet<>()));
            broadcastToAll("ROOM_CREATED:" + roomName);
            System.out.println("📁 Room mới được tạo: " + roomName);
        }
    }

    public static void joinRoom(ClientHandler client, String roomName) {
        // Leave current room first
        leaveCurrentRoom(client);

        // Join new room
        if (rooms.containsKey(roomName)) {
            rooms.get(roomName).add(client);
            client.setCurrentRoom(roomName);
            broadcastToRoom(roomName, "SYSTEM:" + client.getUsername() + " đã tham gia " + roomName);
            System.out.println("👤 " + client.getUsername() + " joined " + roomName);
        }
    }

    public static void leaveCurrentRoom(ClientHandler client) {
        String currentRoom = client.getCurrentRoom();
        if (currentRoom != null && rooms.containsKey(currentRoom)) {
            rooms.get(currentRoom).remove(client);
            broadcastToRoom(currentRoom, "SYSTEM:" + client.getUsername() + " đã rời " + currentRoom);
        }
    }

    public static void broadcastToRoom(String roomName, String message) {
        if (rooms.containsKey(roomName)) {
            synchronized (rooms.get(roomName)) {
                for (ClientHandler client : rooms.get(roomName)) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void broadcastToAll(String message) {
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                client.sendMessage(message);
            }
        }
    }

    public static String getRoomList() {
        return "ROOMS:" + String.join(",", rooms.keySet());
    }

    public static String getOnlineUsers() {
        List<String> users = new ArrayList<>();
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                users.add(client.getUsername() + ":"
                        + (client.getCurrentRoom() != null ? client.getCurrentRoom() : "Lobby"));
            }
        }
        return "ONLINE:" + String.join(",", users);
    }

    public static void removeClient(ClientHandler client) {
        leaveCurrentRoom(client);
        allClients.remove(client);
        broadcastToAll("USER_LEFT:" + client.getUsername());
        broadcastToAll(getOnlineUsers());
    }
}
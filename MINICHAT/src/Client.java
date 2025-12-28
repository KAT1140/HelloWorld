import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    // IP của Server. "127.0.0.1" (hoặc "localhost") có nghĩa là
    // Server đang chạy trên CÙNG MÁY TÍNH với Client.
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9999; // Phải khớp với cổng của Server

    public static void main(String[] args) {
        try {
            // 1. Kết nối đến Server
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Đã kết nối tới MiniChat Server!");

            // 2. Tạo luồng để ĐỌC tin nhắn TỪ Server
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // 3. Tạo luồng để GỬI tin nhắn TỚI Server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // 4. Tạo một luồng (Thread) riêng chỉ để lắng nghe tin nhắn từ Server
            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverIn.readLine()) != null) {
                        // In tin nhắn của Server ra màn hình
                        System.out.println(serverMessage);
                    }
                } catch (Exception e) {
                    System.out.println("Mất kết nối với Server.");
                }
            });
            readThread.start(); // Bắt đầu chạy luồng đọc

            // 5. Luồng main (luồng chính) sẽ đọc tin nhắn từ bàn phím và gửi đi
            Scanner sc = new Scanner(System.in);
            System.out.println("Nhập tin nhắn của bạn (gõ 'bye' để thoát):");
            
            while (true) {
                String myMessage = sc.nextLine(); // Đọc tin nhắn từ bàn phím
                out.println(myMessage); // Gửi tin nhắn đó lên Server

                if ("bye".equalsIgnoreCase(myMessage)) {
                    break; // Nếu gõ 'bye' thì thoát vòng lặp
                }
            }

            // Đóng kết nối
            sc.close();
            socket.close();

        } catch (Exception e) {
            System.err.println("Không thể kết nối tới Server: " + e.getMessage());
        }
    }
}
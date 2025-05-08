import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        int port = 1234; 

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server กำลังรอการเชื่อมต่อที่พอร์ต " + port);

            Socket socket = serverSocket.accept(); 
            System.out.println("Client เชื่อมต่อแล้ว");

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String message = reader.readLine();
            System.out.println("รับข้อความจาก Client: " + message);

            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

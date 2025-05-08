import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1234;

        try (Socket socket = new Socket(host, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Scanner scanner = new Scanner(System.in);
            System.out.print("พิมพ์ข้อความที่ต้องการส่ง: ");
            String message = scanner.nextLine();

            writer.println(message);

            socket.close();
        } catch (UnknownHostException ex) {
            System.out.println("Server ไม่พบ: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("เกิดข้อผิดพลาดในการเชื่อมต่อ: " + ex.getMessage());
        }
    }
}

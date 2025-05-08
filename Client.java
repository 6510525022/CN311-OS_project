import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1234;

        try (Socket socket = new Socket(host, port)) {
            Scanner scanner = new Scanner(System.in);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            System.out.println(reader.readLine());

            String message;

            while (true) {
                System.out.print("คุณพิมพ์: ");
                message = scanner.nextLine();
                writer.println(message);

                if (message.equalsIgnoreCase("exit"))
                    break;

                // ✅ อ่านทุกบรรทัดจาก server จนเจอ "END"
                String responseLine;
                while (!(responseLine = reader.readLine()).equals("END")) {
                    System.out.println("Server: " + responseLine);
                }
            }

            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

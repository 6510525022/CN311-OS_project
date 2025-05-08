import java.io.*;
import java.net.*;

public class GameServer {

    public static void main(String[] args) {
        final int port = 1234;
        GameState gameState = new GameState(); // ใช้ shared state สำหรับ ClientHandler ทุกตัว

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server รันอยู่ที่พอร์ต " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("มี Client ใหม่เชื่อมต่อ");

                ClientHandler clientHandler = new ClientHandler(clientSocket, gameState);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

// เป็นตัวกลางระหว่าง Client แต่ละคนกับ Server
// lobby - Client ส่งสัญญาณการเริ่มเกม
// in-game - รับข้อมูลสดจาก Server, Client ส่งข้อมูลควบคุมปลาของตัวเอง
// Result - รับผลจาก server มาแสดง, Client ส่งสัญญาณเพื่อ next ไปหน้า lobby
class ClientHandler extends Thread {

    private Socket socket;
    private GameState gameState; // shared state

    public ClientHandler(Socket socket, GameState gameState) {
        this.socket = socket;
        this.gameState = gameState;
    }

    public void run() {
        try (InputStream input = socket.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input)); OutputStream output = socket.getOutputStream(); PrintWriter writer = new PrintWriter(output, true);) 
        {
            writer.println("เชื่อมต่อกับ Server สำเร็จ");

            String text;
            while ((text = reader.readLine()) != null) {
                int currentPhase = gameState.getGamePhase(); // ดึงค่า GamePhase ปัจจุบัน

                switch (currentPhase) {
                    case 0: // lobby phase
                        writer.println("Lobby Phase");
                        if (text.equals("start")) {
                            gameState.setGamePhase(1); // ✅ เปลี่ยน phase แล้วทุก client เห็น
                            writer.println("เริ่มเกมแล้ว");
                        }
                        writer.println("END");
                        break;
                    case 1: // in-game phase
                        // ประมวลผล in-game
                        writer.println("In-Game: พิมพ์ up/down/left/right");
                        if (text.equals("next")) {
                            gameState.setGamePhase(2); // ✅ เปลี่ยน phase แล้วทุก client เห็น
                            writer.println("เกมจบแล้ว");
                        }
                        writer.println("END");
                        break;
                    case 2: // result phase
                        writer.println("Result Phase");
                        if (text.equals("next")) {
                            gameState.setGamePhase(0);
                            writer.println("กลับไป Lobby แล้ว");
                        }
                        writer.println("END");
                        break;
                    default:
                        writer.println("Phase ไม่ถูกต้อง");
                        writer.println("END");
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

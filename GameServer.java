
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {

    // setup variable
    final static int PORT = 1234;
    static String currentFishState = "";

    // hashmap เก็บปลาของ client (รองรับการทำงานหลาย thread)
    public static Map<Socket, Fish> fishMap = new ConcurrentHashMap<>();

    // เก็บรายชื่อ client ที่เชื่อมต่อกับ server
    // ใช้ synchronized list เพื่อให้หลายเธรดใช้งานพร้อมกันได้อย่างปลอดภัย
    // (เรื่อง critical section)
    public static List<ClientHandler> handlers = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // สร้าง GameState เพื่อเริ่มต้นแชร์สถานะเกมให้กับ client
        GameState gameState = new GameState();

        // สร้าง ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server รันอยู่ที่พอร์ต " + PORT);

            // Thread สำหรับส่ง FishState ให้ client ทุกคนทุก ๆ 33ms
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                broadcastFishState();
            }, 0, 33, TimeUnit.MILLISECONDS); //30 per second
            
            //รอ client เชื่อมต่อที่ ServerSocket
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("มี Client ใหม่เชื่อมต่อ");

                // สร้างปลาให้ client
                Fish playerFish = new Fish(random(100, 500), random(100, 500), 200, "right", "player", true);
                fishMap.put(clientSocket, playerFish);

                // สร้าง ClientHandler (Thread) สำหรับ client
                ClientHandler clientHandler = new ClientHandler(clientSocket, gameState);
                handlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ส่งสถานะปลาให้ client ทุกคน
    public static void broadcastFishState() {
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Socket, Fish> entry : fishMap.entrySet()) {
            Socket socket = entry.getKey();
            Fish f = entry.getValue();

            sb.append("id:").append(socket.hashCode())
                    .append(", x:").append(f.x)
                    .append(", y:").append(f.y)
                    .append(", size:").append(f.size)
                    .append(", isAlive:").append(f.isAlive)
                    .append("; ");
        }

        String data = "สถานะของปลา:" + sb.toString();
        //ถ้า State ของปลาไม่เปลี่ยนแปลง ก็ไม่ต้องส่งข้อมูลไปให้ client
        if (currentFishState.equals(data)) return;
        currentFishState = data;
        for (ClientHandler h : handlers) {
            h.send(currentFishState);
        }
        System.err.println(currentFishState);
    }

    // method สุ่ม สำหรับใช้สุ่มจุดเกิด
    public static int random(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }
}

class ClientHandler extends Thread {

    private Socket socket;
    private GameState gameState;
    private PrintWriter writer;

    public ClientHandler(Socket socket, GameState gameState) {
        this.socket = socket;
        this.gameState = gameState;
    }

    // method สำหรับส่งข้อมูลให้ client
    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // ใน ClientHandler
    public void run() {
        try (InputStream input = socket.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input)); OutputStream output = socket.getOutputStream();) 
        {
            writer = new PrintWriter(output, true);
            writer.println("เชื่อมต่อกับ Server สำเร็จ");

            String text;
            while ((text = reader.readLine()) != null) {
                int currentPhase = gameState.getGamePhase();
                switch (currentPhase) {
                    case 0: // Lobby phase
                        // ถ้า client ส่งคำสั่งมาให้ server
                        if (text.equals("start")) {
                            gameState.setGamePhase(1); // Change phase เป็น in-game
                            writer.println("เริ่มเกมแล้ว");
                            writer.println("In-game phase {up, down, left, right, next}");
                        }else{
                            writer.println("Lobby phase {start}");
                        }
                        break;

                    case 1: // In-game phase
                        // ถ้า client ส่งคำสั่งมาให้ server
                        Fish playerFish = GameServer.fishMap.get(socket);
                        if (playerFish != null && playerFish.isAlive) {
                            if (List.of("up", "down", "left", "right").contains(text)) {
                                playerFish.move(text);
                            }
                        }

                        if (text.equals("next")) { // จบเกม
                            gameState.setGamePhase(2); // เปลี่ยน phase แล้วทุก client เห็น
                            writer.println("เกมจบแล้ว");
                            writer.println("result phase {next}");
                        } else {
                            writer.println("In-game phase {up, down, left, right, next}");
                        }
                        break;

                    case 2: // Result phase
                        if (text.equals("next")) {
                            gameState.setGamePhase(0); // Return to Lobby phase
                            writer.println("กลับไป Lobby แล้ว");
                            writer.println("Lobby phase {start}");
                        } else {
                            writer.println("result phase {next}");
                        }
                        break;
                }
            
                writer.println("END");
            } 
        
            // ปิดการเชื่อมต่อ
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

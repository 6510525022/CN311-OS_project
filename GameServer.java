
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

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

            // Thread สำหรับส่ง FishState ให้ client ทุกคนทุก ๆ 16ms
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                broadcastFishState(); // ขยับ
                checkFishCollisions(); // ชนหมาย
            }, 0, 16, TimeUnit.MILLISECONDS); //60 per second

            ScheduledExecutorService spawner = Executors.newScheduledThreadPool(1);
            spawner.scheduleAtFixedRate(() -> {
                Fish enemy = Fish.spawnEnemyFish();
                fishMap.put(new Socket(), enemy); // <-- ต้องเปลี่ยนไปใช้ ID จริง
            }, 0, 1300, TimeUnit.MILLISECONDS);

            // รอ client เชื่อมต่อที่ ServerSocket
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("มี Client ใหม่เชื่อมต่อ");

                // สร้างปลาให้ client
                Fish playerFish = new Fish(random(200, 600), random(300, 400), 25, "right", "player", true);
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
        List<Socket> toRemove = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Socket, Fish> entry : fishMap.entrySet()) {
            Socket socket = entry.getKey();
            Fish f = entry.getValue();

            // ปลา bot ว่าย เอง
            if(!f.isPlayer) {
                f.move(f.direction);
            }

            if (!f.isPlayer && (!f.isAlive)) {
                toRemove.add(socket);
                continue; // ไม่ต้องส่งสถานะตัวที่ตายหรือหลุดขอบ
            }

            sb.append("id:").append(socket.hashCode())
                    .append(", x:").append(f.x)
                    .append(", y:").append(f.y)
                    .append(", size:").append(f.size)
                    .append(", isAlive:").append(f.isAlive)
                    .append("; ");
        }

        for (Socket s : toRemove) {
            fishMap.remove(s);
        }

        String data = "Fish:" + sb.toString();
        // ถ้า State ของปลาไม่เปลี่ยนแปลง ก็ไม่ต้องส่งข้อมูลไปให้ client
        if (currentFishState.equals(data))
            return;
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

    public static void broadcastPhase(int currentPhase) {
        for (ClientHandler h : handlers) {
            h.send("phase:" + currentPhase);
        }
    }

    public static void checkFishCollisions() {
        List<Socket> toRemove = new ArrayList<>();

        for (Map.Entry<Socket, Fish> entry1 : fishMap.entrySet()) {
            Fish fish1 = entry1.getValue();
            Socket socket1 = entry1.getKey();

            // ข้ามที่ไม่ใช้ผู้เล่น
            if (!fish1.isPlayer) continue;

            // ข้ามปลาที่ตายแล้ว
            if (!fish1.isAlive) continue;

            for (Map.Entry<Socket, Fish> entry2 : fishMap.entrySet()) {
                Fish fish2 = entry2.getValue();
                Socket socket2 = entry2.getKey();

                // ข้ามที่เป็นผู้เล่น (ไม่ใช่ตัวเองกับเพื่อน)
                if (fish2.isPlayer) continue;

                // ถ้าอยู่ใกล้กันพอจะชนกัน
                if (isColliding(fish1, fish2)) {
                    fish1.eat(fish2);
                }

                if(!fish1.isAlive)
                    toRemove.add(socket1);
                if(!fish2.isAlive)
                    toRemove.add(socket2);
            }
        }

        for (Socket s : toRemove) {
            fishMap.remove(s);
        }
    }

    public static boolean isColliding(Fish a, Fish b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double collisionDistance = (a.size + b.size) / 2.0; // ปรับตามลักษณะการแสดงผล
        return distance < collisionDistance;
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
        try (InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                OutputStream output = socket.getOutputStream();) {
            writer = new PrintWriter(output, true);
            // writer.println("เชื่อมต่อกับ Server สำเร็จ");
            int currentPhase = gameState.getGamePhase();
            System.out.println("____SERVER SEND____, phase:" + currentPhase);
            writer.println("phase:" + currentPhase);
            writer.println("END:");

            String text;
            while ((text = reader.readLine()) != null) {
                currentPhase = gameState.getGamePhase();
                switch (currentPhase) {
                    case 0: // Lobby phase
                        // ถ้า client ส่งคำสั่งมาให้ server
                        if (text.equals("start")) {
                            gameState.setGamePhase(1); // Change phase เป็น in-game
                            GameServer.broadcastPhase(1);
                            System.err.println("เริ่มเกมแล้ว In-game phase {up, down, left, right, next}");
                            // writer.println("เริ่มเกมแล้ว");
                            // writer.println("In-game phase {up, down, left, right, next}");
                        } else {
                            System.err.println("Lobby phase {start}");
                            // writer.println("Lobby phase {start}");
                        }
                        break;

                    case 1: // In-game phase
                        // ถ้า client ส่งคำสั่งมาให้ server
                        Fish playerFish = GameServer.fishMap.get(socket);
                        if (playerFish != null && playerFish.isAlive) {
                            if (List.of("up", "down", "left", "right", "upleft", "upright", "downleft", "downright").contains(text)) {
                                playerFish.move(text);
                            }
                        }

                        if (text.equals("next")) { // จบเกม
                            gameState.setGamePhase(2); // เปลี่ยน phase แล้วทุก client เห็น
                            GameServer.broadcastPhase(2);
                            System.err.println("เกมจบแล้ว result phase {next}");
                            // writer.println("เกมจบแล้ว");
                            // writer.println("result phase {next}");
                        } else {
                            System.err.println(
                                    "In-game phase {up, down, left, right, upleft, upright, downleft, downright, next}");
                            // writer.println("In-game phase {up, down, left, right, next}");
                        }
                        break;

                    case 2: // Result phase
                        if (text.equals("next")) {
                            gameState.setGamePhase(0); // Return to Lobby phase
                            GameServer.broadcastPhase(0);
                            System.err.println("กลับไป Lobby phase {start}");
                            // writer.println("กลับไป Lobby แล้ว");
                            // writer.println("Lobby phase {start}");
                        } else {
                            System.err.println("result phase {next}");
                            // writer.println("result phase {next}");
                        }
                        break;
                }

                // writer.println("END");
            }

            // ปิดการเชื่อมต่อ
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

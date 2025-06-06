
import java.awt.Rectangle;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // setup variable
    final static int PORT = 1234;
    static int playerCount = 0;
    static GameState gameState;

    private static AtomicInteger nextPlayerNum = new AtomicInteger(1);
    private static Map<Socket, Integer> playerNumberMap = new ConcurrentHashMap<>();

    // hashmap เก็บปลาของ client + bot (รองรับการทำงานหลาย thread)
    public static Map<Socket, Fish> fishMap = new ConcurrentHashMap<>();

    // เก็บรายชื่อ client ที่เชื่อมต่อกับ server
    // ใช้ synchronized list เพื่อให้หลายเธรดใช้งานพร้อมกันได้อย่างปลอดภัย
    public static List<ClientHandler> handlers = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        // สร้าง GameState เพื่อเริ่มต้นแชร์สถานะเกมให้กับ client
        gameState = new GameState();

        // สร้าง ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server รันอยู่ที่พอร์ต " + PORT);

            // Thread สำหรับส่ง FishState ให้ client ทุกคนทุก ๆ 16ms
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                if (gameState.getGamePhase() != 1) {
                    return; // ถ้าไม่อยู่ใน phase in-game ให้ไม่ทำงาน

                }
                checkFishCollisions(); // ชนหมาย
                broadcastFishState(); // ขยับ
                checkIfAllPlayersAreDead(); // เช็คว่าผู้เล่นตายหมดหรือยัง
            }, 0, 16, TimeUnit.MILLISECONDS); // ทุก 16 ms เท่า client

            ScheduledExecutorService spawner = Executors.newScheduledThreadPool(1);
            spawner.scheduleAtFixedRate(() -> {
                if (gameState.getGamePhase() != 1) {
                    return; // ถ้าไม่อยู่ใน phase in-game ให้ไม่ทำงาน

                }
                float avgSize = (float) fishMap.values().stream()
                        .filter(fish -> fish.isAlive)
                        .mapToDouble(fish -> fish.size)
                        .average()
                        .orElse(25.0); // หากไม่มีปลาผู้เล่นเหลืออยู่
                float avgPlayerSize = (float) fishMap.values().stream()
                        .filter(fish -> fish.isPlayer)
                        .filter(fish -> fish.isAlive)
                        .mapToDouble(fish -> fish.size)
                        .average()
                        .orElse(25.0); // หากไม่มีปลาผู้เล่นเหลืออยู่
                float avgY = (float) fishMap.values().stream()
                        .filter(fish -> fish.isAlive)
                        .mapToDouble(fish -> fish.y)
                        .average()
                        .orElse(300.0);
                Fish enemy = Fish.spawnEnemyFish(avgPlayerSize, avgSize, avgY);
                fishMap.put(new Socket(), enemy); // <-- ต้องเปลี่ยนไปใช้ ID จริง
            }, 0, 1250 * (playerCount + 1), TimeUnit.MILLISECONDS);

            // รอ client เชื่อมต่อที่ ServerSocket
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("มี Client ใหม่เชื่อมต่อ");
                playerCount++;

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

            // ให้ปลา bot เคลื่อนไหว
            if (!f.isPlayer) {
                f.move(f.direction);
            }

            // ถ้าปลาตายหรือล้มเหลว (disconnect)
            if (!f.isAlive) {
                toRemove.add(socket);
                continue;
            }

            sb.append("id:").append(socket.hashCode())
                    .append(", x:").append(f.x)
                    .append(", y:").append(f.y)
                    .append(", size:").append(f.size)
                    .append(", isAlive:").append(f.isAlive)
                    .append(", score:").append(f.score)
                    .append(", isPlayer:").append(f.isPlayer)
                    .append(", playerNum:").append(f.playerNum)
                    .append("; ");
        }

        // ส่ง REMOVE ไปหา client ทุกคน ก่อนลบจริง
        for (Socket s : toRemove) {
            int fishId = s.hashCode();
            for (ClientHandler h : handlers) {
                h.send("REMOVE " + fishId);
            }
            fishMap.remove(s);
        }

        // ส่งสถานะทั้งหมดไปให้ client ทุกคน
        String data = "Fish:" + sb.toString();
        for (ClientHandler h : handlers) {
            h.send(data);
        }

    }

    // method สุ่ม สำหรับใช้สุ่มจุดเกิด
    public static int random(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    public static void broadcastPhase(int currentPhase) {
        for (ClientHandler h : handlers) {
            h.send("phase:" + currentPhase);

            if (currentPhase == 1) {
                if (GameServer.fishMap.get(h.socket) == null) {
                    // ใช้ playerNum เดิมถ้ามี หรือสร้างใหม่
                    int playerNum = playerNumberMap.computeIfAbsent(h.socket, k -> nextPlayerNum.getAndIncrement());

                    Fish playerFish = new Fish(GameServer.random(200, 600),
                            GameServer.random(300, 400),
                            25,
                            "right",
                            "player",
                            true,
                            playerNum);
                    GameServer.fishMap.put(h.socket, playerFish);
                }
            }
        }
    }

    public static void checkFishCollisions() {
        List<Socket> toRemove = new ArrayList<>();

        for (Map.Entry<Socket, Fish> entry1 : fishMap.entrySet()) {
            Fish fish1 = entry1.getValue();
            Socket socket1 = entry1.getKey();

            // ข้ามที่ไม่ใช้ผู้เล่น
            if (!fish1.isPlayer) {
                continue;
            }

            // ข้ามปลาที่ตายแล้ว
            if (!fish1.isAlive) {
                continue;
            }

            for (Map.Entry<Socket, Fish> entry2 : fishMap.entrySet()) {
                Fish fish2 = entry2.getValue();
                Socket socket2 = entry2.getKey();

                // ข้ามที่เป็นผู้เล่น (ไม่ใช่ตัวเองกับเพื่อน)
                if (fish2.isPlayer) {
                    continue;
                }

                // ถ้าอยู่ใกล้กันพอจะชนกัน
                if (isColliding(fish1, fish2)) {
                    fish1.eat(fish2);
                }

                if (!fish1.isAlive) {
                    toRemove.add(socket1);
                }
                if (!fish2.isAlive) {
                    toRemove.add(socket2);
                }
            }
        }

        for (Socket s : toRemove) {
            fishMap.remove(s);
        }
    }

    public static boolean isColliding(Fish a, Fish b) {
        int aWidth = (int) (a.size * 1.5);
        int aHeight = (int) (a.size);
        int bWidth = (int) (b.size * 1.5);
        int bHeight = (int) (b.size);
        // จุดศูนย์กลางปลา a
        double aCx = a.x + aWidth / 2.0;
        double aCy = a.y + aHeight / 2.0;

        // จุดศูนย์กลางปลา b
        double bCx = b.x + bWidth / 2.0;
        double bCy = b.y + bHeight / 2.0;

        // รัศมีแนวนอนและแนวตั้งของปลาแต่ละตัว
        double aRx = aWidth / 2.0;
        double aRy = aHeight / 2.0;
        double bRx = bWidth / 2.0;
        double bRy = bHeight / 2.0;

        double dx = aCx - bCx;
        double dy = aCy - bCy;

        // สูตรเช็ควงรีชนกัน (normalize ระยะห่างด้วยรัศมีรวม)
        double nx = dx / (aRx + bRx);
        double ny = dy / (aRy + bRy);

        boolean bodyCollide = (nx * nx + ny * ny) <= 1.0;

        // --- ตรวจสอบการชนของหาง ---
        // สร้าง hit box ของหางปลา a
        Rectangle aTailRect;
        if ("right".equals(a.direction)) {
            aTailRect = new Rectangle(
                    (int) (a.x - aWidth / 4),
                    (int) (a.y + aHeight / 4), //ตำแหน่งบนซ้ายของสี่เหลี่ยม
                    aWidth / 4,
                    aHeight / 2 //ความกว้างของหางเป็น 1/4 ของ width และความยาวของหางเป็น 1/2 ของ height
            );
        } else {
            aTailRect = new Rectangle(
                    (int) (a.x + aWidth),
                    (int) (a.y + aHeight / 4),
                    aWidth / 4,
                    aHeight / 2
            );
        }

        // สร้าง hit box ของหางปลา b
        Rectangle bTailRect;
        if ("right".equals(b.direction)) {
            bTailRect = new Rectangle(
                    (int) (b.x - bWidth / 4),
                    (int) (b.y + bHeight / 4),
                    bWidth / 4,
                    bHeight / 2
            );
        } else {
            bTailRect = new Rectangle(
                    (int) (b.x + bWidth),
                    (int) (b.y + bHeight / 4),
                    bWidth / 4,
                    bHeight / 2
            );
        }

        // ตรวจสอบว่า body หรือ tail ชนกัน
        boolean tailCollide = aTailRect.intersects(
                new Rectangle((int) b.x, (int) b.y, bWidth, bHeight) //เช็คว่า หางของปลา a ชนกับตัวปลา b
        ) || bTailRect.intersects(
                new Rectangle((int) a.x, (int) a.y, aWidth, aHeight) //เช็คว่า หางของปลา b ชนกับตัวปลา a
        );

        return bodyCollide || tailCollide;
    }

    private static void checkIfAllPlayersAreDead() {
        boolean anyAlive = fishMap.values().stream()
                .anyMatch(fish -> fish.isPlayer && fish.isAlive);

        if (!anyAlive) {
            System.out.println("All players are dead. Moving to next phase.");
            gameState.setGamePhase(2);
            broadcastPhase(2); // Change phase เป็น Result phase
        }
    }

}

class ClientHandler extends Thread {

    protected Socket socket;
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

    public void run() {
        int currentPhase = gameState.getGamePhase();

        try (
                InputStream input = socket.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input)); OutputStream output = socket.getOutputStream()) {
            writer = new PrintWriter(output, true);
            writer.println("phase:" + currentPhase);
            writer.println("END:");

            String text;
            while ((text = reader.readLine()) != null) {
                currentPhase = gameState.getGamePhase();
                switch (currentPhase) {
                    case 0: // Lobby phase
                        if (text.equals("start")) {
                            gameState.setGamePhase(1);
                            GameServer.fishMap = new ConcurrentHashMap<>(); // reset fishMap
                            GameServer.broadcastPhase(1);
                            System.err.println("เริ่มเกมแล้ว In-game phase {up, down, left, right, next}");
                        } else {
                            System.err.println("Lobby phase {start}");
                        }
                        break;

                    case 1: // In-game phase
                        Fish playerFish = GameServer.fishMap.get(socket);
                        if (playerFish != null && playerFish.isAlive) {
                            if (List.of("up", "down", "left", "right").contains(text)) {
                                playerFish.move(text);
                            }
                        }

                        if (text.equals("next")) {
                            gameState.setGamePhase(2);
                            GameServer.broadcastPhase(2);
                            System.err.println("เกมจบแล้ว result phase {next}");
                        }
                        break;

                    case 2: // Result phase
                        if (text.equals("next")) {
                            gameState.setGamePhase(0);
                            GameServer.broadcastPhase(0);
                            System.err.println("กลับไป Lobby phase {start}");
                        } else {
                            System.err.println("result phase {next}");
                        }
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + ", Socket=" + socket.getPort());
        } finally {
            // ======= เมื่อ client ออกจากระบบ =======
            int clientId = socket.hashCode();
            GameServer.fishMap.remove(socket);
            GameServer.handlers.remove(this);
            System.out.println("Client removed: ID=" + clientId + ", Socket=" + socket.getPort());

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

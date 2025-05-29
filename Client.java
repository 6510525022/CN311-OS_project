
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class Client {

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private JFrame frame;
    private JPanel buttonPanel;
    private JButton controlButton;
    private FishPanel fishPanel;

    private ConcurrentHashMap<Integer, Fish> fishHashMap = new ConcurrentHashMap<>();
    private int phase = 0;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // ใช้สำหรับตรวจสอบว่าปุ่มไหนถูกกดค้างอยู่ เพื่อให้เคลื่อนที่แบบเฉียงได้
    private Set<Integer> pressedKeys = new HashSet<>();

    public static void main(String[] args) {
        String host = "26.158.112.66";
        int PORT = 1234;

        try (Socket socket = new Socket(host, PORT)) {
            Client client = new Client(socket);
            client.setupGUI();
            client.listenToServer();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void setupGUI() {
        frame = new JFrame("Fish Game Client");
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // สร้างปลา
        fishPanel = new FishPanel(fishHashMap);
        frame.add(fishPanel, BorderLayout.CENTER);

        buttonPanel = new JPanel();
        controlButton = new JButton("Start");

        controlButton.addActionListener(e -> {
            if (phase == 0) {
                sendMove("start");
                controlButton.setText("Start");
                phase = 1;
            } else if (phase == 1) {
                sendMove("next");
                controlButton.setText("End Game");
                phase = 2;
            } else if (phase == 2) {
                sendMove("next");
                controlButton.setText("Lobby");
                phase = 0;
            }
        });

        buttonPanel.add(controlButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        setupKeyBindings(frame.getRootPane());
        setupMovementTimer();

        frame.setFocusable(true);
        frame.requestFocusInWindow();
        frame.setVisible(true);
    }

    private void listenToServer() {
        try {
            String responseLine;
            System.out.println("Listening to server...");
            while (!(responseLine = reader.readLine()).equals("END")) {

                if (responseLine.startsWith("REMOVE ")) {
                    int idToRemove = Integer.parseInt(responseLine.substring(7));
                    fishHashMap.remove(idToRemove);
                    fishPanel.repaint();
                    continue;
                }

                if (responseLine.startsWith("phase:")) {
                    int newPhase = Integer.parseInt(responseLine.split(":")[1]);
                    updatePhaseFromServer(newPhase);
                }

                if (responseLine.startsWith("Fish:")) {
                    String data = responseLine.substring("Fish:".length());
                    String[] fishEntries = data.split(";");

                    //set ID ปลาที่รับมาทั้งหมดจาก Server
                    Set<Integer> receivedIds = new HashSet<>();

                    for (String fishEntry : fishEntries) {
                        fishEntry = fishEntry.trim();
                        if (fishEntry.isEmpty()) {
                            continue;
                        }

                        String[] attributes = fishEntry.split(", ");
                        int id = -1, playerNum = 0;
                        float x = 0, y = 0, size = 0, score = 0;
                        boolean isPlayer = false;
                        for (String attr : attributes) {
                            //สร้าง Array 2 ช่อง
                            String[] keyValue = attr.split(":");
                            if (keyValue.length != 2) {
                                continue;
                            }

                            String key = keyValue[0];
                            String value = keyValue[1];

                            switch (key) {
                                case "id" ->
                                    id = Integer.parseInt(value);
                                case "x" ->
                                    x = Float.parseFloat(value);
                                case "y" ->
                                    y = Float.parseFloat(value);
                                case "size" ->
                                    size = Float.parseFloat(value);
                                case "score" ->
                                    score = Float.parseFloat(value);
                                case "isPlayer" ->
                                    isPlayer = Boolean.parseBoolean(value);
                                case "playerNum" -> {
                                    playerNum = Integer.parseInt(value);
                                }
                            }
                        }

                        if (id != -1) {
                            receivedIds.add(id);

                            if (!fishHashMap.containsKey(id)) {
                                Fish newFish = new Fish(x, y, size, "right", "normal", isPlayer, playerNum);
                                newFish.score = score;
                                newFish.playerNum = playerNum;
                                fishHashMap.put(id, newFish);
                            } else {
                                updateFishFromServer(id, x, y, size, score, playerNum);
                                Fish fish = fishHashMap.get(id);
                                if (fish != null && fish.isPlayer != isPlayer) {
                                    fish.isPlayer = isPlayer;
                                }
                            }
                        }
                    }

                    //ลบปลาที่อยู่ใน fishHashMap แต่ไม่อยู่ใน receiveIds แล้ว (กรณี disconnect)
                    fishHashMap.keySet().removeIf(existingId -> !receivedIds.contains(existingId));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePhaseFromServer(int newPhase) {
        this.phase = newPhase;
        System.out.println("Server Phase updated to: " + phase);

        switch (phase) {
            case 0 ->
                controlButton.setText("Start");
            case 1 ->
                controlButton.setText("End Game");
            case 2 ->
                controlButton.setText("Lobby");
        }

        fishPanel.setPhase(phase);

        frame.requestFocusInWindow();
    }

    private void updateFishFromServer(int id, float newX, float newY, float newSize, float newScore, int playerNum) {
        Fish fish = fishHashMap.get(id);
        if (fish == null) {
            return;
        }

        if (fish.x != newX) {
            fish.direction = newX > fish.x ? "right" : "left";
            fish.x = newX;
        }

        if (fish.y != newY) {
            fish.y = newY;
        }
        if (fish.size != newSize) {
            fish.size = newSize;
        }

        if (fish.score != newScore) {
            fish.score = newScore;
        }

        if (fish.playerNum != playerNum) {
            fish.playerNum = playerNum;
        }

        fishPanel.repaint();
    }

    private void sendMove(String msg) {
        writer.println(msg);
    }

    //เชื่อมปุ่มกด (W, A, S, D) กับระบบของเกม เพื่อบอกว่าปุ่มใดกดอยู่
    private void setupKeyBindings(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        int[] keys = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D};

        for (int keyCode : keys) {
            String keyPressed = "pressed " + keyCode;
            String keyReleased = "released " + keyCode;

            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), keyPressed);
            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), keyReleased);

            actionMap.put(keyPressed, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pressedKeys.add(keyCode);
                }
            });

            actionMap.put(keyReleased, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pressedKeys.remove(keyCode);
                }
            });
        }
    }

    // ส่งการกดคีย์ทุกๆ 16 ms เพื่อทำให้ปลาขยับ
    private void setupMovementTimer() {
        javax.swing.Timer movementTimer = new javax.swing.Timer(16, e -> {
            if (phase != 1) {
                return;
            }

            if (pressedKeys.contains(KeyEvent.VK_W)) {
                sendMove("up");
            }
            if (pressedKeys.contains(KeyEvent.VK_S)) {
                sendMove("down");
            }
            if (pressedKeys.contains(KeyEvent.VK_A)) {
                sendMove("left");
            }
            if (pressedKeys.contains(KeyEvent.VK_D)) {
                sendMove("right");
            }
        });
        movementTimer.start();
    }
}

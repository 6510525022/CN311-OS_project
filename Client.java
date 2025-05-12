import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private JFrame frame;
    private JPanel buttonPanel;
    private JButton controlButton;
    private FishPanel fishPanel;

    private HashMap<Integer, Fish> fishHashMap = new HashMap<>();
    private int phase = 0;

    // ใช้สำหรับตรวจสอบว่าปุ่มไหนถูกกดค้างอยู่ เพื่อให้เคลื่อนที่แบบเฉียงได้
    private Set<Integer> pressedKeys = new HashSet<>();

    public static void main(String[] args) {
        String host = "localhost";
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
        frame.setSize(800, 600);
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

        Set<Integer> pressedKeys = new HashSet<>();

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (phase != 1) return;

                pressedKeys.add(e.getKeyCode());
                sendMove(resolveDirection());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }

            private String resolveDirection() {
                boolean up = pressedKeys.contains(KeyEvent.VK_W);
                boolean down = pressedKeys.contains(KeyEvent.VK_S);
                boolean left = pressedKeys.contains(KeyEvent.VK_A);
                boolean right = pressedKeys.contains(KeyEvent.VK_D);

                if (up && down && right && left) return "";
                if (up && right && left) return "up";
                if (down && right && left) return "down";
                if (up && down && left) return "left";
                if (down && down && right) return "right";

                if (left && right) return ""; 
                if (up && down) return "";
                if (up && right) return "up-right";
                if (up && left) return "up-left";
                if (down && right) return "down-right";
                if (down && left) return "down-left";

                if (up) return "up";
                if (down) return "down";
                if (left) return "left";
                if (right) return "right";

                return "";
            }
        });

        frame.setFocusable(true);
        frame.requestFocusInWindow();
        frame.setVisible(true);
    }

    private void listenToServer() {
        try {
            String responseLine;
            System.out.println("Listening to server...");
            while (!(responseLine = reader.readLine()).equals("END")) {
                if (responseLine.startsWith("phase:")) {
                    int newPhase = Integer.parseInt(responseLine.split(":")[1]);
                    updatePhaseFromServer(newPhase);
                }
                if (responseLine.startsWith("Fish:")) {
                    String data = responseLine.substring("Fish:".length());
                    String[] fishEntries = data.split(";");

                    for (String fishEntry : fishEntries) {
                        fishEntry = fishEntry.trim();
                        if (fishEntry.isEmpty())
                            continue;

                        String[] attributes = fishEntry.split(", ");
                        int id = -1;
                        float x = 0, y = 0, size = 0;

                        for (String attr : attributes) {
                            String[] keyValue = attr.split(":");
                            if (keyValue.length != 2)
                                continue;

                            String key = keyValue[0];
                            String value = keyValue[1];

                            switch (key) {
                                case "id":
                                    id = Integer.parseInt(value);
                                    break;
                                case "x":
                                    x = Float.parseFloat(value);
                                    break;
                                case "y":
                                    y = Float.parseFloat(value);
                                    break;
                                case "size":
                                    size = Float.parseFloat(value);
                                    break;
                            }
                        }

                        if (!fishHashMap.containsKey(id)) {
                            Fish newFish = new Fish(x, y, size, "right", "normal", true);
                            fishHashMap.put(id, newFish);
                        } else {
                            updateFishFromServer(id, x, y, size);
                        }
                    }
                }

                if (responseLine.startsWith("result:")) {
                    String newStatus = responseLine.split(":")[1];
                    updateResultFromServer(newStatus);
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
            case 0 -> controlButton.setText("Start");
            case 1 -> controlButton.setText("End Game");
            case 2 -> controlButton.setText("Lobby");
        }

        frame.requestFocusInWindow();
    }

    private void updateFishFromServer(int id, float newX, float newY, float newSize) {
        Fish fish = fishHashMap.get(id);
        if (fish == null)
            return;

        if (!fish.isPlayer) {
            return;
        }

        if (fish.x != newX) {
            fish.direction = newX > fish.x ? "right" : "left";
            fish.x = newX;
        }

        if (fish.y != newY)
            fish.y = newY;
        if (fish.size != newSize)
            fish.size = newSize;

        fishPanel.repaint();
    }

    private void updateResultFromServer(String Result) {

    }

    private void sendMove(String msg) {
        writer.println(msg);
    }

    // function keyBinding สำหรับการจับคีย์
    private void setupKeyBindings(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        int[] keys = { KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D };

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

    // ส่งการกดคีย์ทุกๆ 20 ms
    private void setupMovementTimer() {
        javax.swing.Timer movementTimer = new javax.swing.Timer(20, e -> {
            if (phase != 1)
                return;

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

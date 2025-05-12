import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class Client {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private JFrame frame;
    private JPanel buttonPanel;
    private JButton controlButton;

    private int phase = 0;

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
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

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

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (phase != 1) return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> sendMove("up");
                    case KeyEvent.VK_S -> sendMove("down");
                    case KeyEvent.VK_A -> sendMove("left");
                    case KeyEvent.VK_D -> sendMove("right");
                }
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
                if (responseLine.startsWith("fish:")) {
                    String newStatus = responseLine.split(":")[1];
                    updateFishFromServer(newStatus);
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

    private void updateFishFromServer(String Fish) {
        
    }

    private void updateResultFromServer(String Result) {
        
    }

    private void sendMove(String msg) {
        writer.println(msg);
    }
}

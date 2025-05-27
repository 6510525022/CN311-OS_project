
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.Timer;

public class FishPanel extends JPanel {

    private ConcurrentHashMap<Integer, Fish> fishHashMap;
    private int phase = 0;

    private Map<Integer, Integer> playerNum = new HashMap<>();
    private Map<Integer, Double> playerScores = new HashMap<>();

    public FishPanel(ConcurrentHashMap<Integer, Fish> fishHashMap) {
        this.fishHashMap = fishHashMap;

        Timer animationTimer = new Timer(20, e -> repaint());
        animationTimer.start();
    }

    public void addFish(int id, Fish fish) {
        fishHashMap.put(id, fish);
        if (fish.isPlayer) {
            playerScores.put(id, fish.score);
            playerNum.put(id, fish.playerNum);
        }
    }

    public void removeFish(int id) {
        System.out.println("[removeFish] Removing fish id: " + id);
        fishHashMap.remove(id);

        if (playerScores.containsKey(id)) {
            playerScores.remove(id);
            playerNum.remove(id);
            System.out.println("[removeFish] Removed player data for id: " + id);
        }

        repaint();
    }

    public void updateFishMap(ConcurrentHashMap<Integer, Fish> newFishMap) {
        fishHashMap.clear();
        fishHashMap.putAll(newFishMap);

        if (phase == 1) {
            cleanDisconnectedPlayers(); 
        }

        syncPlayerDataWithFishMap();
        repaint();
    }

    public void updatePlayerScore(int id, double newScore) {
        playerScores.put(id, newScore);
    }

    public void syncPlayerDataWithFishMap() {
        System.out.println("[syncPlayerDataWithFishMap] Before sync playerScores: " + playerScores);
        System.out.println("[syncPlayerDataWithFishMap] fishHashMap keys: " + fishHashMap.keySet());

        for (Integer id : fishHashMap.keySet()) {
            Fish fish = fishHashMap.get(id);
            if (fish.isPlayer && !playerScores.containsKey(id)) {
                playerScores.put(id, fish.score);
                playerNum.put(id, fish.playerNum);
                System.out.println("[syncPlayerDataWithFishMap] Added new active player id: " + id);
            }
        }

        System.out.println("[syncPlayerDataWithFishMap] After sync playerScores: " + playerScores);
    }

    public void updateFishPosition(int id, float newX, float newY, float newSize) {
        Fish fish = fishHashMap.get(id);
        if (fish != null) {
            fish.x = newX;
            fish.y = newY;
            fish.size = newSize;
            repaint();
        }
    }

    public void setPhase(int phase) {
        this.phase = phase;

        if (phase == 2) {
            System.out.println("[setPhase] Game Over phase detected. Skipping sync to keep playerScores intact.");
        } else if (phase == 0) {
            playerNum = new HashMap<>();
            playerScores = new HashMap<>();
        }

        repaint();
    }

    private void cleanDisconnectedPlayers() {
        if (phase != 1) {
            return; 
        }
        Set<Integer> currentFishIds = fishHashMap.keySet();

        Iterator<Integer> scoreIt = playerScores.keySet().iterator();
        while (scoreIt.hasNext()) {
            int id = scoreIt.next();
            if (!currentFishIds.contains(id)) {
                scoreIt.remove();
                System.out.println("[cleanDisconnectedPlayers] Removed playerScore for id: " + id);
            }
        }

        Iterator<Integer> idIt = playerNum.keySet().iterator();
        while (idIt.hasNext()) {
            int id = idIt.next();
            if (!currentFishIds.contains(id)) {
                idIt.remove();
                System.out.println("[cleanDisconnectedPlayers] Removed playerIdToNumber for id: " + id);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // background สีฟ้าอ่อน
        g2d.setColor(new Color(135, 206, 250));  // Sky Blue
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // วาดสาหร่าย (seaweed) ที่พื้น (สมมติฐานอยู่ด้านล่าง panel)
        g2d.setColor(new Color(34, 139, 34)); // สีเขียวเข้ม (forest green)
        int seaweedBaseY = getHeight() - 30;  // ตำแหน่งฐานสาหร่ายด้านล่าง

        // วาดสาหร่ายหลายต้นเป็นเส้นโค้ง
        for (int i = 20; i < getWidth(); i += 40) {
            // วาดเส้นโค้งง่าย ๆ ด้วย cubic curve (หรือใช้ drawPolyline)
            int[] xPoints = {i, i - 5, i + 5, i - 5, i + 5, i};
            int[] yPoints = {seaweedBaseY, seaweedBaseY - 20, seaweedBaseY - 40, seaweedBaseY - 60, seaweedBaseY - 80, seaweedBaseY - 100};
            g2d.setStroke(new BasicStroke(3));  // ความหนาเส้น
            g2d.drawPolyline(xPoints, yPoints, xPoints.length);
        }

        // ปริ้นไอดีปลา player
        System.out.print("Rendering player fish ids: ");
        for (Map.Entry<Integer, Fish> entry : fishHashMap.entrySet()) {
            if (entry.getValue().isPlayer) {
                System.out.print(entry.getKey() + " ");
            }
        }
        System.out.println();

        if (phase == 1 || phase == 2) {
            for (Fish fish : fishHashMap.values()) {
                if (fish.isAlive) {
                    drawFish(g, fish);
                }
            }
        }

        if (phase == 1) {
            drawScores(g);
        } else if (phase == 0) {
            drawStartScreen(g);
        } else if (phase == 2) {
            drawGameOver(g);
        }
    }

    private void drawFish(Graphics g, Fish fish) {
        int fishWidth = (int) (fish.size * 1.5);
        int fishHeight = (int) (fish.size);
        int x = (int) fish.x;
        int y = (int) fish.y;

        Graphics2D g2d = (Graphics2D) g;

        // วาดตัวปลา สีชมพู
        g2d.setColor(Color.PINK);
        g2d.fillOval(x, y, fishWidth, fishHeight);

        // วาดขอบดำรอบปลา
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x, y, fishWidth, fishHeight);

        // วาดหางปลา (tail) แบบเดิม แต่เพิ่มขอบดำ
        int[] tailX, tailY;
        if ("right".equals(fish.direction)) {
            tailX = new int[]{x, x - fishWidth / 4, x - fishWidth / 4};
            tailY = new int[]{y + fishHeight / 2, y, y + fishHeight};
        } else {
            tailX = new int[]{x + fishWidth, x + fishWidth + fishWidth / 4, x + fishWidth + fishWidth / 4};
            tailY = new int[]{y + fishHeight / 2, y, y + fishHeight};
        }

        g2d.fillPolygon(tailX, tailY, 3);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(tailX, tailY, 3);

        // วาดตาดำกับขอบตา (eye)
        int eyeSize = fishWidth / 5;
        int eyeX = x + fishWidth / 3;
        int eyeY = y + fishHeight / 3;

        if ("right".equals(fish.direction)) {
            eyeX += fishWidth / 3;
        } else {
            eyeX -= fishWidth / 3;
        }

        g2d.fillOval(eyeX, eyeY, eyeSize, eyeSize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(eyeX, eyeY, eyeSize, eyeSize);
    }

    private void drawScores(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        int y = 25;
        int x = 10;
        int lineHeight = g.getFontMetrics().getHeight();

        List<Map.Entry<Integer, Fish>> entries = new ArrayList<>(fishHashMap.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<Integer, Fish> entry : entries) {
            Fish fish = entry.getValue();
            if (fish.isPlayer) {
                int id = entry.getKey();
                int playerID= fish.playerNum;
                String text = "Player " + playerID + ", Score: " + (int) fish.score;
                g.drawString(text, x, y);
                y += lineHeight;
                playerScores.put(id, fish.score);
                playerNum.put(id, fish.playerNum);
            }
        }
    }

    private void drawStartScreen(Graphics g) {
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "Fish Game Client";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (getWidth() - fm.stringWidth(title)) / 2;
        int titleY = getHeight() / 2 - 30;
        g.drawString(title, titleX, titleY);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String msg = "Please press Start to begin the game";
        int msgX = (getWidth() - g.getFontMetrics().stringWidth(msg)) / 2;
        int msgY = getHeight() / 2 + 20;
        g.drawString(msg, msgX, msgY);
    }

    private void drawGameOver(Graphics g) {
        System.out.println("[drawGameOver] Final playerScores: " + playerScores);

        g.setColor(Color.BLACK.darker());
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String message = "Game Over";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2 - 60;
        g.drawString(message, x, y);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        int lineHeight = g.getFontMetrics().getHeight();
        int scoreY = y + 50;

        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(playerScores.entrySet());
        sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        for (Map.Entry<Integer, Double> entry : sorted) {
            int id = entry.getKey();
            double score = entry.getValue();
            Integer pNum = playerNum.get(id);
            String text = "Player " + (pNum != null ? pNum : id) + ", Final Score: " + (int) score;

            int textWidth = g.getFontMetrics().stringWidth(text);
            int scoreX = (getWidth() - textWidth) / 2;

            g.setColor(new Color(255, 200, 200, 180));
            g.fillRoundRect(scoreX - 8, scoreY - fm.getAscent(), textWidth + 16, lineHeight + 4, 10, 10);
            g.setColor(Color.BLACK.darker());
            g.drawString(text, scoreX, scoreY);

            scoreY += lineHeight + 10;
        }
    }
}

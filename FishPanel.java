
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class FishPanel extends JPanel {

    private ConcurrentHashMap<Integer, Fish> fishHashMap;
    private int phase = 0; // ใช้ควบคุมสถานะของเกม (0 = เริ่ม, 1 = เล่นอยู่, 2 = จบเกม)

    private Map<Integer, Integer> playerNum = new HashMap<>();    // เก็บ playerId → หมายเลข player
    private Map<Integer, Double> playerScores = new HashMap<>();  // เก็บ playerId → score

    public FishPanel(ConcurrentHashMap<Integer, Fish> fishHashMap) {
        this.fishHashMap = fishHashMap;

        // ตั้ง timer ให้ repaint() ทุก 20 มิลลิวินาที สำหรับ animation
        javax.swing.Timer animationTimer = new javax.swing.Timer(16, e -> repaint());
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
        fishHashMap.remove(id);
        playerScores.remove(id);
        playerNum.remove(id);
        repaint();
    }

    public void updateFishMap(ConcurrentHashMap<Integer, Fish> newFishMap) {
        fishHashMap.clear();
        fishHashMap.putAll(newFishMap);

        if (phase == 1) {
            cleanDisconnectedPlayers(); // ลบข้อมูล player ที่หลุดออกจากเกม
        }

        syncPlayerDataWithFishMap(); // อัปเดต playerScores และ playerNum ให้สอดคล้องกับ fish ใน map
        repaint();
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

    public void updatePlayerScore(int id, double newScore) {
        playerScores.put(id, newScore);
    }

    public void setPhase(int phase) {
        this.phase = phase;

        if (phase == 0) {
            // เริ่มต้นใหม่ ล้างข้อมูลทั้งหมด
            playerNum.clear();
            playerScores.clear();
        }

        repaint();
    }

    // ซิงค์ข้อมูล player ที่อยู่ใน fishHashMap แต่ยังไม่มีใน playerScores และ playerNum
    private void syncPlayerDataWithFishMap() {
        for (Integer id : fishHashMap.keySet()) {
            Fish fish = fishHashMap.get(id);
            if (fish.isPlayer && !playerScores.containsKey(id)) {
                playerScores.put(id, fish.score);
                playerNum.put(id, fish.playerNum);
            }
        }
    }

    // ลบข้อมูลของ player ที่หลุดออกจากเกม
    private void cleanDisconnectedPlayers() {
        Set<Integer> currentFishIds = fishHashMap.keySet();

        playerScores.keySet().removeIf(id -> !currentFishIds.contains(id));
        playerNum.keySet().removeIf(id -> !currentFishIds.contains(id));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // วาดพื้นหลังสีฟ้าอ่อน
        g2d.setColor(new Color(135, 206, 250));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // วาด seaweed
        drawSeaweed(g2d);

        // วาดปลา
        if (phase == 1 || phase == 2) {
            for (Fish fish : fishHashMap.values()) {
                if (fish.isAlive) {
                    drawFish(g, fish);
                }
            }
        }

        if (phase == 0) {
            drawStartScreen(g);
        } else if (phase == 1) {
            drawScores(g);
        } else if (phase == 2) {
            drawGameOver(g);
        }
    }

    private void drawSeaweed(Graphics2D g2d) {
        g2d.setColor(new Color(34, 139, 34));
        int seaweedBaseY = getHeight() - 30;
        for (int i = 20; i < getWidth(); i += 40) {
            int[] xPoints = {i, i - 5, i + 5, i - 5, i + 5, i};
            int[] yPoints = {seaweedBaseY, seaweedBaseY - 20, seaweedBaseY - 40, seaweedBaseY - 60, seaweedBaseY - 80, seaweedBaseY - 100};
            g2d.setStroke(new BasicStroke(3));
            g2d.drawPolyline(xPoints, yPoints, xPoints.length);
        }
    }

    // วาดปลาแต่ละตัว
    private void drawFish(Graphics g, Fish fish) {
        int fishWidth = (int) (fish.size * 1.5);
        int fishHeight = (int) fish.size;
        int x = (int) fish.x;
        int y = (int) fish.y;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.PINK);
        g2d.fillOval(x, y, fishWidth, fishHeight);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x, y, fishWidth, fishHeight);

        // วาดหางปลา
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

        // วาดตา
        int eyeSize = fishWidth / 5;
        int eyeX = x + fishWidth / 3;
        int eyeY = y + fishHeight / 3;
        eyeX += "right".equals(fish.direction) ? fishWidth / 3 : -fishWidth / 3;

        g2d.fillOval(eyeX, eyeY, eyeSize, eyeSize);
        g2d.drawOval(eyeX, eyeY, eyeSize, eyeSize);
    }

    // วาดคะแนนของผู้เล่นใน phase เล่นเกม
    private void drawScores(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        int y = 25;
        int x = 10;
        int lineHeight = g.getFontMetrics().getHeight();

        for (Map.Entry<Integer, Fish> entry : fishHashMap.entrySet()) {
            Fish fish = entry.getValue();
            if (fish.isPlayer) {
                int id = entry.getKey();
                String text = "Player " + fish.playerNum + ", Score: " + (int) fish.score;
                g.drawString(text, x, y);
                y += lineHeight;

                // sync ค่า score ล่าสุดเข้า playerScores
                playerScores.put(id, fish.score);
                playerNum.put(id, fish.playerNum);
            }
        }
    }

    // วาดหน้าจอตอนเริ่มเกม
    private void drawStartScreen(Graphics g) {
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "Fish Game";
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

    // วาดหน้าจอจบเกมพร้อมสรุปอันดับคะแนน
    private void drawGameOver(Graphics g) {
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

        int rank = 0;
        int prevScore = -1;
        for (Map.Entry<Integer, Double> entry : sorted) {
            int id = entry.getKey();
            double score = entry.getValue();
            Integer pNum = playerNum.get(id);

            if ((int) score != prevScore) {
                rank++;
            }

            String text = "(" + rank + ") Player " + (pNum != null ? pNum : id) + ", Final Score: " + (int) score;
            int textWidth = g.getFontMetrics().stringWidth(text);
            int scoreX = (getWidth() - textWidth) / 2;

            g.setColor(new Color(255, 200, 200, 180));
            g.fillRoundRect(scoreX - 8, scoreY - fm.getAscent(), textWidth + 16, lineHeight + 4, 10, 10);
            g.setColor(Color.BLACK.darker());
            g.drawString(text, scoreX, scoreY);

            scoreY += lineHeight + 10;
            prevScore = (int) score;
        }
    }
}

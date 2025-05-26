
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class FishPanel extends JPanel {

    private ConcurrentHashMap<Integer, Fish> fishHashMap;
    private int phase = 0;
    private Map<Integer, Integer> playerIdToNumber = new HashMap<>();
    private int nextPlayerNumber = 1;

    // Map เก็บคะแนนของแต่ละ player แยกต่างหาก (id -> score)
    private Map<Integer, Double> playerScores = new HashMap<>();

    public FishPanel(ConcurrentHashMap<Integer, Fish> fishHashMap) {
        this.fishHashMap = fishHashMap;

        Timer animationTimer = new Timer(20, e -> repaint());
        animationTimer.start();
    }

    public void addFish(int id, Fish fish) {
        fishHashMap.put(id, fish);
        if (fish.isPlayer && !playerIdToNumber.containsKey(id)) {
            playerIdToNumber.put(id, nextPlayerNumber++);
        }
        // เก็บคะแนนเริ่มต้น
        if (fish.isPlayer) {
            playerScores.put(id, fish.score);
        }
    }

    public void setPhase(int phase) {
        this.phase = phase;
        repaint();
    }

    // ฟังก์ชันสำหรับอัพเดตคะแนนแยกเมื่อปลาได้คะแนน (เช่นตอนกิน)
    public void updatePlayerScore(int id, double newScore) {
        playerScores.put(id, newScore);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // วาดปลาใน phase 1 หรือ 2
        if (phase == 1 || phase == 2) {
            for (Fish fish : fishHashMap.values()) {
                if (fish.isAlive) {
                    int fishWidth = (int) (fish.size * 1.5);
                    int fishHeight = (int) (fish.size);

                    int x = (int) fish.x;
                    int y = (int) fish.y;

                    g.setColor(Color.PINK);
                    g.fillOval(x, y, fishWidth, fishHeight);

                    int[] tailX, tailY;

                    if ("right".equals(fish.direction)) {
                        tailX = new int[]{x, x - fishWidth / 4, x - fishWidth / 4};
                        tailY = new int[]{y + fishHeight / 2, y, y + fishHeight};
                    } else {
                        tailX = new int[]{x + fishWidth, x + fishWidth + fishWidth / 4, x + fishWidth + fishWidth / 4};
                        tailY = new int[]{y + fishHeight / 2, y, y + fishHeight};
                    }

                    g.fillPolygon(tailX, tailY, 3);

                    g.setColor(Color.BLACK);
                    int eyeSize = fishWidth / 5;
                    int eyeX = x + fishWidth / 3;
                    int eyeY = y + fishHeight / 3;

                    if ("right".equals(fish.direction)) {
                        eyeX += fishWidth / 3;
                    } else if ("left".equals(fish.direction)) {
                        eyeX -= fishWidth / 3;
                    }

                    g.fillOval(eyeX, eyeY, eyeSize, eyeSize);
                }
            }
        }

        // แสดงคะแนนระหว่าง phase 1 (ดึงจาก fish.score)
        if (phase == 1) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 18));

            int lineHeight = g.getFontMetrics().getHeight();
            int x = 10;
            int y = 25;

            for (Map.Entry<Integer, Fish> entry : fishHashMap.entrySet()) {
                Fish fish = entry.getValue();
                if (fish.isPlayer) {
                    Integer playerNumber = playerIdToNumber.get(entry.getKey());
                    if (playerNumber == null) {
                        playerNumber = nextPlayerNumber++;
                        playerIdToNumber.put(entry.getKey(), playerNumber);
                    }
                    String playerScore = "Player " + playerNumber + ", Score: " + (int) fish.score;
                    g.drawString(playerScore, x, y);
                    y += lineHeight;
                    // อัพเดตคะแนนสำรองทุกครั้ง
                    playerScores.put(entry.getKey(), fish.score);
                }
            }
        } else if (phase == 0) {
            g.setColor(Color.BLUE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String gameTitle = "Fish Game Client";
            FontMetrics fmTitle = g.getFontMetrics();
            int titleX = (getWidth() - fmTitle.stringWidth(gameTitle)) / 2;
            int titleY = getHeight() / 2 - 30;
            g.drawString(gameTitle, titleX, titleY);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            String message = "Please press Start to begin the game";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 + 20;
            g.drawString(message, x, y);

        }

        if (phase == 2) {
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

            if (playerScores.isEmpty()) {
                System.out.println("No players found to display scores at phase 2");
            } else {
                List<Map.Entry<Integer, Double>> sortedScores = new ArrayList<>(playerScores.entrySet());
                sortedScores.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                Double prevScore = null;

                for (Map.Entry<Integer, Double> entry : sortedScores) {
                    Integer playerId = entry.getKey();
                    Double score = entry.getValue();

                    Integer playerNumber = playerIdToNumber.get(playerId);
                    if (playerNumber == null) {
                        playerNumber = nextPlayerNumber++;
                        playerIdToNumber.put(playerId, playerNumber);
                    }

                    String displayText;
                    if (prevScore != null && score.equals(prevScore)) {
                        displayText = "draw - Player " + playerNumber + ", Final Score: " + (int) Math.round(score);
                    } else {
                        displayText = "Player " + playerNumber + ", Final Score: " + (int) Math.round(score);
                    }

                    FontMetrics fmScore = g.getFontMetrics();
                    int textWidth = fmScore.stringWidth(displayText);
                    int scoreX = (getWidth() - textWidth) / 2;

                    Color bgColor = new Color(255, 200, 200, 180);
                    g.setColor(bgColor);
                    g.fillRoundRect(scoreX - 8, scoreY - fmScore.getAscent(), textWidth + 16, lineHeight + 4, 10, 10);

                    g.setColor(Color.BLACK.darker());
                    g.drawString(displayText, scoreX, scoreY);

                    scoreY += lineHeight + 10;
                    prevScore = score;
                }
            }
        }

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
}

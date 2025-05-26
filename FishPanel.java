
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class FishPanel extends JPanel {

    private HashMap<Integer, Fish> fishHashMap;
    private int phase = 0;  // เพิ่มตัวแปร phase
    Map<Integer, Integer> playerIdToNumber = new HashMap<>();
    private int nextPlayerNumber = 1;

    public FishPanel(HashMap<Integer, Fish> fishHashMap) {
        this.fishHashMap = fishHashMap;

        Timer animationTimer = new Timer(20, e -> repaint());
        animationTimer.start();
    }

    public void addFish(int id, Fish fish) {
        fishHashMap.put(id, fish);
        if (fish.isPlayer && !playerIdToNumber.containsKey(id)) {
            playerIdToNumber.put(id, nextPlayerNumber++);
        }
    }

    // เพิ่ม setter สำหรับอัพเดต phase
    public void setPhase(int phase) {
        this.phase = phase;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

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
                    if (playerNumber != null) {
                        String playerScore = "Player " + playerNumber + ", Score: " + (int) fish.score;
                        g.drawString(playerScore, x, y);
                        y += lineHeight;
                    } else {
                        playerNumber = nextPlayerNumber++;
                        playerIdToNumber.put(entry.getKey(), playerNumber);
                        String playerScore = "Player " + playerNumber + ", Score: " + (int) fish.score;
                        g.drawString(playerScore, x, y);
                        y += lineHeight;
                    }
                }
            }
        }

        if (phase == 0) {
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

        } else if (phase == 2) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 24));

            String message = "Game Over";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2 + 20;
            g.drawString(message, x, y);

            //แสดง score
        } else {
            for (Fish fish : fishHashMap.values()) {
                if (fish.isAlive) {
                    int fishWidth = (int) (fish.size * 1.5);
                    int fishHeight = (int) (fish.size);

                    int x = (int) fish.x;
                    int y = (int) fish.y;

                    g.setColor(Color.PINK);
                    g.fillOval(x, y, fishWidth, fishHeight);

                    g.setColor(Color.PINK);
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

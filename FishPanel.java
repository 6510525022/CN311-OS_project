import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class FishPanel extends JPanel {
    private HashMap<Integer, Fish> fishHashMap;

    public FishPanel(HashMap<Integer, Fish> fishHashMap) {
        this.fishHashMap = fishHashMap;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Fish fish : fishHashMap.values()) {
            if (fish.isAlive && fish.isPlayer) {
                int fishWidth = (int) (fish.size * 1.5);
                int fishHeight = (int) (fish.size);

                int x = (int) fish.x;
                int y = (int) fish.y;

                // วาดตัวปลา
                g.setColor(Color.BLUE);
                g.fillOval(x, y, fishWidth, fishHeight);

                // วาดหางปลา
                g.setColor(Color.CYAN);
                int[] tailX, tailY;

                if ("right".equals(fish.direction)) {
                    tailX = new int[] { x, x - fishWidth / 4, x - fishWidth / 4 };
                    tailY = new int[] { y + fishHeight / 2, y, y + fishHeight };
                } else { // left
                    tailX = new int[] { x + fishWidth, x + fishWidth + fishWidth / 4, x + fishWidth + fishWidth / 4 };
                    tailY = new int[] { y + fishHeight / 2, y, y + fishHeight };
                }

                g.fillPolygon(tailX, tailY, 3);

                // วาดตาปลา
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

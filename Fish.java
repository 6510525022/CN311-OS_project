
public class Fish {

    final static double step = 6;
    public double x, y, size, speed = 1;
    public String direction, type;
    public boolean isPlayer, isAlive;
    public int playerNum;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    public double score;

    Fish(float x, float y, float size, String direction, String type, boolean isPlayer, int playerNum) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.size = size;
        this.type = type;
        this.isPlayer = isPlayer;
        this.isAlive = true;
        this.score = 0;
        this.playerNum = playerNum;
    }

    Fish(float x, float y, float size, String direction, String type) {
        this(x, y, size, direction, type, false, 0);
    }

    // ฟังก์ชันสำหรับการเคลื่อนไหวของปลา
    public void move(String di) {
        if (this.isAlive) {
            switch (di) {
                case "up":
                    if (!(this.y < -size / 2)) {
                        this.y -= step * speed * 0.85;
                    }
                    break;
                case "down":
                    if (!(this.y > HEIGHT - size / 2 - 75)) {
                        this.y += step * speed * 0.85;
                    }
                    break;
                case "left":
                    if (!(this.x < -size / 2) || !this.isPlayer) {
                        if (this.x < 0 - WIDTH * 0.5) {
                            this.isAlive = false;
                        }
                        this.x -= step * 1.10 * speed * 0.85;
                    }
                    break;
                case "right":
                    if (!(this.x > WIDTH - size) || !this.isPlayer) {
                        if (this.x > WIDTH * 1.5) {
                            this.isAlive = false;
                        }
                        this.x += step * 1.10 * speed * 0.85;
                    }
                    break;
            }
        }
    }

    // ฟังก์ชันสำหรับการกินปลาอื่น
    public boolean eat(Fish other) {
        if (this.size > other.size) { // ใหญ่กว่ากินได้
            this.size += (Math.sqrt(other.size) / 5.0) + ((other.size / this.size) * 1.1); // เราใหญ่ขึ้น
            this.speed = (-0.0008 * this.size) + 1.12; // size 25 -> 1.10, size 150 -> 1.0
            this.score += other.size;
            other.isAlive = false; // อีกตัวตาย
            return true;
        } else if (this.size < other.size) { // เล็กกว่ากินไม่ได้
            this.isAlive = false; // เราตาย
            return false;
        }
        this.isAlive = false; // เราตาย
        other.isAlive = false; // อีกตัวตาย
        return false;
    }

    public static Fish spawnEnemyFish(float avgPlayerSize, float avgSize, float avgY) {
        boolean spawnLeft = Math.random() < 0.5;
        float size;
        if (avgPlayerSize < 70) {
            if (avgPlayerSize < avgSize) {
                size = (float) (Math.random() * (avgPlayerSize * 1.10) + 5.0); // 5 - 1.10xavgSize
            } else {
                size = (float) (Math.random() * (avgPlayerSize * 2.00) + (avgPlayerSize * 0.50)); // 0.50x - 2.50xavgSize
            }
        } else {
            if (avgPlayerSize * 0.90 < avgSize) {
                size = (float) (Math.random() * (avgPlayerSize * 0.90) + 5.0); // 5 - 0.90xavgSize
            } else {
                size = (float) (Math.random() * (avgPlayerSize * 0.45) + (avgPlayerSize * 0.95)); // 0.95x - 1.40xavgSize
            }
        }

        float midY = ((HEIGHT - size / 2 - 75) + (-size / 2)) / 2.0f;
        int y;
        if (avgY < midY - 230) {
            y = (int) (Math.random() * (HEIGHT - midY) + midY);
        } else if (avgY > midY + 230) {
            y = (int) (Math.random() * (midY));
        } else {
            y = (int) (Math.random() * (HEIGHT - size / 2 - 75) + (-size / 2));
        }

        int x = spawnLeft ? -200 : 950;
        String direction = spawnLeft ? "right" : "left";
        float speed = (float) (0.3 + (Math.random() * 0.9) + ((5.0 / size) * 0.2)); //x0.3-1.2 (+ 0.2 liner if ตัวเล็กสุด) 

        Fish f = new Fish(x, y, size, direction, "enemy");
        f.isPlayer = false;
        f.speed = speed;
        return f;
    }

}

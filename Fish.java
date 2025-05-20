public class Fish {
    final static double step = 6, stepCheng = 1.2;
    public double x, y, size, speed = 1;
    public String direction, type;
    public boolean isPlayer, isAlive;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    Fish(float x, float y, float size, String direction, String type, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.size = size;
        this.type = type;
        this.isPlayer = isPlayer;
        this.isAlive = true;
    }

    Fish(float x, float y, float size, String direction, String type) {
        this(x, y, size, direction, type, false);
    }

    public boolean isSmash(Fish other) {
        if (this.isAlive && other.isAlive) {
            // คำนวณการชนโดยพิจารณาจากตำแหน่งและขนาด
            double distance = Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2)); //คำนวณระยะห่าง
            if (distance < (this.size / 2 + other.size / 2)) { // ห่าง < อ้วนชนกัน?
                if (this.eat(other)) { // ถ้าเรากินได้
                    return true; // เราไปต่อ
                } else { // ถ้าเรากินไม่ได้
                    return false; // เราตาย
                }
            }
        }
        return true; // ถ้าไม่มีการชนหรือเรายังมีชีวิต
    }

    // ฟังก์ชันสำหรับการเคลื่อนไหวของปลา
    public void move(String di) {
        if (this.isAlive) {
            switch (di) {
                case "up":
                    if (!(this.y < -size / 2)) {
                        this.y -= step * speed;
                        }
                    break;
                case "down":
                    if (!(this.y > HEIGHT - size / 2 - 75)) {
                        this.y += step * speed;
                        }
                    break;
                case "left":
                    if (!(this.x < -size / 2) || !this.isPlayer) {
                        if (this.x < 0 -WIDTH*0.5) this.isAlive = false;
                        this.x -= step * 1.10 * speed;
                        }
                    break;
                case "right":
                    if (!(this.x > WIDTH - size) || !this.isPlayer) {
                        if (this.x > WIDTH*1.5) this.isAlive = false;
                        this.x += step * 1.10 * speed;
                        }
                    break;
                case "upright":
                    if (!(this.x > WIDTH - size)) {
                        this.x += stepCheng * speed;
                        }
                    if (!(this.x > WIDTH + size / 2)) {
                        this.y -= stepCheng * 0.75 * speed;
                        }
                    break;
                case "upleft":
                    if (!(this.x < -size / 2)) {
                        this.x -= stepCheng * speed;
                        }
                    if (!(this.y < -size / 2)) {
                        this.y -= stepCheng * 0.75 * speed;
                        }
                    break;
                case "downright":
                    if (!(this.x > WIDTH - size)) {
                        this.x += stepCheng * speed;
                        }
                    if (!(this.y > HEIGHT - size / 2 - 75)) {
                        this.y += stepCheng * 0.8 * speed;
                        }
                    break;
                case "downleft":
                    if (!(this.x < -size / 2)) {
                        this.x -= stepCheng * speed;
                        }
                    if (!(this.y > HEIGHT - size / 2 - 75)) {
                        this.y += stepCheng * 0.8 * speed;
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
        if (avgPlayerSize < avgSize){
            if (avgPlayerSize < 75){
                size = (float) (Math.random() * (avgPlayerSize*1.50) + 4.0); // 5 - 1.5xavgSize
            } else  {
                size = (float) (Math.random() * (avgPlayerSize*2.25) + 4.0); // 5 - 2.25xavgSize
            }
        } else {
            size = (float) (Math.random() * (avgPlayerSize*2.75) + 19.0); // 19 - 2.75xavgSize
        }

        float midY = ((HEIGHT - size / 2 - 75)  + (-size / 2)) / 2.0f;
        int y;
        if (avgY < midY - 230) {
            y = (int) (Math.random() * (HEIGHT - midY) + midY);
        } else if (avgY > midY + 230) {
            y = (int) (Math.random() * (midY));
        } else {
            y = (int) (Math.random() * (HEIGHT - size / 2 - 75)  + (-size / 2));
        }

        int x = spawnLeft ? -200 : 950; 
        String direction = spawnLeft ? "right" : "left";
        float speed = (float) (0.3 + (Math.random()*0.8) + ((5.0/size)*0.2)); //x0.3-1.1 (+ 0.2 liner if ตัวเล็กสุด) 

        Fish f = new Fish(x, y, size, direction, "enemy");
        f.speed = speed;
    return f;
}

}
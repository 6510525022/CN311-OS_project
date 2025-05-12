public class Fish {
    float x, y, size;
    String direction, type;
    boolean isPlayer, isAlive;

    Fish(int x, int y, int size, String direction, String type, boolean isPlayer) {
        this.x = x; 
        this.y = y; 
        this.direction = direction;
        this.size = size; 
        this.type = type;
        this.isPlayer = isPlayer;
        this.isAlive = true;
    }

    Fish(int x, int y, int size, String direction, String type) {
        this(x, y, size, direction, type, false);
    }

    public boolean isSmash(Fish other) {
        if (this.isAlive && other.isAlive) {
            // คำนวณการชนโดยพิจารณาจากตำแหน่งและขนาด
            double distance = Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
            if (distance < (this.size / 2 + other.size / 2)) {
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
                    this.y += 1;
                    break;
                case "down":
                    this.y -= 1;
                    break;
                case "left":
                    this.x -= 1;
                    break;
                case "right":
                    this.x += 1;
                    break;
            }
        }
    }

    // ฟังก์ชันสำหรับการกินปลาอื่น
    private boolean eat(Fish other) {
        if (this.size > other.size) { // ใหญ่กว่ากินได้
            this.size += other.size / 6.0; // เราใหญ่ขึ้น
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
}
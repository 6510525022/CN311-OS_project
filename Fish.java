public class Fish {
    float x, y, size;
    String direction, type;
    boolean isPlayer, isAlive;

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
    // ฟังก์ชันสำหรับการเคลื่อนไหวของปลา
    public void move(String di) {
        if (this.isAlive) {
            int step = 5;

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
                case "up-right":
                    this.x += 0.7071;
                    this.y += 0.7071;
                    break;
                case "up-left":
                    this.x -= 0.7071;
                    this.y += 0.7071;
                    break;
                case "down-right":
                    this.x += 0.7071;
                    this.y -= 0.7071;
                    break;
                case "down-left":
                    this.x -= 0.7071;
                    this.y -= 0.7071;
                    break;
            }
        }
    }

    // ฟังก์ชันสำหรับการกินปลาอื่น
    private boolean eat(Fish other) {
        if (this.size > other.size) { // ใหญ่กว่ากินได้
            this.size += other.size / 4.0; // เราใหญ่ขึ้น
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
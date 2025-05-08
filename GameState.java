public class GameState {
    private int gamePhase = 0;

    public synchronized int getGamePhase() {
        return gamePhase;
    }

    public synchronized void setGamePhase(int newPhase) {
        this.gamePhase = newPhase;
        System.out.println("GamePhase เปลี่ยนเป็น: " + newPhase);
    }
}
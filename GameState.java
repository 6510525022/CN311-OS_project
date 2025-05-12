public class GameState {
    private int gamePhase = 0;
    private String[] phaseName = {"lobby", "in-game", "result"}; // 0 = lobby, 1 = in-game, 2 = result

    public synchronized int getGamePhase() {
        //System.out.println("Get - GamePhase ปัจจุบัน: " + phaseName[gamePhase] + "[" + gamePhase + "]" );
        return gamePhase;
    }

    public synchronized void setGamePhase(int newPhase) {
        this.gamePhase = newPhase;
        //System.out.println("Set - GamePhase เปลี่ยนเป็น: " + phaseName[newPhase] + "[" + newPhase + "]" );
    }
}
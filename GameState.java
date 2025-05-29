
public class GameState {

    private int gamePhase = 0;
    private String[] phaseName = {"lobby", "in-game", "result"}; // 0 = lobby, 1 = in-game, 2 = result

    public synchronized int getGamePhase() {
        return gamePhase;
    }

    public synchronized void setGamePhase(int newPhase) {
        this.gamePhase = newPhase;

    }
}

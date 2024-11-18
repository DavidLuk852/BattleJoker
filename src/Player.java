import java.net.Socket;

public class Player {
    Socket socket;
    String name;
    int score;
    int totalMoveCount;
    int level;


    public Player(Socket socket, String name, int score, int totalMoveCount, int level) {
        this.socket = socket;
        this.name = name;
        this.score = score;
        this.totalMoveCount = totalMoveCount;
        this.level = level;

    }

}

import java.net.Socket;

public class Player {
    Socket socket;
    String name;
    int score;
    int totalMoveCount;

    public Player(Socket socket, String name, int score, int totalMoveCount){
        this.socket = socket;
        this.name = name;
        this.score = score;
        this.totalMoveCount = totalMoveCount;
    }

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public Socket getSocket(){
        return socket;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setScore(int score){
        this.score = score;
    }

    public int getScore(){
        return score;
    }

    public void setTotalMoveCount(int totalMoveCount){
        this.totalMoveCount = totalMoveCount;
    }

    public int getTotalMoveCount(){
        return totalMoveCount;
    }
}

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class JokerServer {

    ArrayList<Socket> clientList = new ArrayList<>();

    //fff
    //gga

    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);
    private final Map<String, Runnable> actionMap = new HashMap<>();
    private int combo;
    private int numOfTilesMoved;
    private int score;
    private int totalMoveCount;
    private boolean gameOver;
    private int level = 1;
    private String playerName;
    public JokerServer(int port) throws IOException {
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);

        nextRound();

        ServerSocket srvSocket = new ServerSocket(port);
        while (true) {
            Socket clientSocket = srvSocket.accept();

            synchronized (clientList){
                clientList.add(clientSocket);
            }

            Thread childThread = new Thread(()->{
                try{
                    serve(clientSocket);
                } catch(IOException ex){
                    ex.printStackTrace();
                }

                synchronized (clientList){
                    clientList.remove(clientSocket);
                }
            });
            childThread.start();
        }
    }

    public void serve(Socket clientSocket) throws IOException {
        System.out.println(clientSocket.getInetAddress());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        // send a copy of the array to the client when it has just connected
        sendArray(new DataOutputStream(clientSocket.getOutputStream()));
        while(true){
            char dir = (char) in.read();
            System.out.println(dir);

            synchronized (clientList){ /// lock the client list, other thread will wait outside the zone
                moveMerge("" + dir);

                for(Socket s : clientList){
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.write(dir);
                    out.flush();
                    //DO NOT CLOSE the socket or the output stream

                    //send the array to the client
                    sendArray(out);
                }
            }

//            nextRound();
        }
    }

    void sendArray(DataOutputStream out) throws IOException{
        //send the array to the client
        out.write('A');
        out.writeInt(board.length);
        for(int v: board){
            out.writeInt(v);
        }
        out.flush();
    }
    private boolean nextRound() {
        if (isFull()) return false;
        int i;

        // randomly find an empty place
        do {
            i = random.nextInt(SIZE * SIZE);
        } while (board[i] > 0);

        // randomly generate a card based on the existing level, and assign it to the select place
        board[i] = random.nextInt(level) / 4 + 1;
        return true;
    }

    private boolean isFull() {
        for (int v : board)
            if (v == 0) return false;
        return true;
    }

    private void moveDown() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i);
    }

    /**
     * move the values upward and merge them.
     */
    private void moveUp() {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i);
    }

    /**
     * move the values rightward and merge them.
     */
    private void moveRight() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i);
    }

    /**
     * move the values leftward and merge them.
     */
    private void moveLeft() {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i);
    }

    private void moveMerge(int d, int s, int l) {
        int v, j;
        for (int i = s - d; i != l - d; i -= d) {
            j = i;
            if (board[j] <= 0) continue;
            v = board[j];
            board[j] = 0;
            while (j + d != s && board[j + d] == 0)
                j += d;

            if (board[j + d] == 0) {
                j += d;
                board[j] = v;
            } else {
                while (j != s && board[j + d] == v) {
                    j += d;
                    board[j] = 0;
                    v++;
                    score++;
                    combo++;
                }
                board[j] = v;
                if (v > level) level = v;
            }
            if (i != j)
                numOfTilesMoved++;

        }
    }

    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setPlayerName(String name) {
        playerName = name;
    }

    public int getScore() {
        return score;
    }

    public int getCombo() {
        return combo;
    }

    public int getLevel() {
        return level;
    }

    public int getMoveCount() {
        return totalMoveCount;
    }

    public void moveMerge(String dir){
        synchronized (board) {
            if (actionMap.containsKey(dir)) {
                combo = numOfTilesMoved = 0;

                // go to the hash map, find the corresponding method and call it
                actionMap.get(dir).run();

                // calculate the new score
                score += combo / 5 * 2;

                // determine whether the game is over or not
                if (numOfTilesMoved > 0) {
                    totalMoveCount++;
                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                // update the database if the game is over
                if (gameOver) {
                    try {
                        Database.putScore(playerName, score, level);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void  main(String[] args) throws IOException{
        new JokerServer(12345);
    }
}

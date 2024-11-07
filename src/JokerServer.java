import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class JokerServer {

    final static String url = "jdbc:sqlite:data/battleJoker.db";
    static Connection conn;

    ArrayList<Player> clientList = new ArrayList<>();

    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];
    Random random = new Random(0);
    private final Map<String, Consumer<Player>> actionMap = new HashMap<>();
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
            Player player = new Player(clientSocket, playerName, 0, 0, 1);

            synchronized (clientList){
                clientList.add(player);
            }

            Thread childThread = new Thread(()->{
                try{
                    serve(player);
                } catch(IOException ex){
                    ex.printStackTrace();
                }

                synchronized (clientList){
                    clientList.remove(player);
                }
            });
            childThread.start();
        }
    }

    public void serve(Player player) throws IOException {
        System.out.println(player.socket.getInetAddress());
        DataInputStream in = new DataInputStream(player.socket.getInputStream());

        // send a copy of the array to the client when it has just connected
        sendArray(new DataOutputStream(player.socket.getOutputStream()));
        sendLevel(new DataOutputStream(player.socket.getOutputStream()));
        while(true){
            player.name = in.readUTF();
            System.out.print(player.name + ": ");
            char dir = (char) in.read();
            System.out.println(dir);

            synchronized (clientList){ /// lock the client list, other thread will wait outside the zone
                moveMerge("" + dir, player);

                sendScore(new DataOutputStream(player.socket.getOutputStream()), player);
                sendLevel(new DataOutputStream(player.socket.getOutputStream()));
                sendCombo(new DataOutputStream(player.socket.getOutputStream()));
                sendMove(new DataOutputStream(player.socket.getOutputStream()), player);

                for(Player s : clientList){
                    DataOutputStream out = new DataOutputStream(s.socket.getOutputStream());
                    out.write(dir);
                    out.flush();
                    //DO NOT CLOSE the socket or the output stream

                    //send the array to the client
                    sendArray(out);
                    sendLevel(out);
                    sendGameOver(out);
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

    void sendScore(DataOutputStream out, Player player) throws IOException{
        out.write('S');
        out.writeInt(player.score);
        out.flush();
    }

    void sendLevel(DataOutputStream out) throws IOException{
        out.write('l');
        out.writeInt(level);
        out.flush();
    }

    void sendCombo(DataOutputStream out) throws IOException{
        out.write('C');
        out.writeInt(combo);
        out.flush();
    }

    void sendMove(DataOutputStream out, Player player) throws IOException{
        out.write('M');
        out.writeInt(player.totalMoveCount);
        out.flush();
    }

    void sendGameOver(DataOutputStream out) throws IOException{
        out.write('G');
        if(gameOver){
            out.writeInt(1);
        }else{
            out.writeInt(0);
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

    private void moveDown(Player player) {
        for (int i = 0; i < SIZE; i++)
            moveMerge(SIZE, SIZE * (SIZE - 1) + i, i, player);
    }

    /**
     * move the values upward and merge them.
     */
    private void moveUp(Player player) {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i, player);
    }

    /**
     * move the values rightward and merge them.
     */
    private void moveRight(Player player) {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i, player);
    }

    /**
     * move the values leftward and merge them.
     */
    private void moveLeft(Player player) {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(-1, i, SIZE - 1 + i, player);
    }

    private void moveMerge(int d, int s, int l, Player player) {
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
                    player.score++;
                    combo++;
                }
                board[j] = v;
                if (v > level) {
                    level = v;
                    player.level = v;
                }
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

    public void moveMerge(String dir, Player player){
        synchronized (board) {
            if (actionMap.containsKey(dir)) {
                combo = numOfTilesMoved = 0;

                // go to the hash map, find the corresponding method and call it
                actionMap.get(dir).accept(player);

                // calculate the new score
                player.score += combo / 5 * 2;

                // determine whether the game is over or not
                if (numOfTilesMoved > 0) {
                    player.totalMoveCount++;
                    gameOver = level == LIMIT || !nextRound();
                } else
                    gameOver = isFull();

                // update the database if the game is over
                if (gameOver) {
                    try {
                        connect();
                        for(Player p : clientList){
                            putScore(p.name, p.score, p.level);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void connect() throws SQLException, ClassNotFoundException {
        if (conn == null) {
//            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
        }

    }

    public static void disconnect() throws SQLException {
        if (conn != null)
            conn.close();
    }

    public static ArrayList<HashMap<String, String>> getScores() throws SQLException {
        String sql = "SELECT * FROM scores ORDER BY score DESC LIMIT 10";
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        while (resultSet.next()) {
            HashMap<String, String> m = new HashMap<>();
            m.put("name", resultSet.getString("name"));
            m.put("score", resultSet.getString("score"));
            m.put("level", resultSet.getString("level"));
            m.put("time", resultSet.getString("time"));
            data.add(m);
        }
        return data;
    }

    public static void putScore(String name, int score, int level) throws SQLException {
        String sql = String.format("INSERT INTO scores ('name', 'score', 'level', 'time') VALUES ('%s', %d, %d, datetime('now'))", name, score, level);
        Statement statement = conn.createStatement();
        statement.execute(sql);
    }

    public static void  main(String[] args) throws IOException{
        new JokerServer(12345);
    }
}

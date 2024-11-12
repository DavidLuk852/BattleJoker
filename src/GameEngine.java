
import javafx.beans.property.IntegerProperty;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameEngine {
    public static final int SIZE = 4;
    final int[] board = new int[SIZE * SIZE];

    private static GameEngine instance;
    private boolean gameOver;

    private String playerName;
    private int level = 1;
    private int score;
    private int combo;
    private int totalMoveCount;
    private int playerCount;
    private boolean gameStarted;
    private long startTime;  // To record when the game starts
    private boolean timerRunning = false;  // To track if the timer is running
    private int canMove;
    private int moveLeft;
    private String currentPlayer;
    private String WinnerName;
    private int WinnerScore;
    private int WinnerLevel;
    private int WinnerMoves;
    private int newGame;

    Socket clientSocket;
    DataOutputStream out;
    DataInputStream in;

    Thread receiverThread = new Thread(()->{
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            while(true){
                char data = (char) in.read();
                System.out.print(data + ": ");
                switch (data){
                    case 'A':
                        // download array
                        receiveArray(in);
                        break;
                    case 'S':
                        receiveScore(in);
                        break;
                    case 'l':
                        receiveLevel(in);
                        break;
                    case 'C':
                        receiveCombo(in);
                        break;
                    case 'M':
                        receiveMove(in);
                        break;
                    case 'G':
                        receiveGameOver(in);
                        break;
                    case 'P':
                        receivePlayer(in);
                        break;
                    case 'T':
                        receiveGameStart(in);
                        break;
                    case 'Y':
                        receiveCanMove(in);
                        break;
                    case 'N':
                        receiveMoveLeft(in);
                        break;
                    case 'Z':
                        receiveCurrentPlayer(in);
                        break;
                    case 'W':
                        receiveWinner(in);
                        break;
                    case 'E':
                        receiveNewGame(in);
                        break;
                    default:
                        // print the direction
                        System.out.println(data);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(); ///debugging only, remove it before production
        }
    });

    void receiveNewGame(DataInputStream in) throws IOException {
        newGame = in.readInt();
        System.out.println(newGame);
    }

    void receiveWinner(DataInputStream in) throws IOException {
        WinnerName = in.readUTF();
        WinnerScore = in.readInt();
        WinnerLevel = in.readInt();
        WinnerMoves = in.readInt();

        System.out.println(WinnerName + ", " + WinnerScore + ", " + WinnerLevel + ", " + WinnerMoves );
    }
    void receiveCurrentPlayer(DataInputStream in) throws IOException {
        currentPlayer = in.readUTF();
        System.out.println(currentPlayer);
    }

    void receiveMoveLeft(DataInputStream in) throws IOException {
        moveLeft = in.readInt();
        System.out.println(moveLeft);
    }

    void receiveCanMove(DataInputStream in) throws IOException {
        canMove = in.readInt();
        System.out.println(canMove);
    }

    void receiveGameStart(DataInputStream in) throws IOException {
        int index = in.readInt();
        if(index == 1){
            gameStarted = true;
        }else{
            gameStarted = false;
        }
        System.out.println(index);
    }
    void receivePlayer(DataInputStream in) throws IOException{
        playerCount = in.readInt();
        System.out.println(playerCount);

    }
    void receiveArray(DataInputStream in) throws IOException {
        int size = in.readInt();
        for(int i=0; i<size; i++) {
            board[i] = in.readInt();
            System.out.print(board[i]);
        }
        System.out.println();
    }
    void receiveScore(DataInputStream in) throws IOException{
        score = in.readInt();
        System.out.println(score);
    }
    void receiveLevel(DataInputStream in) throws IOException{
        level = in.readInt();
        System.out.println(level);
    }
    void receiveCombo(DataInputStream in) throws IOException{
        combo = in.readInt();
        System.out.println(combo);
    }
    void receiveMove(DataInputStream in) throws IOException{
        totalMoveCount = in.readInt();
        System.out.println(totalMoveCount);
    }
    void receiveGameOver(DataInputStream in) throws IOException{
        int index = in.readInt();
        if(index == 0){
            gameOver = false;
        }else{
            gameOver = true;
        }
        System.out.println(index);
    }

    private GameEngine(String ip, int port) {
        try{
            clientSocket = new Socket(ip, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
            receiverThread.start();
        } catch(IOException ex){
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static GameEngine getInstance(String ip, int port) {
        if (instance == null)
            instance = new GameEngine(ip, port);
        return instance;
    }

    public void waitStart() throws IOException{
        out.write('W');
        out.flush();
    }

    /**
     * Move and combine the cards based on the input direction
     * @param dir
     */
    public void moveMerge(String dir) throws IOException {
        System.out.println(dir);
        /// send direction to server
        out.writeUTF("Move Merge");
        out.write(dir.charAt(0));
        out.flush();
    }

    public int getValue(int r, int c) {
        synchronized (board) {
            return board[r * SIZE + c];
        }
    }

    public int getPlayerCount(){
        return playerCount;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setPlayerName(String name) throws IOException {
        playerName = name;
        out.writeUTF("Player Name");
        out.writeUTF(playerName);
        out.flush();
    }

    public String getCurrentPlayer() {
        return currentPlayer;
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

    public void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunning = true;
    }

    public boolean getGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) throws IOException {
        this.gameStarted = gameStarted;
        out.writeUTF("Game Start");
        out.flush();
    }

    public int getCanMove() {
        return canMove;
    }

    public String getWinnerName(){
        return WinnerName;
    }

    public int getWinnerScore(){
        return WinnerScore;
    }

    public int getWinnerLevel(){
        return WinnerLevel;
    }

    public int getWinnerMoveCount(){
        return WinnerMoves;
    }

    public int getNewGame(){
        return newGame;
    }

    public void stopTimer() {
        timerRunning = false;
    }

    public double getElapsedTime() {
        if (!timerRunning) {
            return 0;
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime / 1000.0;  // Convert milliseconds to seconds
    }
}



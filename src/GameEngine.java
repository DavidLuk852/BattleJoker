
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
import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;


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
    public static long startTime;  // To record when the game starts
    public static boolean timerRunning = false;  // To track if the timer is running
    private int canMove;
    private int moveLeft;
    private String currentPlayer;
    private String WinnerName;
    private int WinnerScore;
    private int WinnerLevel;
    private int WinnerMoves;
    private int newGame;
    private String updatePlayer;
    private boolean update = false;
    private String cancelPlayer;
    private boolean cancel = false;

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
                    case 'K':
                        receiveUpdatePuzzle(in);
                        break;
                    case 'B':
                        receiveCancelAction(in);
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

    void receiveCancelAction(DataInputStream in) throws IOException {
        cancelPlayer = in.readUTF();
        cancel = true;
        System.out.println(cancelPlayer);
    }
    void receiveUpdatePuzzle(DataInputStream in) throws IOException {
        updatePlayer = in.readUTF();
        update = true;
        System.out.println(updatePlayer);
    }
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

    public String getPlayerName() {
        return playerName;
    }

    public String getUpdatePlayer(){
        return updatePlayer;
    }

    public boolean getUpdate(){
        return update;
    }
    public void setUpdate(){
        this.update = false;
    }
    public String getCancelPlayer(){
        return cancelPlayer;
    }
    public boolean getCancel(){
        return cancel;
    }
    public void setCancel(){
        this.cancel = false;
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

    public static void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunning = true;
    }

    public  static double getElapsedTime() {
        if (!timerRunning) {
            return 0;
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime / 1000.0;  // Convert milliseconds to seconds
    }

    public static void stopTimer() {
        timerRunning = false;
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

    public int getMoveLeft(){
        return moveLeft;
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

    public void cancelAction() throws IOException {
        out.writeUTF("Cancel Last Action");
        out.flush();
    }

    public void savePuzzle(File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            synchronized (board) {
                out.writeInt(SIZE);
                for (int value : board) {
                    out.writeInt(value);
                }
                out.writeUTF(currentPlayer);
                out.writeInt(level);
                out.writeInt(score);
                out.writeInt(combo);
                out.writeInt(totalMoveCount);
                out.writeBoolean(gameOver);
                out.writeInt(playerCount);
                out.writeBoolean(gameStarted);
            }
        }
    }

    public void loadPuzzle(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            synchronized (board) {
                int newSize = in.readInt();
                if (newSize != SIZE) {
                    throw new IOException("Invalid puzzle size");
                }
                for (int i = 0; i < board.length; i++) {
                    board[i] = in.readInt();
                }
                currentPlayer = in.readUTF();
                level = in.readInt();
                score = in.readInt();
                combo = in.readInt();
                totalMoveCount = in.readInt();
                gameOver = in.readBoolean();
                playerCount = in.readInt();
                gameStarted = in.readBoolean();
            }
            updateGameState();
        }
    }

    private void updateGameState() {
        updateCurrentPlayer();
        updateBoard();
        updateScore();
        updateLevel();
        updateCombo();
        updateMoveCount();
        updateGameOver();
        updatePlayerCount();
        updateGameStarted();
    }

    private void updateBoard() {

        System.out.println("Board updated:");
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print(getValue(i, j) + " ");
            }
            System.out.println();
        }
    }

    private void updateScore() {
        System.out.println("Score updated: " + score);
    }

    private void updateLevel() {
        System.out.println("Level updated: " + level);
    }

    private void updateCombo() {
        // 更新連擊數的界面
        // updateComboUI(combo);
        System.out.println("Combo updated: " + combo);
    }

    private void updateMoveCount() {
        // 更新移動次數的界面
        // updateMoveCountUI(totalMoveCount);
        System.out.println("Move count updated: " + totalMoveCount);
    }

    private void updateGameOver() {
        // 更新遊戲結束狀態的界面
        // updateGameOverUI(gameOver);
        System.out.println("Game over status updated: " + gameOver);
    }

    private void updatePlayerCount() {
        // 更新玩家數量的界面
        // updatePlayerCountUI(playerCount);
        System.out.println("Player count updated: " + playerCount);
    }

    private void updateGameStarted() {
        // 更新遊戲開始狀態的界面
        // updateGameStartedUI(gameStarted);
        System.out.println("Game started status updated: " + gameStarted);
    }

    private void updateCurrentPlayer() {
        // 更新當前玩家的界面
        // updateCurrentPlayerUI(currentPlayer);
        System.out.println("Current player updated: " + currentPlayer);
    }


    public void uploadPuzzleToServer(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            out.writeUTF("Upload Puzzle");
            out.writeInt(SIZE);
            for (int value : board) {
                out.writeInt(value);
            }
            out.writeUTF(currentPlayer);
            out.writeInt(level);
            out.writeInt(score);
            out.writeInt(combo);
            out.writeInt(totalMoveCount);
            out.writeBoolean(gameOver);
            out.writeInt(playerCount);
            out.writeBoolean(gameStarted);
            out.flush();
        }
    }


}



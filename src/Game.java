import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class Game {
    ArrayList<Player> clientList = new ArrayList<>();
    public static final int LIMIT = 14;
    public static final int SIZE = 4;
    private final int[] board;
    Random random = new Random(0);
    private final Map<String, Consumer<Player>> actionMap = new HashMap<>();
    private int combo;
    private int numOfTilesMoved;
    private int score;
    private int totalMoveCount;
    private boolean gameOver;
    private boolean gameStart;
    private boolean gameStarted;
    private int level = 1;
    private String playerName;
    private Player currentPlayer;
    private int playerCount;
    private int movesLeft;

    public Game(){
        this.board = new int[SIZE * SIZE];
        actionMap.put("U", this::moveUp);
        actionMap.put("D", this::moveDown);
        actionMap.put("L", this::moveLeft);
        actionMap.put("R", this::moveRight);
        nextRound();
    }

    public synchronized boolean addPlayer(Player player) throws IOException {
        if (clientList.size() < 4 && !gameStart) {
            clientList.add(player);
            return true;
        }
        return false;
    }

    public synchronized void startGame() {
        gameStart = true;
        for (Player p : clientList) {
            try {
                DataOutputStream out = new DataOutputStream(p.socket.getOutputStream());
                sendGameStart(out);
                boolean isFirstPlayer = clientList.indexOf(p) == 0;
                sendTurnNotification(out, isFirstPlayer); // First player can move
                sendMoveCountNotification(out, isFirstPlayer ? 4 : 0); // First player has 4 moves
                if (isFirstPlayer) {
                    currentPlayer = p;
                    movesLeft = 4;
                }
                sendCurrentPlayer(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serve(Player player) {
        System.out.println(player.socket.getInetAddress());
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            in = new DataInputStream(player.socket.getInputStream());
            out = new DataOutputStream(player.socket.getOutputStream());

            // Send initial game state to the client
            sendArray(out);
            sendLevel(out);

            for (Player p : clientList) {
                sendPlayer(new DataOutputStream(p.socket.getOutputStream()));
            }

            // Send initial turn notification
            sendTurnNotification(out, false); // Initially, the player cannot move
            sendMoveCountNotification(out, 0); // No moves left initially

            movesLeft = 0;
            currentPlayer = null;

            while (!gameOver) {
                String message = in.readUTF();

                switch (message) {
                    case "Upload Puzzle":
                        loadPuzzleFromStream(in);
                        synchronized (clientList) {
                            for (Player p : clientList) {
                                DataOutputStream pOut = new DataOutputStream(p.socket.getOutputStream());
                                sendUpdatePuzzle(pOut);
                                sendArray(pOut);
                                sendLevel(pOut);
                            }
                        }
                        break;

                    case "Game Start":
                        startGame();
                        break;

                    case "Player Name":
                        player.name = in.readUTF();
                        System.out.println("Player name set to: " + player.name);
                        break;

                    case "Move Merge":
                        handleMoveMerge(player, in, out);
                        break;

                    case "Cancel Last Action":
                        useCancelSkill(player);
                        break;

                    default:
                        System.out.println("Unknown message: " + message);
                        break;
                }
            }
        } catch (SocketException e) {
            System.out.println("Connection reset by client: " + player.name);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (player.socket != null) {
                    player.socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Remove player from client list and reset game state if needed
            synchronized (clientList) {
                clientList.remove(player);
                if (clientList.isEmpty()) {
                    resetGame();
                }
            }
        }
    }

    private void handleMoveMerge(Player player, DataInputStream in, DataOutputStream out) throws IOException {
        System.out.print(player.name + ": ");
        char dir = (char) in.read();
        System.out.println(dir);

        if (player.equals(currentPlayer)) {
            synchronized (clientList) {
                moveMerge("" + dir, player);

                sendScore(out, player);
                sendLevel(out);
                sendCombo(out);
                sendMove(out, player);

                for (Player s : clientList) {
                    DataOutputStream sOut = new DataOutputStream(s.socket.getOutputStream());
                    sOut.write(dir);
                    sOut.flush();
                    sendArray(sOut);
                    sendLevel(sOut);
                }

                movesLeft--;

                if (movesLeft == 0) {
                    endTurn(out);
                } else {
                    sendMoveCountNotification(out, movesLeft);
                }
            }
        }
    }

    private void endTurn(DataOutputStream out) throws IOException {
        sendTurnNotification(out, false);
        sendMoveCountNotification(out, 0);

        int currentPlayerIndex = clientList.indexOf(currentPlayer);
        int nextPlayerIndex = (currentPlayerIndex + 1) % clientList.size();
        currentPlayer = clientList.get(nextPlayerIndex);
        movesLeft = 4;

        currentPlayer.skillUsed = false;
        currentPlayer.previousBoard = null;
        currentPlayer.previousScore = 0;
        currentPlayer.previousLevel = 0;
        currentPlayer.previousCombo = 0;
        currentPlayer.previousTotalMoveCount = 0;


        DataOutputStream nextOut = new DataOutputStream(currentPlayer.socket.getOutputStream());
        sendTurnNotification(nextOut, true);
        sendMoveCountNotification(nextOut, movesLeft);

        for (Player p : clientList) {
            sendCurrentPlayer(new DataOutputStream(p.socket.getOutputStream()));
        }
    }


    private void loadPuzzleFromStream(DataInputStream in) throws IOException {
        synchronized (board) {
            int newSize = in.readInt();
            if (newSize != SIZE) {
                throw new IOException("Invalid puzzle size");
            }
            for (int i = 0; i < board.length; i++) {
                board[i] = in.readInt();
            }
            currentPlayer = findPlayerByName(in.readUTF());
            level = in.readInt();
            currentPlayer.score = in.readInt();
            combo = in.readInt();
            currentPlayer.totalMoveCount = in.readInt();
            gameOver = in.readBoolean();
            playerCount = in.readInt();
            gameStarted = in.readBoolean();
        }
    }

    private Player findPlayerByName(String name) {
        for (Player p : clientList) {
            if (p.name.equals(name)) {
                return p;
            }
        }
        return null;
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

    private void moveUp(Player player) {
        for (int i = 0; i < SIZE; i++)
            moveMerge(-SIZE, i, SIZE * (SIZE - 1) + i, player);
    }

    private void moveRight(Player player) {
        for (int i = 0; i <= SIZE * (SIZE - 1); i += SIZE)
            moveMerge(1, SIZE - 1 + i, i, player);
    }

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
                    for (Player p : clientList) {
                        p.level = v;
                    }
                }
            }
            if (i != j)
                numOfTilesMoved++;
        }
    }

    public void moveMerge(String dir, Player player) {
        synchronized (board) {
            if (actionMap.containsKey(dir)) {
                // Store previous state
                player.previousBoard = board.clone();
                player.previousScore = player.score;
                player.previousLevel = level;
                player.previousCombo = combo;
                player.previousTotalMoveCount = player.totalMoveCount;

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
                        JokerServer.connect();
                        for (Player p : clientList) {
                            JokerServer.putScore(p.name, p.score, p.level);
                            sendGameOver(new DataOutputStream(p.socket.getOutputStream()));
                        }
                        determineWinner();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void useCancelSkill(Player player) throws IOException {
        if (!player.skillUsed && player.previousBoard != null) {
            // Revert to the previous state
            System.arraycopy(player.previousBoard, 0, board, 0, board.length);
            player.score = player.previousScore;
            level = player.previousLevel;
            combo = player.previousCombo;
            player.totalMoveCount = player.previousTotalMoveCount;
            player.skillUsed = true;
            movesLeft += 1;

            // Notify all players of the reverted state
            synchronized (clientList) {
                for (Player p : clientList) {
                    DataOutputStream out = new DataOutputStream(p.socket.getOutputStream());
                    sendArray(out);
                    sendLevel(out);
                    sendScore(out, p);
                    sendCombo(out);
                    sendMove(out, p);
                    sendCancelAction(out, p);
                }
            }
        }
    }

    private void determineWinner() throws IOException {
        int winnerScore = 0;
        Player winPlayer = null;
        for (Player p : clientList) {
            if (p.score > winnerScore) {
                winnerScore = p.score;
                winPlayer = p;
            }
        }

        for (Player p : clientList) {
            DataOutputStream out = new DataOutputStream(p.socket.getOutputStream());
            sendWinner(out, winPlayer);
        }
    }

    public void resetGame() {
        for (int i = 0; i < board.length; i++) {
            board[i] = 0;
        }

        board[11] = 1;

        combo = 0;
        numOfTilesMoved = 0;
        gameOver = false;
        gameStart = false;
        level = 1;
        currentPlayer = null;
        movesLeft = 0;

        clientList.clear();
    }

    void sendCancelAction(DataOutputStream out,Player p) throws IOException {
        out.write('B');
        out.writeUTF(p.name);
        out.flush();
    }
    void sendUpdatePuzzle(DataOutputStream out) throws IOException {
        out.write('K');
        out.writeUTF(currentPlayer.name);
        out.flush();
    }
    void sendWinner(DataOutputStream out, Player p) throws IOException {
        out.write('W');
        out.writeUTF(p.name);
        out.writeInt(p.score);
        out.writeInt(p.level);
        out.writeInt(p.totalMoveCount);
        out.flush();
    }
    void sendCurrentPlayer(DataOutputStream out) throws IOException {
        out.write('Z');
        out.writeUTF(currentPlayer.name);
        out.flush();
    }
    void sendTurnNotification(DataOutputStream out,boolean canMove) throws IOException {
        out.write('Y');
        if(canMove){
            out.writeInt(1);
        }else{
            out.writeInt(0);
        }
        out.flush();
    }
    void sendMoveCountNotification(DataOutputStream out, int movesLeft) throws IOException {
        out.write('N');
        out.writeInt(movesLeft);
        out.flush();
    }
    void sendGameStart(DataOutputStream out) throws IOException {
        out.write('T');
        if(gameStart){
            out.writeInt(1);
        }else{
            out.writeInt(0);
        }
        out.flush();
    }
    void sendPlayer(DataOutputStream out) throws IOException{
        out.write('P');
        out.writeInt(clientList.size());
        out.flush();
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
}

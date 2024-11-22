import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class JokerServer {

    final static String url = "jdbc:sqlite:data/battleJoker.db";
    static Connection conn;

    private final List<Game> games = new ArrayList<>();
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public JokerServer(int port) throws IOException {
        ServerSocket srvSocket = new ServerSocket(port);
        while (true) {
            Socket clientSocket = srvSocket.accept();
            Player player = new Player(clientSocket, "", 0, 0, 1);

            Game assignedGame = null;
            synchronized (games) {
                for (Game game : games) {
                    if (game.addPlayer(player)) {
                        assignedGame = game;
                        break;
                    }
                }
                if (assignedGame == null) {
                    assignedGame = new Game();
                    assignedGame.addPlayer(player);
                    games.add(assignedGame);
                }
            }

            Game finalAssignedGame = assignedGame;
            executor.execute(() -> {
                finalAssignedGame.serve(player);
            });
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

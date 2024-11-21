import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.scene.paint.Color; // Add this import

public class GameWindow {
    @FXML
    MenuBar menuBar;

    @FXML
    Label nameLabel;

    @FXML
    Label scoreLabel;

    @FXML
    Label levelLabel;

    @FXML
    Label comboLabel;

    @FXML
    Label moveCountLabel;

    @FXML
    Label numberofPlayerLabel;

    @FXML
    Label currentPlayerLabel;

    @FXML
    Pane boardPane;

    @FXML
    Canvas canvas;

    @FXML
    Label timerLabel;

    @FXML
    Button goButton;

    @FXML
    Button cancelButton;

    @FXML
    MenuItem saveMenuItem;

    @FXML
    MenuItem loadMenuItem;

    @FXML
    MenuItem changeColorMenuItem;

    @FXML
    TextArea message;

    long startTime;
    Stage stage;
    AnimationTimer animationTimer;
    AnimationTimer gameStartTimer;
    AnimationTimer moveCheckTimer;
    AnimationTimer newGameTimer;

    final String imagePath = "images/";
    final String[] symbols = {"bg", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "Joker"};
    final Image[] images = new Image[symbols.length];
    GameEngine gameEngine;

    public GameWindow(Stage stage, String ip, int port) throws IOException {
        loadImages();

        gameEngine = GameEngine.getInstance(ip, port);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainUI.fxml"));
        if (loader.getLocation() == null) {
            throw new IOException("FXML file not found");
        }

        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        this.stage = stage;

        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        stage.widthProperty().addListener(w -> onWidthChangedWindow(((ReadOnlyDoubleProperty) w).getValue()));
        stage.heightProperty().addListener(h -> onHeightChangedWindow(((ReadOnlyDoubleProperty) h).getValue()));
        stage.setOnCloseRequest(event -> quit());

        saveMenuItem.setOnAction(event -> savePuzzle());
        loadMenuItem.setOnAction(event -> loadPuzzle());
        changeColorMenuItem.setOnAction(event -> changeWindowColor()); // Set action for color change
        message.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            event.consume();
        });
        message.textProperty().addListener((observable, oldValue, newValue) -> {
            message.setScrollTop(Double.MAX_VALUE); // Scroll to the bottom
        });
        cancelButton.setOnAction(event -> {
            try {
                cancelAction(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        stage.show();

        if (gameEngine.getPlayerCount() == 1 && !gameEngine.getGameStarted()) {
            goButton.setVisible(true);
            goButton.setDisable(false);
            goButton.setOnMouseClicked(event -> {
                try {
                    OnButtonClick(event);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            waitGameStart();
        } else if (gameEngine.getPlayerCount() == 2 || gameEngine.getPlayerCount() == 3) {
            if (!gameEngine.getGameStarted()) {
                waitGameStart();
            } else {
                waitNewGame();
            }
        } else if (gameEngine.getPlayerCount() == 4) {
            if (!gameEngine.getGameStarted()) {
                numberofPlayerLabel.setText("Number of Players: " + gameEngine.getPlayerCount());
                gameEngine.setGameStarted(true);
                initCanvas();
                gameStart();

                saveMenuItem.setVisible(true);
                loadMenuItem.setVisible(true);
            } else {
                waitNewGame();
            }
        } else if (gameEngine.getPlayerCount() >= 5 || gameEngine.getGameStarted()) {
            waitNewGame();
        }
    }

    private void changeWindowColor() {
        // Generate random RGB values
        double red = Math.random();
        double green = Math.random();
        double blue = Math.random();

        // Set the background color of the VBox
        nameLabel.getParent().setStyle(String.format("-fx-background-color: rgb(%.0f, %.0f, %.0f);",
                red * 255, green * 255, blue * 255));

        // Set the background color of the HBox
        ((HBox) nameLabel.getParent().getParent()).setStyle(String.format("-fx-background-color: rgb(%.0f, %.0f, %.0f);",
                red * 255, green*  255, blue * 255));

        // Set the background color of the Pane (boardPane)
        boardPane.setStyle(String.format("-fx-background-color: rgb(%.0f, %.0f, %.0f);",
                red * 255, green  *255, blue * 255));

        // Set the background color of the Scene
        Scene scene = stage.getScene();
        scene.setFill(Color.rgb((int)(red*  255), (int)(green * 255), (int)(blue * 255)));
    }

    private void cancelAction(Event event) throws IOException {
        cancelButton.setVisible(false);
        cancelButton.setDisable(true);
        gameEngine.cancelAction();
    }

    private void savePuzzle() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Puzzle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Puzzle Files", "*.pzl"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                gameEngine.savePuzzle(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadPuzzle() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Puzzle");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Puzzle Files", "*.pzl"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                gameEngine.loadPuzzle(file);
                gameEngine.uploadPuzzleToServer(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void OnButtonClick(Event event) throws IOException {
        goButton.setVisible(false);
        goButton.setDisable(true);
        gameEngine.setGameStarted(true);
    }

    private void waitGameStart() {
        gameStartTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updatePlayerNumber();
                if (gameEngine.getPlayerCount() == 4 || gameEngine.getGameStarted()) {
                    goButton.setVisible(false);
                    goButton.setDisable(true);
                    Platform.runLater(() -> {
                        initCanvas();
                        gameStart();
                        gameStartTimer.stop(); // Stop the timer once the game starts

                        saveMenuItem.setVisible(true);
                        loadMenuItem.setVisible(true);
                    });
                }
            }
        };
        gameStartTimer.start(); // Start the timer after it's initialized
    }

    private void waitNewGame() throws IOException {
        new gameFullWindow(gameEngine);

        newGameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameEngine.getNewGame() == 1) {
                    Platform.runLater(() -> {
                        waitGameStart();
                        newGameTimer.stop(); // Stop the timer once the game starts
                    });
                }
            }
        };
        newGameTimer.start();
    }

    private void updatePlayerNumber() {
        numberofPlayerLabel.setText("Number of Players: " + gameEngine.getPlayerCount());
    }

    private void loadImages() throws IOException {
        for (int i = 0; i < symbols.length; i++)
            images[i] = new Image(Files.newInputStream(Paths.get(imagePath + symbols[i] + ".png")));
    }

    private void gameStart() {
        startTime = System.currentTimeMillis();  // Initialize the start time
        gameEngine.startTimer();  // Start the timer in GameEngine
        animationTimer.start();
        moveCheckTimer.start();
        message.appendText("Message: Game Started\n");
    }

    private void initCanvas() {
        // Create an AnimationTimer to periodically check gameEngine.getCanMove()
        moveCheckTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameEngine.getCanMove() == 1 && !gameEngine.isGameOver()) {
                    updateCurrentPlayer();
                    // Set the event handler if the player can move
                    canvas.setOnKeyPressed(event -> {
                        try {
                            gameEngine.moveMerge(event.getCode().toString());
                            // scoreLabel.setText("Score: " + gameEngine.getScore());
                            // levelLabel.setText("Level: " + gameEngine.getLevel());
                            // comboLabel.setText("Combo: " + gameEngine.getCombo());
                            // moveCountLabel.setText("# of Moves: " + gameEngine.getMoveCount());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            System.exit(-1);
                        }
                    });
                } else {
                    // Remove the event handler if the player cannot move
                    canvas.setOnKeyPressed(null);
                }
            }
        };

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateCurrentPlayer();
                render();
                updateTimerDisplay();
                updateMessage();
                if (gameEngine.isGameOver()) {
                    System.out.println("Game Over!");
                    animationTimer.stop();
                    saveMenuItem.setVisible(false);
                    loadMenuItem.setVisible(false);
                    Platform.runLater(() -> {
                        try {
                            new gameWinnerWindow(gameEngine);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }else if(gameEngine.getMoveLeft() == 4){
                    cancelButton.setVisible(true);
                    cancelButton.setDisable(false);
                } else if (gameEngine.getMoveLeft() == 0) {
                    cancelButton.setVisible(false);
                    cancelButton.setDisable(true);
                }
            }
        };
        canvas.requestFocus();
    }

    private void updateMessage(){
        if(gameEngine.getUpdate()){
            message.appendText("Message: " + gameEngine.getUpdatePlayer() + " just Update Last Action!\n");
            gameEngine.setUpdate();
        }else if(gameEngine.getCancel()){
            message.appendText("Message: " + gameEngine.getCancelPlayer() + " just Cancel Last Action!\n");
            gameEngine.setCancel();
        }
    }

    private void updateCurrentPlayer() {
        currentPlayerLabel.setText("Current Player: " + gameEngine.getCurrentPlayer());
    }

    private void updateTimerDisplay() {
        long elapsedTime = System.currentTimeMillis() - startTime;  // Get elapsed time
        double seconds = elapsedTime / 1000.0;  // Convert milliseconds to seconds
        timerLabel.setText(String.format("Time: %.2f s", seconds));  // Update timer label
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        double sceneSize = Math.min(w, h);
        double blockSize = sceneSize / GameEngine.SIZE;
        double padding = blockSize * .05;
        double startX = (w - sceneSize) / 2;
        double startY = (h - sceneSize) / 2;
        double cardSize = blockSize - (padding * 2);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double y = startY;
        int v;

        scoreLabel.setText("Score: " + gameEngine.getScore());
        levelLabel.setText("Level: " + gameEngine.getLevel());
        comboLabel.setText("Combo: " + gameEngine.getCombo());
        moveCountLabel.setText("# of Moves: " + gameEngine.getMoveCount());

        // Draw the background and cards from left to right, and top to bottom.
        for (int i = 0; i < GameEngine.SIZE; i++) {
            double x = startX;
            for (int j = 0; j < GameEngine.SIZE; j++) {
                gc.drawImage(images[0], x, y, blockSize, blockSize);  // Draw the background

                v = gameEngine.getValue(i, j);

                if (v > 0)  // if a card is in the place, draw it
                    gc.drawImage(images[v], x + padding, y + padding, cardSize, cardSize);

                x += blockSize;
            }
            y += blockSize;
        }
    }

    void onWidthChangedWindow(double w) {
        double width = w - boardPane.getBoundsInParent().getMinX();
        boardPane.setMinWidth(width);
        canvas.setWidth(width);
        render();
    }

    void onHeightChangedWindow(double h) {
        double height = h - boardPane.getBoundsInParent().getMinY() - menuBar.getHeight();
        boardPane.setMinHeight(height);
        canvas.setHeight(height);
        render();
    }

    void quit() {
        System.out.println("Bye bye");
        stage.close();
        System.exit(0);
    }

    public void setName(String name) throws IOException {
        nameLabel.setText(name);
        gameEngine.setPlayerName(name);
    }
}

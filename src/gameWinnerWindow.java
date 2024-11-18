import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class gameWinnerWindow {
    Stage stage;

    @FXML
    Label winnerNameLabel;
    @FXML
    Label winnerScoreLabel;
    @FXML
    Label winnerLevelLabel;
    @FXML
    Label winnerMoveCountLabel;
    @FXML
    Label TimeLabel;
    @FXML
    Button goButton;

    public gameWinnerWindow(GameEngine gameEngine) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameWinnerUI.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        goButton.setOnMouseClicked(this::OnButtonClick);

        winnerNameLabel.setText("Winner: " + gameEngine.getWinnerName());
        winnerScoreLabel.setText("Score: " + gameEngine.getWinnerScore());
        winnerLevelLabel.setText("Level: " + gameEngine.getWinnerLevel());
        winnerMoveCountLabel.setText("Total Moves: " + gameEngine.getWinnerMoveCount());
        TimeLabel.setText("Total time for the game: " + gameEngine.getElapsedTime());

        stage.showAndWait();
    }

    @FXML
    void OnButtonClick(Event event) {
        try {
            new ScoreboardWindow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

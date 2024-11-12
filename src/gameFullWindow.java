import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;

public class gameFullWindow {
    Stage stage;

    @FXML
    Button waitButton;

    @FXML
    Button leaveButton;

    GameEngine gameEngine;
    public gameFullWindow(GameEngine gameEngine) throws IOException {
        this.gameEngine = gameEngine;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameFull.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Battle Joker");
        stage.setMinWidth(scene.getWidth());
        stage.setMinHeight(scene.getHeight());

        waitButton.setOnMouseClicked(this::OnWaitButtonClick);
        leaveButton.setOnMouseClicked(this::OnLeaveButtonClick);

        stage.showAndWait();
    }

    @FXML
    void OnWaitButtonClick(Event event) {
        stage.close();
        System.exit(0);
    }

    @FXML
    void OnLeaveButtonClick(Event event) {
        stage.close();
        System.exit(0);
    }
}

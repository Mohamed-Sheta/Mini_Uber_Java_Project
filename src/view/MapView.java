package view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MapView extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/MapView.fxml"));
        Scene scene = new Scene(root, 390, 750);

        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        Image icon = new Image(getClass().getResource("/Logo.jpg").toExternalForm());
        stage.getIcons().add(icon);
        
        stage.setTitle("MiniGo");
        stage.setScene(scene);
        stage.setMinWidth(320);
        stage.setMinHeight(600);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

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
        // Load FXML from resources directory
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getClassLoader().getResource("MapView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 390, 750);

        scene.getStylesheets().add(getClass().getClassLoader().getResource("style.css").toExternalForm());

        // Set application icon
        try {
            java.io.InputStream iconStream = getClass().getClassLoader().getResourceAsStream("Logo.jpg");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    stage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load MapView icon: " + e.getMessage());
        }

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
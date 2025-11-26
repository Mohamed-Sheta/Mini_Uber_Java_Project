package controller;

import Model.ProblemType;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.MapGraph;
import services.Request;

import java.io.IOException;
import java.util.Map;

public class miniuber extends Application { // Fixed naming convention
    @Override
    public void start(Stage stage) throws IOException {
        try {
            Request.DatabaseInitializer dbInit = new Request.DatabaseInitializer();
            Map<ProblemType, Integer> problemTypeMap = dbInit.initialize(true); // true = reset DB
            MapGraph.CityMapSetup citySetup = new MapGraph.CityMapSetup();
            citySetup.initializeAll();

            stage.setMinWidth(400);
            stage.setMinHeight(750);
            stage.setResizable(false);

            // Set application icon
            try {
                java.io.InputStream iconStream = getClass().getResourceAsStream("/Logo-removebg-preview.png");
                if (iconStream != null) {
                    javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                    if (!icon.isError()) {
                        stage.getIcons().setAll(icon);
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not load application icon: " + e.getMessage());
            }

            FXMLLoader fxmlLoader = new FXMLLoader(miniuber.class.getResource("/view/Splash.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 390, 750);

            stage.setTitle("MiniGO");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load FXML file: " + e.getMessage());
            e.printStackTrace();
            throw e; // rethrow so JavaFX sees initialization failed
        } catch (Exception ex) {
            System.err.println("Unexpected error during startup: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

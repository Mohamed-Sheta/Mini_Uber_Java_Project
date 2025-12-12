package controller;
import Model.Driver;
import Model.Passenger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;
public class AboutController {
    @FXML
    private Button backButton;
    private Object currentUser;
    private boolean isDriver = false;

    public void setUser(Object user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
    }
    @FXML
    public void onBackToMap() {
        try {
            if (isDriver) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDashboard.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverDashboardController controller = loader.getController();
                if (currentUser != null) {
                    controller.setDriver((Driver) currentUser);
                }

                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                java.net.URL fxmlUrl = getClass().getResource("/MapView.fxml");
                if (fxmlUrl == null) {
                    fxmlUrl = getClass().getClassLoader().getResource("MapView.fxml");
                }
                if (fxmlUrl == null) {
                    System.err.println("ERROR: Could not find MapView.fxml");
                    return;
                }
                System.out.println("Loading MapView from: " + fxmlUrl);
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Scene scene = new Scene(loader.load(), 390, 750);
                MapController controller = loader.getController();
                if (currentUser != null) {
                    controller.setPassenger((Passenger) currentUser);
                }
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
                System.out.println("MapView loaded successfully");
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate back: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


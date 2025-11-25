package controller;

import Model.Driver;
import Model.Passenger;
import Model.Person;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {

    private Stage stage;
    private Scene scene;
    private Passenger passenger;
    private Driver driver;

    @FXML
    RadioButton radio1;
    @FXML
    RadioButton radio2;

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
        // You can use passenger data here to customize the UI
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
        // You can use driver data here to customize the UI
    }

    @FXML
    public void logipath(ActionEvent e) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene = new Scene(root, 741, 602);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void Rgister(ActionEvent e) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("/view/Register.fxml"));
        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene = new Scene(root, 741, 602);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void radiocheck() {
        if (radio1.isSelected()) {
            radio2.setSelected(false);
        }
        if (radio2.isSelected()) {
            radio1.setSelected(false);
        }
        radio1.setStyle("-fx-text-fill: #331394");
        radio2.setStyle("-fx-text-fill: #331394");
    }

    @FXML
    public void navigateToProfile(ActionEvent e) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Parent root = loader.load();

            // Get controller and pass user data
            ProfileController controller = loader.getController();
            if (passenger != null) {
                controller.setUser(passenger);
            } else if (driver != null) {
                controller.setUser(driver);
            }

            stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            scene = new Scene(root, 320, 600);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            System.err.println("Failed to load Profile screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
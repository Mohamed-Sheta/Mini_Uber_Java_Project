package controller;

import Model.Driver;
import services.Request;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DriverAssignedDialogController {

    @FXML
    private Label driverNameLabel;
    @FXML
    private Label carModelLabel;
    @FXML
    private Label licensePlateLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label estimatedTimeLabel;
    @FXML
    private Label estimatedPriceLabel;
    @FXML
    private Button okButton;
    @FXML
    private Button refuseButton;
    @FXML
    private Button cancelRideButton;
    @FXML
    private Button closeButton;
    @FXML
    private HBox initialButtonBox;

    // Chat elements
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatMessagesContainer;
    @FXML
    private HBox cancelButtonBox;
    @FXML
    private VBox dialogContainer;
    @FXML
    private VBox contentWrapper;
    @FXML
    private StackPane chatPlaceholder;

    private Driver assignedDriver;
    private Request rideRequest;
    private boolean accepted = false;
    private Runnable onAcceptCallback;
    private Runnable onCancelCallback;
    private long rideId = 0; // Track ride ID for chat

    /**
     * Initialize chat UI
     */
    @FXML
    public void initialize() {
        if (chatMessagesContainer != null) {
            chatMessagesContainer.getChildren().clear();
            System.out.println("[DriverAssignedDialog] Chat initialized");
        }
        // Start polling for new messages
        startChatPolling();
    }

    /**
     * Set the ride ID for chat
     */
    public void setRideId(long rideId) {
        this.rideId = rideId;
        loadChatHistory();
    }

    public void setDriverInfo(Driver driver, Request request) {
        this.assignedDriver = driver;
        this.rideRequest = request;

        if (driver != null) {
            driverNameLabel.setText(driver.getName());
            carModelLabel.setText(driver.getCarModel());
            licensePlateLabel.setText(driver.getLicensePlate());
            phoneLabel.setText(driver.getPhoneNumber());
        }

        if (request != null) {
            estimatedTimeLabel.setText(request.getEstimatedTime() + " min");
            estimatedPriceLabel.setText(String.format("%.2f EGP", request.getEstimatedPrice()));
        }
    }

    public void setOnAcceptCallback(Runnable callback) {
        this.onAcceptCallback = callback;
    }

    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    @FXML
    public void onOk() {
        accepted = true;

        // Hide initial buttons (Start Ride / Refuse)
        if (initialButtonBox != null) {
            initialButtonBox.setVisible(false);
            initialButtonBox.setManaged(false);
        }

        // Show Cancel Ride button inside the panel
        showCancelButton();

        // Trigger the ride workflow
        if (onAcceptCallback != null) {
            onAcceptCallback.run();
        }
    }

    @FXML
    public void onRefuse() {
        accepted = false;
        closeDialog();
        // Just close - no database operations
        System.out.println("Ride refused by passenger. Dialog closed.");
    }

    @FXML
    public void onCancelRide() {
        // Trigger cancel callback
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
        closeDialog();
    }

    @FXML
    public void onCloseButtonClick() {
        // X button clicked - close dialog without canceling ride
        // This only hides the panel, keeping the ride logic intact
        System.out.println("[DriverAssignedDialogController] X button clicked - closing panel (ride continues)");
        closeDialog();
    }

    @FXML
    public void onOverlayClick(MouseEvent event) {
        // Hit-testing: Check if click coordinates are inside the panel bounds
        if (contentWrapper != null) {
            // Get the bounds of the content wrapper in the scene coordinate system
            javafx.geometry.Bounds panelBounds = contentWrapper.localToScene(contentWrapper.getBoundsInLocal());

            // Get click coordinates in scene coordinate system
            double clickX = event.getSceneX();
            double clickY = event.getSceneY();

            // Check if click is inside panel bounds
            boolean clickedInsidePanel = panelBounds.contains(clickX, clickY);

            if (!clickedInsidePanel) {
                // Clicked outside the panel - close dialog
                System.out.println("[DriverAssignedDialogController] Clicked outside panel at (" +
                        clickX + ", " + clickY + ") - closing dialog");
                closeDialog();
            } else {
                // Clicked inside the panel - keep open
                System.out.println("[DriverAssignedDialogController] Clicked inside panel at (" +
                        clickX + ", " + clickY + ") - keeping dialog open");
            }
        }
    }

    @FXML
    public void onPanelClick(MouseEvent event) {
        // No longer needed - hit-testing in onOverlayClick handles this
        // Keeping for backward compatibility
    }

    /**
     * Check if a node is contained within a parent node
     * @deprecated No longer needed with current implementation
     */
    private boolean isNodeInsideParent(Node node, Node parent) {
        if (node == null) {
            return false;
        }
        if (node == parent) {
            return true;
        }
        // Traverse up the parent hierarchy
        Node current = node;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @FXML
    public void onDialogClick(MouseEvent event) {
        // No longer needed - keeping for backward compatibility
        event.consume();
    }

    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void disableCancelButton() {
        if (cancelRideButton != null && cancelButtonBox != null) {
            // Hide the cancel button entirely
            cancelButtonBox.setVisible(false);
            cancelButtonBox.setManaged(false);
            System.out.println("[DriverAssignedDialogController] Cancel button hidden - passenger is onboard");
        }
    }

    public void hideDriverPanel() {
        if (dialogContainer != null) {
            // Hide the entire driver info panel
            dialogContainer.setVisible(false);
            dialogContainer.setManaged(false);
            System.out.println("[DriverAssignedDialogController] Driver panel hidden - ride started");
        }
    }

    public void showCancelButton() {
        if (cancelButtonBox != null) {
            // Show red Cancel Ride button inside the panel
            cancelButtonBox.setVisible(true);
            cancelButtonBox.setManaged(true);
            System.out.println("[DriverAssignedDialogController] Cancel Ride button shown inside panel");
        }
    }

    // ==================== CHAT FUNCTIONALITY ====================

    /**
     * Send passenger message to chat
     */
    private void sendPassengerMessage(String message) {
        if (rideId == 0) {
            System.err.println("[DriverAssignedDialog] Cannot send message - ride ID not set");
            return;
        }

        Model.ChatMessage chatMsg = new Model.ChatMessage(message, false); // false = passenger
        utils.ChatStorage.getInstance().addMessage(rideId, chatMsg);
        displayChatMessage(chatMsg);
    }

    /**
     * Display a chat message in the UI
     */
    private void displayChatMessage(Model.ChatMessage message) {
        if (chatMessagesContainer == null) return;

        Platform.runLater(() -> {
            // Create message bubble
            Label msgLabel = new Label(message.getText());
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(220);
            msgLabel.setPadding(new Insets(6, 10, 6, 10));

            // Style based on sender
            if (message.isDriver()) {
                // Driver message (green, right-aligned)
                msgLabel.setStyle("-fx-background-color: #238636; -fx-text-fill: white; " +
                                "-fx-background-radius: 10; -fx-font-size: 11px;");
            } else {
                // Passenger message (blue, left-aligned)
                msgLabel.setStyle("-fx-background-color: #1F6FEB; -fx-text-fill: white; " +
                                "-fx-background-radius: 10; -fx-font-size: 11px;");
            }

            // Time label
            Label timeLabel = new Label(message.getTimestamp());
            timeLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-size: 8px;");

            // Container for message
            VBox msgBox = new VBox(3, msgLabel, timeLabel);
            msgBox.setAlignment(message.isDriver() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            chatMessagesContainer.getChildren().add(msgBox);

            // Auto-scroll to bottom
            if (chatScrollPane != null) {
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            }
        });
    }

    /**
     * Load existing chat messages when dialog opens
     */
    private void loadChatHistory() {
        if (rideId == 0) return;

        List<Model.ChatMessage> messages = utils.ChatStorage.getInstance().getMessages(rideId);
        for (Model.ChatMessage msg : messages) {
            displayChatMessage(msg);
        }
    }

    /**
     * Poll for new messages from driver
     */
    private Timer chatPollingTimer;
    private int lastMessageCount = 0;

    private void startChatPolling() {
        chatPollingTimer = new Timer(true);
        chatPollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (rideId == 0) return;

                List<Model.ChatMessage> messages = utils.ChatStorage.getInstance().getMessages(rideId);
                if (messages.size() > lastMessageCount) {
                    // New messages arrived
                    for (int i = lastMessageCount; i < messages.size(); i++) {
                        displayChatMessage(messages.get(i));
                    }
                    lastMessageCount = messages.size();
                }
            }
        }, 1000, 1000); // Check every 1 second
    }

    // Quick message buttons for passenger
    @FXML
    private void onPassengerQuickMessage1() {
        sendPassengerMessage("Where are you?");
    }

    @FXML
    private void onPassengerQuickMessage2() {
        sendPassengerMessage("How long until you arrive?");
    }

    @FXML
    private void onPassengerQuickMessage3() {
        sendPassengerMessage("I'm waiting at the pickup point.");
    }
}

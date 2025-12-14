package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
public class ChatViewController {
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesBox;
    @FXML private VBox quickButtonsBox;
    @FXML private Button clearChatButton;
    @FXML private Button closeChatButton;
    @FXML private TextField messageTextField;
    @FXML private Button sendButton;
    private long currentRideId = 0;
    private boolean isDriver = false;
    private boolean rideAcceptedMessageSent = false;
    private Runnable onCloseCallback = null;
    @FXML
    public void initialize() {
        System.out.println("[ChatViewController] Initialized");
        if (messagesBox != null) {
            messagesBox.setVisible(true);
            messagesBox.setManaged(true);
            messagesBox.getChildren().clear();
        }
        if (messagesScrollPane != null) {
            messagesScrollPane.setVisible(true);
            messagesScrollPane.setManaged(true);
        }
        if (messageTextField != null) {
            messageTextField.setOnAction(e -> onSendMessage());
        }
    }
    public void setChatSession(long rideId, boolean isDriver) {
        this.currentRideId = rideId;
        this.isDriver = isDriver;
        System.out.println("[ChatViewController] Chat session set - Ride ID: " + rideId + ", IsDriver: " + isDriver);
        setupQuickButtons();
        Platform.runLater(() -> {
            if (messagesBox != null) {
                messagesBox.setVisible(true);
                messagesBox.setManaged(true);
                messagesBox.layout();
            }
            if (messagesScrollPane != null) {
                messagesScrollPane.setVisible(true);
                messagesScrollPane.setManaged(true);
                messagesScrollPane.layout();
            }
        });
    }
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    private void setupQuickButtons() {
        if (quickButtonsBox == null) return;

        quickButtonsBox.getChildren().clear();
            // Passenger quick messages (trigger driver auto-reply)
            addQuickButton("📍 Where are you?", "Where are you?");
            addQuickButton("⏰ How long until you arrive?", "How long until you arrive?");
            addQuickButton("👋 I'm waiting at the pickup point", "I'm waiting at the pickup point");
    }
    private void addQuickButton(String displayText, String messageText) {
        Button btn = new Button(displayText);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(36);
        btn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
                    "-fx-padding: 8 14;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #60A5FA; -fx-text-fill: white; " +
            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
            "-fx-padding: 8 14;"));

        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #3B82F6; -fx-text-fill: white; " +
            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
            "-fx-padding: 8 14;"));
        btn.setOnAction(e -> sendPassengerMessage(messageText));
        quickButtonsBox.getChildren().add(btn);
    }
    private void sendPassengerMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        addMessageToChat(text, false);
        sendDriverReply(text);
    }
    private void sendDriverReply(String passengerText) {
        String driverReply;
        if (passengerText.equals("Where are you?")) {
            driverReply = "I'm 2 minutes away from your location.";
        } else if (passengerText.equals("How long until you arrive?")) {
            driverReply = "Almost there! Just a couple of minutes.";
        } else if (passengerText.equals("I'm waiting at the pickup point")) {
            driverReply = "Great! I'm heading to you now.";
        } else if (passengerText.toLowerCase().contains("where") || passengerText.toLowerCase().contains("location")) {
            driverReply = "I can see your location on the map. On my way!";
        } else if (passengerText.toLowerCase().contains("how long") || passengerText.toLowerCase().contains("time")) {
            driverReply = "I'll be there very soon!";
        } else if (passengerText.toLowerCase().contains("waiting") || passengerText.toLowerCase().contains("here")) {
            driverReply = "Thanks for letting me know. I'm coming!";
        } else if (passengerText.toLowerCase().contains("thanks") || passengerText.toLowerCase().contains("thank you")) {
            driverReply = "You're welcome! See you soon.";
        } else if (passengerText.toLowerCase().contains("hello") || passengerText.toLowerCase().contains("hi")) {
            driverReply = "Hello! I'm on my way to pick you up.";
        } else {
            driverReply = "Got it! I'm on the way.";
        }
        final String reply = driverReply;
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> addMessageToChat(reply, true));
            }
        }, 500); // 500ms delay
    }
    private void addMessageToChat(String text, boolean isFromDriver) {
        if (messagesBox == null || text == null || text.trim().isEmpty()) return;

        Platform.runLater(() -> {
            Label msgLabel = new Label(text);
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(340);
            msgLabel.setMinWidth(100);
            msgLabel.setPadding(new Insets(14, 14, 14, 14));
            if (isFromDriver) {
                msgLabel.setStyle("-fx-background-color: #238636; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 14px;");
            } else {
                msgLabel.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 14px;");
            }
            String timestamp = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
            Label timeLabel = new Label(timestamp);
            timeLabel.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 11px;");
            VBox msgBox = new VBox(5, msgLabel, timeLabel);
            msgBox.setAlignment(isFromDriver ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            msgBox.setPadding(new Insets(0, 0, 10, 0));
            messagesBox.getChildren().add(msgBox);
            messagesBox.layout();
            if (messagesScrollPane != null) {
                messagesScrollPane.layout();
                messagesScrollPane.setVvalue(1.0);
            }
        });
    }
    @FXML
    private void onClearChat() {
        if (messagesBox != null) {
            messagesBox.getChildren().clear();
            System.out.println("[ChatViewController] Chat cleared");
        }
    }
    @FXML
    private void onSendMessage() {
        if (messageTextField == null || messageTextField.getText().trim().isEmpty()) {
            return;
        }
        String messageText = messageTextField.getText().trim();
        sendPassengerMessage(messageText);
        messageTextField.clear();
        Platform.runLater(() -> messageTextField.requestFocus());
    }
    @FXML
    private void onCloseChat() {
        System.out.println("[ChatViewController] Close chat clicked");
        if (onCloseCallback != null) {
            onCloseCallback.run();
            System.out.println("[ChatViewController] Close callback executed");
        } else {
            hideChatPanel();
        }
    }
    private void hideChatPanel() {
        try {
            if (closeChatButton != null && closeChatButton.getScene() != null) {
                javafx.scene.Parent root = closeChatButton.getScene().getRoot();
                if (root instanceof StackPane) {
                    StackPane stackPane = (StackPane) root;
                    for (Node node : stackPane.getChildren()) {
                        if (node instanceof VBox && "chatSidePanel".equals(node.getId())) {
                            node.setVisible(false);
                            node.setManaged(false);
                            node.setPickOnBounds(false);
                            System.out.println("[ChatViewController]  Chat panel hidden");
                            return;
                        }
                    }
                }
            }
            System.out.println("[ChatViewController]  Could not find chat panel to hide");
        } catch (Exception e) {
            System.err.println("[ChatViewController]  Error hiding chat panel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void cleanup() {
        System.out.println("[ChatViewController] Cleanup called");
    }
}


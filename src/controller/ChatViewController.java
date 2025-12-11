package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;

/**
 * Controller for the standalone Chat View
 * Manages chat messages, quick buttons, and chat interactions
 */
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

    // Callback for close button action
    private Runnable onCloseCallback = null;

    /**
     * Initialize the chat view
     */
    @FXML
    public void initialize() {
        System.out.println("[ChatViewController] Initialized");

        // Ensure messagesBox is visible and managed
        if (messagesBox != null) {
            messagesBox.setVisible(true);
            messagesBox.setManaged(true);
            messagesBox.getChildren().clear();
        }

        // Ensure ScrollPane is visible
        if (messagesScrollPane != null) {
            messagesScrollPane.setVisible(true);
            messagesScrollPane.setManaged(true);
        }

        // Setup Enter key listener for message text field
        if (messageTextField != null) {
            messageTextField.setOnAction(e -> onSendMessage());
        }

        // DO NOT send any automatic welcome message
    }

    /**
     * Called when the driver accepts the ride
     * Sends the first driver message to the passenger
     */
    public void onRideAccepted() {
        if (!rideAcceptedMessageSent) {
            Platform.runLater(() -> {
                addMessageToChat("🚗 I'm on the way to your pickup location", true);
                rideAcceptedMessageSent = true;
            });
            System.out.println("[ChatViewController] Ride accepted message sent");
        }
    }

    /**
     * Send automated driver message based on ride progress
     * Called by MapController during ride lifecycle
     */
    public void sendAutomatedDriverMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;

        Platform.runLater(() -> {
            addMessageToChat(message, true);
            System.out.println("[ChatViewController] Automated driver message: " + message);
        });
    }

    /**
     * Set the ride ID and user role for this chat session
     */
    public void setChatSession(long rideId, boolean isDriver) {
        this.currentRideId = rideId;
        this.isDriver = isDriver;

        System.out.println("[ChatViewController] Chat session set - Ride ID: " + rideId + ", IsDriver: " + isDriver);

        // Setup quick buttons for user role
        setupQuickButtons();

        // Ensure everything is visible
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

    /**
     * Set callback for close button action
     * This allows MapController to handle the hide animation
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Setup quick message buttons based on user role
     */
    private void setupQuickButtons() {
        if (quickButtonsBox == null) return;

        quickButtonsBox.getChildren().clear();

        if (isDriver) {
            // Driver quick messages (not used in current logic)
            addQuickButton("✅ I have arrived at your location", "I have arrived at your location");
            addQuickButton("⏱️ I will reach you in 5 minutes", "I will reach you in 5 minutes");
            addQuickButton("🚗 I am downstairs", "I am downstairs");
        } else {
            // Passenger quick messages (trigger driver auto-reply)
            addQuickButton("📍 Where are you?", "Where are you?");
            addQuickButton("⏰ How long until you arrive?", "How long until you arrive?");
            addQuickButton("👋 I'm waiting at the pickup point", "I'm waiting at the pickup point");
        }
    }

    /**
     * Add a quick message button
     */
    private void addQuickButton(String displayText, String messageText) {
        Button btn = new Button(displayText);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(36);
        btn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
                    "-fx-padding: 8 14;");

        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #60A5FA; -fx-text-fill: white; " +
            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
            "-fx-padding: 8 14;"));

        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #3B82F6; -fx-text-fill: white; " +
            "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; " +
            "-fx-padding: 8 14;"));

        // Send message when clicked
        btn.setOnAction(e -> sendPassengerMessage(messageText));

        quickButtonsBox.getChildren().add(btn);
    }

    /**
     * Send a passenger message and trigger driver auto-reply
     */
    private void sendPassengerMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Add passenger message to chat (called once per user action)
        addMessageToChat(text, false);

        // Trigger driver reply based on message (called once)
        sendDriverReply(text);
    }

    /**
     * Send driver auto-reply based on passenger message
     */
    private void sendDriverReply(String passengerText) {
        String driverReply;

        // Match passenger messages and generate appropriate replies
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
            // For any other custom text from passenger
            driverReply = "Got it! I'm on the way.";
        }

        // Add driver reply to chat with slight delay for realism
        final String reply = driverReply;
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> addMessageToChat(reply, true));
            }
        }, 500); // 500ms delay
    }

    /**
     * Add a message to the chat display (SINGLE display method)
     */
    private void addMessageToChat(String text, boolean isFromDriver) {
        if (messagesBox == null || text == null || text.trim().isEmpty()) return;

        Platform.runLater(() -> {
            // Create message bubble
            Label msgLabel = new Label(text);
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(340);
            msgLabel.setMinWidth(100);
            msgLabel.setPadding(new Insets(14, 14, 14, 14));

            // Style based on sender
            if (isFromDriver) {
                // Driver message (green, left-aligned)
                msgLabel.setStyle("-fx-background-color: #238636; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 14px;");
            } else {
                // Passenger message (blue, right-aligned)
                msgLabel.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 14px;");
            }

            // Time label
            String timestamp = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
            Label timeLabel = new Label(timestamp);
            timeLabel.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 11px;");

            // Container for message with proper spacing and alignment
            VBox msgBox = new VBox(5, msgLabel, timeLabel);
            // FIX: Passenger (isFromDriver=false) aligns RIGHT, Driver (isFromDriver=true) aligns LEFT
            msgBox.setAlignment(isFromDriver ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            msgBox.setPadding(new Insets(0, 0, 10, 0));

            messagesBox.getChildren().add(msgBox);

            // Force layout update and scroll to bottom
            messagesBox.layout();
            if (messagesScrollPane != null) {
                messagesScrollPane.layout();
                messagesScrollPane.setVvalue(1.0);
            }
        });
    }


    /**
     * Clear chat button action
     */
    @FXML
    private void onClearChat() {
        if (messagesBox != null) {
            messagesBox.getChildren().clear();
            System.out.println("[ChatViewController] Chat cleared");
        }
    }

    /**
     * Send custom message from text field
     */
    @FXML
    private void onSendMessage() {
        if (messageTextField == null || messageTextField.getText().trim().isEmpty()) {
            return;
        }

        String messageText = messageTextField.getText().trim();

        // Send the passenger message
        sendPassengerMessage(messageText);

        // Clear the text field
        messageTextField.clear();

        // Request focus back to text field for next message
        Platform.runLater(() -> messageTextField.requestFocus());
    }

    /**
     * Close chat button action
     */
    @FXML
    private void onCloseChat() {
        System.out.println("[ChatViewController] Close chat clicked");

        // If callback is set (preferred method), use it
        if (onCloseCallback != null) {
            onCloseCallback.run();
            System.out.println("[ChatViewController] Close callback executed");
        } else {
            // Fallback: try to hide the panel directly
            hideChatPanel();
        }
    }

    /**
     * Hide the chat panel (find and hide the parent chat side panel)
     */
    private void hideChatPanel() {
        try {
            // Find the chatSidePanel in the scene graph and hide it
            if (closeChatButton != null && closeChatButton.getScene() != null) {
                javafx.scene.Parent root = closeChatButton.getScene().getRoot();
                if (root instanceof StackPane) {
                    StackPane stackPane = (StackPane) root;
                    // Find the chat side panel by ID
                    for (Node node : stackPane.getChildren()) {
                        if (node instanceof VBox && "chatSidePanel".equals(node.getId())) {
                            // Hide the panel
                            node.setVisible(false);
                            node.setManaged(false);
                            node.setPickOnBounds(false);
                            System.out.println("[ChatViewController] ✅ Chat panel hidden");
                            return;
                        }
                    }
                }
            }
            System.out.println("[ChatViewController] ⚠️ Could not find chat panel to hide");
        } catch (Exception e) {
            System.err.println("[ChatViewController] ❌ Error hiding chat panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        System.out.println("[ChatViewController] Cleanup called");
    }
}


package utils;

import Model.ChatMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory chat storage for ride sessions
 * Maps ride ID to list of chat messages
 * No database persistence - purely for current session communication
 */
public class ChatStorage {
    private static ChatStorage instance;
    private Map<Long, List<ChatMessage>> rideChats;

    private ChatStorage() {
        rideChats = new HashMap<>();
    }

    public static ChatStorage getInstance() {
        if (instance == null) {
            instance = new ChatStorage();
        }
        return instance;
    }

    /**
     * Add a message to a ride's chat
     */
    public void addMessage(long rideId, ChatMessage message) {
        rideChats.computeIfAbsent(rideId, k -> new ArrayList<>()).add(message);
        System.out.println("[ChatStorage] Message added to ride " + rideId + ": " + message.getText());
    }

    /**
     * Get all messages for a ride
     */
    public List<ChatMessage> getMessages(long rideId) {
        return rideChats.getOrDefault(rideId, new ArrayList<>());
    }

    /**
     * Clear chat for a ride (called when ride is completed)
     */
    public void clearRideChat(long rideId) {
        rideChats.remove(rideId);
        System.out.println("[ChatStorage] Chat cleared for ride " + rideId);
    }

    /**
     * Clear all chats (optional - for cleanup)
     */
    public void clearAll() {
        rideChats.clear();
    }
}


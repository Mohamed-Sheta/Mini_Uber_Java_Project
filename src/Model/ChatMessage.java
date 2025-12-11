package Model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple chat message model for in-app communication between Passenger and Driver
 * No database storage - purely in-memory for current ride session
 */
public class ChatMessage {
    private String text;
    private boolean isDriver; // true if sent by driver, false if sent by passenger
    private String timestamp;

    public ChatMessage(String text, boolean isDriver) {
        this.text = text;
        this.isDriver = isDriver;
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getText() {
        return text;
    }

    public boolean isDriver() {
        return isDriver;
    }

    public String getTimestamp() {
        return timestamp;
    }
}


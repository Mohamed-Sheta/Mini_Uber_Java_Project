package Model;

import java.time.LocalDateTime;

public class Report {
    private long id;
    private long userId;
    private ReportType type;
    private String description;
    private LocalDateTime createdAt;

    // Full constructor
    public Report(long id, long userId, ReportType type, String description, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Constructor for new reports (without id)
    public Report(long userId, ReportType type, String description) {
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", userId=" + userId +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


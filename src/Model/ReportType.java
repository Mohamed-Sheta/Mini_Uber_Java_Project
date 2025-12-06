package Model;

public enum ReportType {
    TECHNICAL_ISSUE("Technical Issue"),
    FEATURE_REQUEST("Feature Request"),
    COMPLAINT("Complaint"),
    OTHER("Other");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}


package Model;

import services.RideManager;

import java.time.LocalDateTime;
import java.util.Set;

public class ProblemReport {

    private static int reportIdCounter = 1000;
    private int reportId;
    protected RideManager rideManager;
    private LocalDateTime timestamp;
    private Set<ProblemType> types;
    private String details;

    public ProblemReport(RideManager rideManager, Set<ProblemType> types, String details) {
        this.reportId = ++ProblemReport.reportIdCounter;
        this.rideManager = rideManager;
        this.types = types;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }


    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setTypes(Set<ProblemType> types) {
        this.types = types;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public int getReportId() {
        return reportId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Set<ProblemType> getTypes() {
        return types;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "ProblemReport Details:" +
               "\n  - ID: " + reportId + 
               "\n  - Reported Against Driver: " + rideManager.getCurrentDriver().getName() +
               "\n  - Driver SSN: " + rideManager.getCurrentDriver().getUserSSN() +
               "\n  - Types: " + types.toString() +
               "\n  - Details: " + details;
    }
}
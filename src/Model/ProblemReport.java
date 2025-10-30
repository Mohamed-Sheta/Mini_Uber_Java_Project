package com.mycompany.uper;

import java.time.LocalDateTime;
import java.util.Set;

public class ProblemReport {
    
    private static int reportId = 1000;
//    protected RideManager rideManager;
    private LocalDateTime timestamp;
    private Set<ProblemType> types;
    private String details;

    public static void setReportId(int reportId) {
        ProblemReport.reportId = reportId;
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

    public static int getReportId() {
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
//    public ProblemReport(RideManager rideManager, Set<ProblemType> types, String details) {
//        // ID Assignment and Increment
//        this.reportId++; 
//        this.rideManager = rideManager; 
//        this.types = types;
//        this.details = details;
//        this.timestamp = LocalDateTime.now(); 
//    }

    @Override
    public String toString() {
        // Accessing data through the RideManager
        return "ProblemReport Details:" +
               "\n  - ID: " + reportId + 
//               "\n  - Reported Against Driver: " + rideManager.getAssignedDriver().getName() +
               "\n  - Types: " + types.toString() +
               "\n  - Details: " + details;
    }
}
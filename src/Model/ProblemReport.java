/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;
import java.util.Set;

/**
 *
 * @author Mohamed
 */

import java.time.LocalDateTime;
import java.util.Set;

public class ProblemReport {
    
    private static int reportId = 1000;
//    protected RideManager rideManager;
private LocalDateTime timestamp;
    private Set<ProblemType> types;
    private String details;
    

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

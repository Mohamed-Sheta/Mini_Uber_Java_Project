package services;
import Model.*;
import DAO.ProblemReportTypeDAO;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class Request {

    private static int requestCounter = 1;
    private int requestId;
    private Passenger passenger;
    private Location origin;
    private Location destination;
    private Status status;
    private double distance;
    private int estimatedTime;
    private double estimatedPrice;

    private long dbId;

    /**
     * Static helper to initialize database (reset and seed problem types)
     */
    public static class DatabaseInitializer {
        private ProblemReportTypeDAO problemReportTypeDAO;

        public DatabaseInitializer() {
            this.problemReportTypeDAO = new ProblemReportTypeDAO();
        }

        /**
         * Reset all database tables
         */
        public void resetDatabase() {
            try {
                Connection con = utils.DBConnection.getConnection();

                try (var ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS=0")) {
                    ps.execute();
                }

                String[] tables = {
                    "problem_report_types", "problem_reports",
                    "ride_history", "ride_requests",
                    "edges", "locations",
                    "drivers", "passengers",
                    "problem_types"
                };

                for (String table : tables) {
                    try (var ps = con.prepareStatement("TRUNCATE TABLE " + table)) {
                        ps.execute();
                    }
                }

                try (var ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS=1")) {
                    ps.execute();
                }

                System.out.println("[DB] All tables truncated.\n");
            } catch (Exception e) {
                System.out.println("[DB] Reset error: " + e.getMessage());
            }
        }

        /**
         * Seed problem types
         */
        public void seedProblemTypes() {
            try {
                problemReportTypeDAO.initializeProblemTypes();
                System.out.println("[DB] problem_types seeded.\n");
            } catch (Exception e) {
                System.out.println("[DB] Seed problem_types error: " + e.getMessage());
            }
        }

        /**
         * Get problem type ID mapping
         */
        public Map<ProblemType, Integer> getProblemTypeMapping() {
            Map<ProblemType, Integer> mapping = new HashMap<>();
            mapping.put(ProblemType.DRIVER_BEHAVIOR, 1);
            mapping.put(ProblemType.DRIVER_LATE, 2);
            mapping.put(ProblemType.RECKLESS_DRIVING, 3);
            mapping.put(ProblemType.VEHICLE_CLEANLINESS, 4);
            mapping.put(ProblemType.TECHNICAL_ISSUE, 5);
            mapping.put(ProblemType.FARE_DISPUTE, 6);
            mapping.put(ProblemType.ACCOUNT_ISSUE, 7);
            return mapping;
        }

        /**
         * Complete initialization
         */
        public Map<ProblemType, Integer> initialize(boolean resetDB) {
            if (resetDB) {
                resetDatabase();
            }
            seedProblemTypes();
            return getProblemTypeMapping();
        }
    }
    public Request(Passenger passenger, Location origin, Location destination, Status status, MapGraph mapGraph) {
        this.requestId = requestCounter++;
        this.passenger = passenger;
        this.origin =origin;
        this.destination = destination;
        this.status = status;

        this.distance = mapGraph.shortestDistance(origin, destination);
        if (this.distance == Double.MAX_VALUE) {
            System.out.println(" ERROR: Request " + this.requestId + " failed. No valid path found from " + origin.getName() + " to " + destination.getName());

            this.distance = 0.0;
            this.estimatedTime = 0;
            this.estimatedPrice = 0.0;
            this.status = Status.Cancelled;

            requestCounter--;
            return;
        }
        this.estimatedTime = calculateEstimatedTime(distance);
        this.estimatedPrice = calculateEstimatedPrice(distance);
    }
    public void setDbId(long id) { this.dbId = id; }

    public long getDatabaseId() { return this.dbId; }

    public int getRequestId() {
        return requestId;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location getDestination() {
        return destination;
    }

    public Status getStatus() {
        return status;
    }

    public double getDistance() {
        return distance;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public int calculateEstimatedTime(double distance) {
        double avgSpeedKmPerHour = 60.0;
        double timeInHours = distance / avgSpeedKmPerHour;
        return (int) (timeInHours * 60);
    }

    private double calculateEstimatedPrice(double distance) {
        double baseFare = 11.5;
        double ratePerKm = 4.0;
        return baseFare + (distance * ratePerKm);
    }

    @Override
    public String toString() {
        return "Request{" +
                "requestId=" + requestId +
                ", passenger=" + passenger +
                ", origin=" + origin +
                ", destination=" + destination +
                ", status=" + status +
                ", distance=" + distance +
                ", estimatedTime=" + estimatedTime +
                ", estimatedPrice=" + estimatedPrice +
                '}';
    }

    /**
     * Static helper to submit problem reports
     */
    public static void submitProblemReport(long rideRequestId, long passengerId, long driverId,
                                          ProblemType problemType, String description,
                                          Map<ProblemType, Integer> problemTypeMapping) {
        try {
            DAO.ProblemReportDAO problemReportDAO = new DAO.ProblemReportDAO();
            DAO.ProblemReportTypeDAO problemReportTypeDAO = new DAO.ProblemReportTypeDAO();

            long reportId = problemReportDAO.insertReport(rideRequestId, passengerId, driverId);

            Integer typeId = problemTypeMapping.get(problemType);
            if (typeId != null) {
                problemReportTypeDAO.insert(reportId, typeId, description);
                System.out.println("[DB] problem_reports inserted id=" + reportId +
                                 " (Type: " + problemType + ")");
            } else {
                System.out.println("[DB] Invalid problem type: " + problemType);
            }
        } catch (Exception e) {
            System.out.println("[DB] Insert problem_report error: " + e.getMessage());
        }
    }
}
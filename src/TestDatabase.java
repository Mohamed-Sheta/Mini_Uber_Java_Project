import DAO.*;
import Model.*;
import Model.Driver;
import utils.connection;

import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestDatabase {
    public static void main(String[] args) {
        Connection conn = null;

        try {
            // Initialize connection
            conn = connection.getConnection();
            System.out.println("✅ Connected to database successfully!\n");

            // Clear tables to avoid duplicate key errors
            clearTables(conn);

            // Initialize DAO objects
            LocationDAO locationDAO = new LocationDAO();
            EdgesDAO edgesDAO = new EdgesDAO();
            DriverDAO driverDAO = new DriverDAO();
            PassengerDAO passengerDAO = new PassengerDAO();
            OptionDAO optionDAO = new OptionDAO();
            PaymentDAO paymentDAO = new PaymentDAO();
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();
            ProblemReportDAO problemReportDAO = new ProblemReportDAO();

            // -------------------------------
            // 1️⃣ Test LocationDAO
            // -------------------------------
            System.out.println("📍 Testing LocationDAO...");
            Location location1 = new Location("Nasr City", 30.05, 31.33);
            Location location2 = new Location("Airport", 30.11, 31.40);
            Location location3 = new Location("Zamalek", 30.06, 31.22);
            Location location4 = new Location("Maadi", 29.96, 31.25);
            locationDAO.saveOrGetLocation(location1);
            locationDAO.saveOrGetLocation(location2);
            locationDAO.saveOrGetLocation(location3);
            locationDAO.saveOrGetLocation(location4);
            System.out.println("✅ Locations inserted: Nasr City, Airport, Zamalek, Maadi");

            // Verify location retrieval
            Location retrievedLocation = locationDAO.getLocationByNameLatLon("Nasr City", 30.05, 31.33);
            if (retrievedLocation != null) {
                System.out.println("✅ Retrieved location: " + retrievedLocation.getName());
            } else {
                System.out.println("❌ Failed to retrieve location: Nasr City");
            }

            // -------------------------------
            // 2️⃣ Test EdgesDAO
            // -------------------------------
            System.out.println("\n🛤️ Testing EdgesDAO...");
            Edge edge1 = new Edge(location1, location2, 15.5, 25);
            Edge edge2 = new Edge(location2, location3, 10.0, 15);
            Edge edge3 = new Edge(location3, location4, 12.0, 20);
            Edge edge4 = new Edge(location4, location1, 8.5, 18);
            edgesDAO.insertEdge(edge1);
            edgesDAO.insertEdge(edge2);
            edgesDAO.insertEdge(edge3);
            edgesDAO.insertEdge(edge4);
            System.out.println("✅ Edges inserted between locations");

            // Retrieve all edges
            List<Edge> edges = edgesDAO.getAllEdges();
            for (Edge e : edges) {
                System.out.println(" - Edge: " + e.getFrom().getName() + " to " + e.getTo().getName() + ", Distance: " + e.getDistance());
            }

            // -------------------------------
            // 3️⃣ Test DriverDAO
            // -------------------------------
            System.out.println("\n🚗 Testing DriverDAO...");
            Driver driver1 = new Driver("LIC999", "Hyundai Elantra", true, location1, "111222333", "Ahmed Mostafa", "0112222333", "ahmed@example.com", 100.0, 50.0, 4.0);
            Driver driver2 = new Driver("LIC1000", "Toyota Corolla", true, location2, "111222334", "Mohamed Ali", "0112222334", "mohamed@example.com", 120.0, 30.0, 4.5);
            Driver driver3 = new Driver("LIC1001", "Kia Sportage", true, location3, "111222335", "Khaled Hassan", "0112222335", "khaled@example.com", 80.0, 20.0, 4.2);
            Driver driver4 = new Driver("LIC1002", "Honda Civic", true, location4, "111222336", "Sara Ahmed", "0112222336", "sara@example.com", 150.0, 60.0, 4.8);
            driverDAO.addDriver(driver1);
            driverDAO.addDriver(driver2);
            driverDAO.addDriver(driver3);
            driverDAO.addDriver(driver4);
            System.out.println("✅ Drivers inserted: Ahmed, Mohamed, Khaled, Sara");

            // Test retrieving driver
            Driver retrievedDriver = driverDAO.getDriverBySSN("111222333");
            if (retrievedDriver != null) {
                System.out.println("✅ Retrieved driver: " + retrievedDriver.getName() + " | " + retrievedDriver.getEmail());
            } else {
                System.out.println("❌ Failed to retrieve driver: 111222333");
            }

            // Test adding amount to driver wallet
            driverDAO.addAmount("111222333", 50.0);
            retrievedDriver = driverDAO.getDriverBySSN("111222333");
            if (retrievedDriver != null) {
                System.out.println("✅ Driver new wallet balance: $" + retrievedDriver.getWalletBalance());
            }

            // -------------------------------
            // 4️⃣ Test PassengerDAO
            // -------------------------------
            System.out.println("\n🧍 Testing PassengerDAO...");
            Passenger passenger1 = new Passenger(location1, "222333444", "Omar Khaled", "0101234567", "omar@example.com", 150.0, 50.0, 5.0);
            passenger1.setDestination(location2);
            Passenger passenger2 = new Passenger(location2, "222333445", "Laila Mostafa", "0101234568", "laila@example.com", 200.0, 40.0, 4.7);
            passenger2.setDestination(location3);
            Passenger passenger3 = new Passenger(location3, "222333446", "Youssef Amr", "0101234569", "youssef@example.com", 100.0, 30.0, 4.3);
            passenger3.setDestination(location4);
            Passenger passenger4 = new Passenger(location4, "222333447", "Fatima Zahr", "0101234570", "fatima@example.com", 180.0, 60.0, 4.9);
            passenger4.setDestination(location1);
            passengerDAO.addPassenger(passenger1);
            passengerDAO.addPassenger(passenger2);
            passengerDAO.addPassenger(passenger3);
            passengerDAO.addPassenger(passenger4);
            System.out.println("✅ Passengers inserted: Omar, Laila, Youssef, Fatima");

            // Test retrieving passenger
            Passenger retrievedPassenger = passengerDAO.getPassengerBySSN("222333444");
            if (retrievedPassenger != null) {
                System.out.println("✅ Retrieved passenger: " + retrievedPassenger.getName() + " | " + retrievedPassenger.getEmail());
            } else {
                System.out.println("❌ Failed to retrieve passenger: 222333444");
            }

            // -------------------------------
            // 5️⃣ Test OptionDAO
            // -------------------------------
            System.out.println("\n💡 Testing OptionDAO...");
            Option option1 = new Option(10.0f, 5.0f, "CharityOrg1");
            Option option2 = new Option(15.0f, 10.0f, "CharityOrg2");
            Option option3 = new Option(8.0f, 0.0f, null);
            Option option4 = new Option(12.0f, 7.0f, "CharityOrg3");
            int optionId1 = optionDAO.addOption(option1);
            int optionId2 = optionDAO.addOption(option2);
            int optionId3 = optionDAO.addOption(option3);
            int optionId4 = optionDAO.addOption(option4);
            System.out.println("✅ Options inserted with IDs: " + optionId1 + ", " + optionId2 + ", " + optionId3 + ", " + optionId4);

            // Retrieve option
            Option retrievedOption = optionDAO.getOptionById(optionId1);
            if (retrievedOption != null) {
                System.out.println("✅ Retrieved option with tips: " + retrievedOption.getTips());
            } else {
                System.out.println("❌ Failed to retrieve option: " + optionId1);
            }

            // -------------------------------
            // 6️⃣ Test PaymentDAO
            // -------------------------------
            System.out.println("\n💳 Testing PaymentDAO...");
            Payment payment1 = new Payment(0, 120.0, PaymentType.credit, null);
            Payment payment2 = new Payment(0, 80.0, PaymentType.wallet, null);
            Payment payment3 = new Payment(0, 100.0, PaymentType.credit, null);
            Payment payment4 = new Payment(0, 150.0, PaymentType.wallet, null);
            boolean paymentSuccess1 = paymentDAO.addPayment(payment1, optionId1);
            boolean paymentSuccess2 = paymentDAO.addPayment(payment2, optionId2);
            boolean paymentSuccess3 = paymentDAO.addPayment(payment3, optionId3);
            boolean paymentSuccess4 = paymentDAO.addPayment(payment4, optionId4);
            if (paymentSuccess1) {
                System.out.println("✅ Payment inserted with ID: " + payment1.getPaymentId());
            } else {
                System.out.println("❌ Failed to insert payment 1");
            }
            if (paymentSuccess2) {
                System.out.println("✅ Payment inserted with ID: " + payment2.getPaymentId());
            } else {
                System.out.println("❌ Failed to insert payment 2");
            }
            if (paymentSuccess3) {
                System.out.println("✅ Payment inserted with ID: " + payment3.getPaymentId());
            } else {
                System.out.println("❌ Failed to insert payment 3");
            }
            if (paymentSuccess4) {
                System.out.println("✅ Payment inserted with ID: " + payment4.getPaymentId());
            } else {
                System.out.println("❌ Failed to insert payment 4");
            }

            // -------------------------------
            // 7️⃣ Test RideHistoryDAO
            // -------------------------------
            System.out.println("\n🕓 Testing RideHistoryDAO...");
            RideHistory rideHistory1 = new RideHistory(0, driver1, passenger1, 5, 4);
            RideHistory rideHistory2 = new RideHistory(0, driver2, passenger2, 4, 5);
            RideHistory rideHistory3 = new RideHistory(0, driver3, passenger3, 3, 4);
            RideHistory rideHistory4 = new RideHistory(0, driver4, passenger4, 5, 5);
            boolean rideSuccess1 = rideHistoryDAO.addRideHistory(rideHistory1);
            boolean rideSuccess2 = rideHistoryDAO.addRideHistory(rideHistory2);
            boolean rideSuccess3 = rideHistoryDAO.addRideHistory(rideHistory3);
            boolean rideSuccess4 = rideHistoryDAO.addRideHistory(rideHistory4);
            if (rideSuccess1) {
                System.out.println("✅ RideHistory inserted with ID: " + rideHistory1.getHistoryId());
            }
            if (rideSuccess2) {
                System.out.println("✅ RideHistory inserted with ID: " + rideHistory2.getHistoryId());
            }
            if (rideSuccess3) {
                System.out.println("✅ RideHistory inserted with ID: " + rideHistory3.getHistoryId());
            }
            if (rideSuccess4) {
                System.out.println("✅ RideHistory inserted with ID: " + rideHistory4.getHistoryId());
            }

            // Retrieve ride history
            RideHistory retrievedRide = rideHistoryDAO.getRideHistoryById(rideHistory1.getHistoryId());
            if (retrievedRide != null) {
                System.out.println("✅ Retrieved RideHistory: Driver=" + retrievedRide.getDriver().getName() +
                        ", Passenger=" + retrievedRide.getPassenger().getName());
            } else {
                System.out.println("❌ Failed to retrieve RideHistory: " + rideHistory1.getHistoryId());
            }

            // Update ratings
            rideHistoryDAO.updateRatings(rideHistory1.getHistoryId(), 4, 5);
            System.out.println("✅ Updated ratings for RideHistory ID: " + rideHistory1.getHistoryId());

            // Get ride histories by user
            List<RideHistory> userHistories = rideHistoryDAO.getRideHistoriesByUser("222333444");
            System.out.println("✅ Retrieved " + userHistories.size() + " ride histories for passenger SSN: 222333444");

            // -------------------------------
            // 8️⃣ Test ProblemReportDAO
            // -------------------------------
            System.out.println("\n🚨 Testing ProblemReportDAO...");
            Set<ProblemType> problemTypes = new HashSet<>();
            problemTypes.add(ProblemType.DRIVER_BEHAVIOR);
            problemTypes.add(ProblemType.FARE_DISPUTE);
            int reportId = problemReportDAO.addProblemReport(rideHistory1.getHistoryId(), "Driver was rude and overcharged", problemTypes);
            System.out.println("✅ ProblemReport inserted with ID: " + reportId);

            // -------------------------------
            // 9️⃣ Final SELECT tests
            // -------------------------------
            System.out.println("\n📋 Fetching all drivers:");
            ResultSet rsDrivers = conn.prepareStatement("SELECT * FROM Driver").executeQuery();
            while (rsDrivers.next()) {
                System.out.println(" - " + rsDrivers.getString("name") + " | " + rsDrivers.getString("email"));
            }

            System.out.println("\n📋 Fetching all passengers:");
            ResultSet rsPassengers = conn.prepareStatement("SELECT * FROM Passenger").executeQuery();
            while (rsPassengers.next()) {
                System.out.println(" - " + rsPassengers.getString("name") + " | " + rsPassengers.getString("email"));
            }

            System.out.println("\n✅ All database tests completed successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("\n🔒 Connection closed.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clearTables(Connection conn) throws SQLException {
        String[] tables = {"ProblemReport", "RideHistory", "Payment", "Options", "Passenger", "Driver", "Edge", "Location"};
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0"); // Disable foreign key checks
            for (String table : tables) {
                stmt.executeUpdate("TRUNCATE TABLE " + table);
                System.out.println("✅ Truncated table: " + table);
            }
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1"); // Re-enable foreign key checks
        } catch (SQLException e) {
            System.err.println("❌ Failed to truncate tables: " + e.getMessage());
            throw e;
        }
    }
}
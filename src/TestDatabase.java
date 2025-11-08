import DAO.*;
import Model.*;
import services.MapGraph;
import services.Request;
import utils.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class TestDatabase {

    public static void main(String[] args) {
        try (Connection conn = connection.getConnection()) {
            System.out.println("✅ Connected to database.");

            clearTables(conn);
            insertBaseProblemTypes(conn);

            LocationDAO locationDAO = new LocationDAO();
            EdgesDAO edgesDAO = new EdgesDAO();
            DriverDAO driverDAO = new DriverDAO();
            PassengerDAO passengerDAO = new PassengerDAO();
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();
            ProblemReportDAO problemReportDAO = new ProblemReportDAO();

            // ------------------------------------------
            // 1) Insert Locations
            // ------------------------------------------
            Location[] locations = {
                    new Location("Nasr City"),
                    new Location("Airport"),
                    new Location("Zamalek"),
                    new Location("Maadi"),
                    new Location("Heliopolis")
            };

            for (Location loc : locations) locationDAO.save(loc);
            System.out.println("✅ Inserted Locations.");

            // ------------------------------------------
            // 2) Insert Edges
            // ------------------------------------------
            edgesDAO.insertEdge(new Edge(locations[0], locations[1], 10));
            edgesDAO.insertEdge(new Edge(locations[1], locations[2], 7));
            edgesDAO.insertEdge(new Edge(locations[2], locations[3], 5));
            edgesDAO.insertEdge(new Edge(locations[3], locations[4], 6));
            edgesDAO.insertEdge(new Edge(locations[4], locations[0], 9));
            System.out.println("✅ Inserted Edges.");

            MapGraph mapGraph = new MapGraph();
            for (Edge e : edgesDAO.getAllEdges()) {
                mapGraph.addEdge(e.getFrom(), e.getTo(), e.getDistance());
            }
            System.out.println("✅ MapGraph Loaded.");

            // ------------------------------------------
            // 3) Insert Drivers
            // ------------------------------------------
            Driver[] drivers = {
                    new Driver("ABC111", "Kia", true, "1111", "Omar", "010000001", "o@a.com", 100, 25, locations[0], new ArrayList<>()),
                    new Driver("ABC112", "BMW", true, "1112", "Ahmed", "010000002", "a@a.com", 80, 50, locations[1], new ArrayList<>()),
                    new Driver("ABC113", "Honda", true, "1113", "Sara", "010000003", "s@a.com", 140, 35, locations[2], new ArrayList<>()),
                    new Driver("ABC114", "Hyundai", true, "1114", "Khaled", "010000004", "k@a.com", 95, 60, locations[3], new ArrayList<>()),
                    new Driver("ABC115", "Tesla", true, "1115", "Yasmin", "010000005", "y@a.com", 200, 10, locations[4], new ArrayList<>())
            };

            for (Driver d : drivers) driverDAO.addDriver(d);
            System.out.println("✅ Inserted Drivers.");

            // ------------------------------------------
            // 4) Insert Passengers
            // ------------------------------------------
            Passenger[] passengers = {
                    new Passenger("2221", "Nour", "011100001", "n@a.com", 120, 20, locations[0], new ArrayList<>()),
                    new Passenger("2222", "Laila", "011100002", "l@a.com", 90, 10, locations[1], new ArrayList<>()),
                    new Passenger("2223", "Salma", "011100003", "s@a.com", 60, 50, locations[2], new ArrayList<>()),
                    new Passenger("2224", "Mostafa", "011100004", "m@a.com", 200, 0, locations[3], new ArrayList<>()),
                    new Passenger("2225", "Karim", "011100005", "k@a.com", 40, 30, locations[4], new ArrayList<>())
            };

            for (Passenger p : passengers) passengerDAO.addPassenger(p);
            System.out.println("✅ Inserted Passengers.");

            // ------------------------------------------
            // 5) Create Ride Requests + Ride History
            // ------------------------------------------
            for (int i = 0; i < 5; i++) {

                // ✅ mapGraph is now NOT null
                Request request = new Request(passengers[i], locations[i], locations[(i + 1) % 5], Status.Pending, mapGraph);
                long requestDbId = rideRequestDAO.saveRequest(request);

                rideRequestDAO.assignDriver(requestDbId, drivers[i]);

                RideHistory history = new RideHistory(drivers[i], passengers[i], 4, 5, request);

                rideHistoryDAO.addRideHistory(history, request.getEstimatedPrice(), "wallet", 5, 2, "Red Crescent");
            }

            System.out.println("✅ RideRequests + RideHistory inserted.");

            System.out.println("🎉 ALL DONE SUCCESSFULLY 🎉");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearTables(Connection conn) throws SQLException {
        String[] tables = {
                "problem_report_types",
                "problem_reports",
                "ride_history",
                "ride_requests",
                "drivers",
                "passengers",
                "edges",
                "locations",
                "problem_types"
        };
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                stmt.executeUpdate("TRUNCATE TABLE " + table);
            }
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("✅ Database cleared.");
        }
    }

    private static void insertBaseProblemTypes(Connection conn) { }
}

import DAO.*;
import Model.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class TestDatabase {

    public static void main(String[] args) {
        try {
            System.out.println("=== DATABASE TEST START ===\n");

            // 1️⃣ LOCATIONS
            LocationDAO locationDAO = new LocationDAO();
            Location cairo = new Location("Cairo");
            Location giza = new Location("Giza");
            Location alex = new Location("Alexandria");
            long idCairo = locationDAO.insert(cairo);
            long idGiza = locationDAO.insert(giza);
            long idAlex = locationDAO.insert(alex);
            System.out.println("[INSERT] Locations inserted successfully");
            locationDAO.showAll().forEach(System.out::println);

            // Update
            locationDAO.update((int) idAlex, "Alex City");
            System.out.println("\n[UPDATE] Location updated:");
            locationDAO.showAll().forEach(System.out::println);

            // 2️⃣ EDGES
            EdgeDAO edgeDAO = new EdgeDAO();
            edgeDAO.insert(cairo, giza, 20.5);
            edgeDAO.insert(giza, alex, 220.0);
            edgeDAO.insert(cairo, alex, 240.0);
            System.out.println("\n[INSERT] Edges:");
            edgeDAO.showAll().forEach(System.out::println);

            // 3️⃣ PASSENGERS
            PassengerDAO passengerDAO = new PassengerDAO();
            Passenger p1 = new Passenger("PSSN001", "Mohamed Ali", "01000111222", "mohamed@mail.com", 500, 200, cairo, null);
            Passenger p2 = new Passenger("PSSN002", "Ahmed Samir", "01055667788", "ahmed@mail.com", 1000, 500, giza, null);
            long pid1 = passengerDAO.insert(p1, (int) idCairo);
            long pid2 = passengerDAO.insert(p2, (int) idGiza);
            System.out.println("\n[INSERT] Passengers:");
            passengerDAO.showAll().forEach(System.out::println);

            // Update Passenger
            p2.setWalletBalance(1200);
            passengerDAO.update(pid2, p2, (int) idAlex, 5);
            System.out.println("\n[UPDATE] Passenger Ahmed Samir updated:");
            passengerDAO.showAll().forEach(System.out::println);

            // 4️⃣ DRIVERS
            DriverDAO driverDAO = new DriverDAO();
            Driver d1 = new Driver("CAR111", "Toyota Corolla", true, "DSSN001", "Khaled Hassan", "01111111111", "khaled@mail.com", 800, 300, giza, null);
            Driver d2 = new Driver("CAR222", "Nissan Sunny", true, "DSSN002", "Omar Youssef", "01122222222", "omar@mail.com", 1000, 400, cairo, null);
            long did1 = driverDAO.insert(d1, (int) idGiza);
            long did2 = driverDAO.insert(d2, (int) idCairo);
            System.out.println("\n[INSERT] Drivers:");
            driverDAO.showAll().forEach(System.out::println);

            // Update driver
            d1.updateWalletBalance(1200);
            driverDAO.update(did1, d1, (int) idAlex, 4);
            System.out.println("\n[UPDATE] Driver Khaled updated:");
            driverDAO.showAll().forEach(System.out::println);

            // 5️⃣ RIDE REQUESTS
            RideRequestDAO reqDAO = new RideRequestDAO();
            long req1 = reqDAO.insert(pid1, (int) did1, (int) idCairo, (int) idAlex,
                    Status.Accepted, 220.0, 180, 900.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, false);
            long req2 = reqDAO.insert(pid2, (int) did2, (int) idGiza, (int) idAlex,
                    Status.Completed, 240.0, 200, 950.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);
            System.out.println("\n[INSERT] Ride Requests:");
            reqDAO.showAll().forEach(System.out::println);

            // Update Request
            reqDAO.update(req1, (long) did2, Status.Completed, 220.0, 190, 880.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);
            System.out.println("\n[UPDATE] Ride Request updated:");
            reqDAO.showAll().forEach(System.out::println);

            // 6️⃣ RIDE HISTORY
            RideHistoryDAO histDAO = new RideHistoryDAO();
            histDAO.insert(req1, did1, pid1, 5, 4, 900.0, PaymentType.wallet, 20.0, 5.0, "UNICEF");
            histDAO.insert(req2, did2, pid2, 4, 5, 950.0, PaymentType.credit, 10.0, 0.0, "");
            System.out.println("\n[INSERT] Ride History:");
            histDAO.showAll().forEach(System.out::println);

            // Update RideHistory
            histDAO.update(1, 5, 5, 1000.0, PaymentType.wallet, 30.0, 10.0, "Red Crescent");
            System.out.println("\n[UPDATE] Ride History updated:");
            histDAO.showAll().forEach(System.out::println);

            // 7️⃣ PROBLEM REPORTS
            ProblemReportDAO reportDAO = new ProblemReportDAO();
            long rep1 = reportDAO.insertReport(req1, pid1, did1, "Driver was late and rude.");
            long rep2 = reportDAO.insertReport(req2, pid2, did2, "Vehicle not clean.");
            System.out.println("\n[INSERT] Problem Reports:");
            reportDAO.showAllReports().forEach(System.out::println);

            // Update Report
            reportDAO.updateReport(rep1, "Driver was late but apologized.", did1);
            System.out.println("\n[UPDATE] Problem Report updated:");
            reportDAO.showAllReports().forEach(System.out::println);

            // 8️⃣ PROBLEM REPORT TYPES (new DAO)
            ProblemReportTypeDAO prtDAO = new ProblemReportTypeDAO();
            prtDAO.insert(rep1, 1); // DRIVER_BEHAVIOR
            prtDAO.insert(rep1, 2); // DRIVER_LATE
            prtDAO.insert(rep2, 4); // VEHICLE_CLEANLINESS
            System.out.println("\n[INSERT] Problem Report Types:");
            prtDAO.showAll().forEach(System.out::println);

            // Delete one link
            prtDAO.delete(rep1, 2);
            System.out.println("\n[DELETE] One problem type link removed:");
            prtDAO.showAll().forEach(System.out::println);

            System.out.println("\n=== ALL TESTS COMPLETED SUCCESSFULLY ===");

        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ General Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

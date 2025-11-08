import DAO.*;
import Model.*;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TestDatabase {

    public static void main(String[] args) {
        try {
            System.out.println("=== DATABASE TEST START ===\n");

            resetDatabase();
            System.out.println("[RESET] All tables truncated.\n");

            ProblemReportTypeDAO prtDAO = new ProblemReportTypeDAO();
            prtDAO.initializeProblemTypes();
            System.out.println("[INIT] Problem types inserted.\n");

            LocationDAO locationDAO = new LocationDAO();
            Location cairo = new Location("Cairo");
            Location giza = new Location("Giza");
            Location alex = new Location("Alexandria");

            long idCairo = locationDAO.insert(cairo);
            long idGiza = locationDAO.insert(giza);
            long idAlex = locationDAO.insert(alex);

            locationDAO.update((int) idAlex, "Alex City");

            Location mansoura = new Location("Mansoura");
            Location tanta = new Location("Tanta");
            Location aswan = new Location("Aswan");
            Location luxor = new Location("Luxor");
            Location zagazig = new Location("Zagazig");
            Location ismailia = new Location("Ismailia");
            Location portSaid = new Location("Port Said");
            Location sohag = new Location("Sohag");
            Location beniSuef = new Location("Beni Suef");
            Location minya = new Location("Minya");

            long idMansoura = locationDAO.insert(mansoura);
            long idTanta = locationDAO.insert(tanta);
            long idAswan = locationDAO.insert(aswan);
            long idLuxor = locationDAO.insert(luxor);
            long idZagazig = locationDAO.insert(zagazig);
            long idIsmailia = locationDAO.insert(ismailia);
            long idPortSaid = locationDAO.insert(portSaid);
            long idBeniSuef = locationDAO.insert(beniSuef);

            System.out.println("[INSERT] Locations:");
            locationDAO.showAll().forEach(System.out::println);

            EdgeDAO edgeDAO = new EdgeDAO();
            edgeDAO.insert(cairo, giza, 20.5);
            edgeDAO.insert(giza, alex, 220.0);
            edgeDAO.insert(cairo, alex, 240.0);
            // Extra network
            edgeDAO.insert(cairo, mansoura, 130.0);
            edgeDAO.insert(mansoura, tanta, 40.0);
            edgeDAO.insert(aswan, luxor, 260.0);
            edgeDAO.insert(cairo, zagazig, 85.0);
            edgeDAO.insert(zagazig, ismailia, 90.0);
            edgeDAO.insert(ismailia, portSaid, 60.0);
            edgeDAO.insert(cairo, beniSuef, 125.0);
            edgeDAO.insert(beniSuef, minya, 140.0);
            edgeDAO.insert(minya, sohag, 190.0);
            edgeDAO.insert(sohag, luxor, 160.0);
            edgeDAO.insert(giza, beniSuef, 110.0);

            System.out.println("\n[INSERT] Edges:");
            edgeDAO.showAll().forEach(System.out::println);

            // 3️⃣ PASSENGERS — Base + Extra
            PassengerDAO passengerDAO = new PassengerDAO();
            Passenger p1 = new Passenger("PSSN001", "ahmed ashraf", "01000111222", "mohamed@mail.com", 500, 200, cairo, null);
            Passenger p2 = new Passenger("PSSN002", "mostafa hassan", "01055667788", "ahmed@mail.com", 1000, 500, giza, null);

            long pid1 = passengerDAO.insert(p1, (int) idCairo);
            long pid2 = passengerDAO.insert(p2, (int) idGiza);

            p2.setWalletBalance(1200);
            passengerDAO.update(pid2, p2, (int) idAlex, 5);

            Passenger p3 = new Passenger("PSSN003", "Abdo", "01011111111", "abdo@mail.com", 300, 120, cairo, null);
            Passenger p4 = new Passenger("PSSN004", "Amin", "01022222222", "amin@mail.com", 700, 300, giza, null);
            Passenger p5 = new Passenger("PSSN005", "Amr Nabil", "01033333333", "amr@mail.com", 900, 250, alex, null);
            Passenger p6 = new Passenger("PSSN006", "Azzay", "01044444444", "azzay@mail.com", 600, 200, mansoura, null);
            Passenger p7 = new Passenger("PSSN007", "Islam Ali", "01055555555", "islam@mail.com", 500, 180, aswan, null);
            Passenger p8 = new Passenger("PSSN008", "Marwan", "01066666666", "marwan@mail.com", 800, 260, luxor, null);
            Passenger p9 = new Passenger("PSSN009", "Shatoot", "01077777777", "shatoot@mail.com", 450, 160, tanta, null);

            long pid3 = passengerDAO.insert(p3, (int) idCairo);
            long pid4 = passengerDAO.insert(p4, (int) idGiza);
            long pid5 = passengerDAO.insert(p5, (int) idAlex);
            long pid6 = passengerDAO.insert(p6, (int) idMansoura);
            long pid7 = passengerDAO.insert(p7, (int) idAswan);
            long pid8 = passengerDAO.insert(p8, (int) idLuxor);
            long pid9 = passengerDAO.insert(p9, (int) idTanta);

            System.out.println("\n[INSERT] Passengers:");
            passengerDAO.showAll().forEach(System.out::println);

            // 4️⃣ DRIVERS — Base + Extra
            DriverDAO driverDAO = new DriverDAO();
            Driver d1 = new Driver("CAR111", "Toyota Corolla", true, "DSSN001", "Khaled Hassan", "01111111111", "khaled@mail.com", 800, 300, giza, null);
            Driver d2 = new Driver("CAR222", "Nissan Sunny", true, "DSSN002", "Omar Youssef", "01122222222", "omar@mail.com", 1000, 400, cairo, null);

            long did1 = driverDAO.insert(d1, (int) idGiza);
            long did2 = driverDAO.insert(d2, (int) idCairo);

            d1.updateWalletBalance(1200);
            driverDAO.update(did1, d1, (int) idAlex, 4);

            // Extra drivers
            Driver d3 = new Driver("CAR333", "Hyundai Elantra", true, "DSSN003", "Omar.Elemary_", "01133333333", "elemary@mail.com", 1200, 350, alex, null);
            Driver d4 = new Driver("CAR444", "Kia Cerato", true, "DSSN004", "AdminAhmed", "01144444444", "admin@mail.com", 900, 260, mansoura, null);
            Driver d5 = new Driver("CAR555", "Renault Logan", true, "DSSN005", "Mostafa", "01155555555", "mostafa@mail.com", 950, 280, luxor, null);
            Driver d6 = new Driver("CAR666", "Chevrolet Optra", true, "DSSN006", "Yassin", "01166666666", "yassin@mail.com", 1100, 320, beniSuef, null);

            long did3 = driverDAO.insert(d3, (int) idAlex);
            long did4 = driverDAO.insert(d4, (int) idMansoura);
            long did5 = driverDAO.insert(d5, (int) idLuxor);
            long did6 = driverDAO.insert(d6, (int) idBeniSuef);

            System.out.println("\n[INSERT] Drivers:");
            driverDAO.showAll().forEach(System.out::println);

            // 5️⃣ RIDE REQUESTS — Base + Extra (⚠️ use DB ids, not object.getId())
            RideRequestDAO reqDAO = new RideRequestDAO();

            long req1 = reqDAO.insert(pid1, (int) did1, (int) idCairo, (int) idAlex,
                    Status.Accepted, 220.0, 180, 900.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, false);

            long req2 = reqDAO.insert(pid2, (int) did2, (int) idGiza, (int) idAlex,
                    Status.Completed, 240.0, 200, 950.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);

            // Extra requests
            long req3 = reqDAO.insert(pid3, (int) did2, (int) idCairo, (int) idMansoura,
                    Status.Completed, 130.0, 120, 600.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, false);

            long req4 = reqDAO.insert(pid4, (int) did1, (int) idGiza, (int) idCairo,
                    Status.Accepted, 25.0, 40, 120.0,
                    Timestamp.valueOf(LocalDateTime.now()), false, false);

            long req5 = reqDAO.insert(pid5, (int) did3, (int) idAlex, (int) idLuxor,
                    Status.Pending, 620.0, 420, 2500.0,
                    Timestamp.valueOf(LocalDateTime.now()), false, false);

            long req6 = reqDAO.insert(pid7, (int) did5, (int) idAswan, (int) idLuxor,
                    Status.Completed, 260.0, 210, 1100.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);



            long req9 = reqDAO.insert(pid6, (int) did4, (int) idMansoura, (int) idTanta,
                    Status.Completed, 40.0, 35, 150.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);

            // Update an earlier one
            reqDAO.update(req1, (long) did2, Status.Completed, 220.0, 190, 880.0,
                    Timestamp.valueOf(LocalDateTime.now()), true, true);

            System.out.println("\n[INSERT] Ride Requests:");
            reqDAO.showAll().forEach(System.out::println);

            // 6️⃣ RIDE HISTORY (+ payment inside) — add many rows
            RideHistoryDAO histDAO = new RideHistoryDAO();

            histDAO.insert(req1, did1, pid1, 5, 4, 900.0, PaymentType.wallet, 20.0, 5.0, "UNICEF");
            histDAO.insert(req2, did2, pid2, 4, 5, 950.0, PaymentType.credit, 10.0, 0.0, "");

            histDAO.insert(req3, did2, pid3, 5, 5, 600.0, PaymentType.wallet, 0.0, 0.0, "");
            histDAO.insert(req4, did1, pid4, 3, 4, 120.0, PaymentType.credit, 0.0, 10.0, "");
            histDAO.insert(req6, did5, pid7, 5, 5, 1100.0, PaymentType.credit, 50.0, 0.0, "Resala");
            histDAO.insert(req9, did4, pid6, 5, 5, 150.0, PaymentType.wallet, 0.0, 0.0, "");
            // Simulate an adjustment on history #1
            histDAO.update(1, 5, 5, 1000.0, PaymentType.wallet, 30.0, 10.0, "Red Crescent");

            System.out.println("\n[INSERT] Ride History:");
            histDAO.showAll().forEach(System.out::println);

            // 7️⃣ PROBLEM REPORTS — several samples
            ProblemReportDAO reportDAO = new ProblemReportDAO();
            long rep1 = reportDAO.insertReport(req1, pid1, did1, "Driver was late and rude.");
            long rep2 = reportDAO.insertReport(req2, pid2, did2, "Vehicle not clean.");
            long rep3 = reportDAO.insertReport(req4, pid4, did1, "Wrong pickup location.");

            System.out.println("\n[INSERT] Problem Reports:");
            reportDAO.showAllReports().forEach(System.out::println);

            reportDAO.updateReport(rep1, "Driver was late but apologized.", did1);

            System.out.println("\n[UPDATE] Problem Report updated:");
            reportDAO.showAllReports().forEach(System.out::println);

            // 8️⃣ PROBLEM REPORT TYPES — link reports to multiple types
            // Assuming initializeProblemTypes() inserted type IDs like:
            // 1=Driver Behavior, 2=Vehicle Cleanliness, 3=Navigation, 4=Pricing, 5=Cancellation
            prtDAO.insert(rep1, 1); // behavior
            prtDAO.insert(rep1, 3); // navigation
            prtDAO.insert(rep2, 2); // cleanliness
            prtDAO.insert(rep3, 3); // navigation
            prtDAO.delete(rep1, 3); // remove one link as example

            System.out.println("\n[INSERT] Problem Report Types:");
            prtDAO.showAll().forEach(System.out::println);

            System.out.println("\n=== ALL TESTS COMPLETED SUCCESSFULLY ✅ ===");

        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
        }
    }

    // ⛔ Truncate all tables safely (FK off/on)
    private static void resetDatabase() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("TRUNCATE TABLE problem_report_types");
            stmt.execute("TRUNCATE TABLE problem_reports");
            stmt.execute("TRUNCATE TABLE ride_history");
            stmt.execute("TRUNCATE TABLE ride_requests");
            stmt.execute("TRUNCATE TABLE drivers");
            stmt.execute("TRUNCATE TABLE passengers");
            stmt.execute("TRUNCATE TABLE edges");
            stmt.execute("TRUNCATE TABLE locations");
            stmt.execute("TRUNCATE TABLE problem_types");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}

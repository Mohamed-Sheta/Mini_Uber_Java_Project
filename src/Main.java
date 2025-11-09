import Model.*;
import services.*;
import DAO.*;

import java.sql.Timestamp;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Mini Uber System Egypt (DB Enabled Version) ===\n");
        final boolean RESET_DB = true;
        // ===================== DAO OBJECTS =====================
        LocationDAO locationDAO = new LocationDAO();
        EdgeDAO edgeDAO = new EdgeDAO();
        DriverDAO driverDAO = new DriverDAO();
        PassengerDAO passengerDAO = new PassengerDAO();
        RideRequestDAO rideRequestDAO = new RideRequestDAO();
        RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();
        ProblemReportDAO problemReportDAO = new ProblemReportDAO();
        ProblemReportTypeDAO problemReportTypeDAO = new ProblemReportTypeDAO();
        // ===================== OPTIONAL: RESET DB =====================
        if (RESET_DB) {
            try {
                // Foreign keys off
                java.sql.Connection con = utils.DBConnection.getConnection();
                try (var ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS=0")) { ps.execute(); }

                String[] tables = {
                        "problem_report_types", "problem_reports",
                        "ride_history", "ride_requests",
                        "edges", "locations",
                        "drivers", "passengers",
                        "problem_types"
                };
                for (String t : tables) {
                    try (var ps = con.prepareStatement("TRUNCATE TABLE " + t)) { ps.execute(); }
                }
                try (var ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS=1")) { ps.execute(); }
                System.out.println("[DB] All tables truncated.\n");
            } catch (Exception e) {
                System.out.println("[DB] Reset error: " + e.getMessage());
            }
        }

        // ===================== SEED PROBLEM TYPES =====================
        try {
            problemReportTypeDAO.initializeProblemTypes();
            System.out.println("[DB] problem_types seeded.\n");
        } catch (Exception e) {
            System.out.println("[DB] Seed problem_types error: " + e.getMessage());
        }
        // ثابت يربط enum -> id (لازم يبقى مطابق للسييد)
        final Map<ProblemType, Integer> PROBLEM_TYPE_ID = new HashMap<>();
        PROBLEM_TYPE_ID.put(ProblemType.DRIVER_BEHAVIOR, 1);
        PROBLEM_TYPE_ID.put(ProblemType.DRIVER_LATE, 2);
        PROBLEM_TYPE_ID.put(ProblemType.RECKLESS_DRIVING, 3);
        PROBLEM_TYPE_ID.put(ProblemType.VEHICLE_CLEANLINESS, 4);
        PROBLEM_TYPE_ID.put(ProblemType.TECHNICAL_ISSUE, 5);
        PROBLEM_TYPE_ID.put(ProblemType.FARE_DISPUTE, 6);
        PROBLEM_TYPE_ID.put(ProblemType.ACCOUNT_ISSUE, 7);

        // ===================== LOCATIONS =====================
        Location downtown = new Location("Downtown Cairo");
        Location nasrCity = new Location("Nasr City");
        Location maadi = new Location("Maadi");
        Location giza = new Location("Giza");
        Location newCairo = new Location("New Cairo");

        MapGraph cityMap = new MapGraph();
        List<Location> places = Arrays.asList(downtown, nasrCity, maadi, giza, newCairo);
        for (Location l : places) cityMap.addLocation(l);

        // Insert locations (وخزّن الـ id في الـ object عشان edges تستخدمه)
        try {
            for (Location l : places) {
                long id = locationDAO.insert(l);
                // موديلاك فيه setId — لو مش موجود عندك شيل السطرين دول وخزّن ids في Map<Location, Integer>
                l.setId((int) id);
                System.out.println("[DB] Insert Location: " + l.getName() + " -> id=" + id);
            }
        } catch (Exception e) {
            System.out.println("[DB] Insert locations error: " + e.getMessage());
        }

        // ===================== EDGES (Model + DB) =====================
        cityMap.addEdge(downtown, nasrCity, 6.0);
        cityMap.addEdge(nasrCity, downtown, 6.0);
        cityMap.addEdge(downtown, maadi, 8.0);
        cityMap.addEdge(maadi, downtown, 8.0);
        cityMap.addEdge(maadi, giza, 5.0);
        cityMap.addEdge(giza, maadi, 5.0);
        cityMap.addEdge(nasrCity, newCairo, 10.0);
        cityMap.addEdge(newCairo, nasrCity, 10.0);

        try {
            edgeDAO.insert(downtown, nasrCity, 6.0);
            edgeDAO.insert(nasrCity, downtown, 6.0);
            edgeDAO.insert(downtown, maadi, 8.0);
            edgeDAO.insert(maadi, downtown, 8.0);
            edgeDAO.insert(maadi, giza, 5.0);
            edgeDAO.insert(giza, maadi, 5.0);
            edgeDAO.insert(nasrCity, newCairo, 10.0);
            edgeDAO.insert(newCairo, nasrCity, 10.0);
            System.out.println("[DB] Edges inserted.\n");
        } catch (Exception e) {
            System.out.println("[DB] Insert edges error: " + e.getMessage());
        }

        System.out.println("MapGraph + DB setup done.\n");

        // ===================== DRIVERS (Model + DB) =====================
        List<RideHistory> emptyHistory = new ArrayList<>();
        Driver d1 = new Driver("CAR001", "Toyota Corolla", true, "SSN100", "marwan wael", "01010001000", "marwan@gmail.com", 100.0, 50.0, downtown, emptyHistory);
        Driver d2 = new Driver("CAR002", "Hyundai Verna", true, "SSN101", "c ali", "01010001001", "islam@gmail.com", 120.0, 60.0, nasrCity, emptyHistory);
        Driver d3 = new Driver("CAR003", "Kia Cerato", false, "SSN102", "amin ahmed", "01010001002", "amin@gmail.com", 90.0, 45.0, giza, emptyHistory);
        Driver d4 = new Driver("CAR004", "Nissan Sunny", true, "SSN103", "Youssef Ibrahim", "01010001003", "youssef@gmail.com", 150.0, 75.0, maadi, emptyHistory);
        List<Driver> allDrivers = Arrays.asList(d1, d2, d3, d4);

        Map<Driver, Long> driverId = new HashMap<>();
        try {
            for (Driver d : allDrivers) {
                String locName = d.getCurrentLocation() != null ? d.getCurrentLocation().getName() : null;
                long id = driverDAO.insert(d, locName);
                driverId.put(d, id);
                System.out.println("[DB] Insert Driver: " + d.getName() + " -> id=" + id);
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("[DB] Insert drivers error: " + e.getMessage());
        }

        // ===================== PASSENGERS (Model + DB) =====================
        Passenger p1 = new Passenger("PSSN01", "ahmed ashraf", "01110001001", "ahmed@gmail.com", 200.0, 100.0, maadi, new ArrayList<>());
        Passenger p2 = new Passenger("PSSN02", "mohamed sheta", "01110001002", "sheta@gmail.com", 40.0, 10.0, downtown, new ArrayList<>());
        Passenger p3 = new Passenger("PSSN03", "mostafa hassan", "01110001003", "mostafa@gmail.com", 500.0, 250.0, nasrCity, new ArrayList<>());
        Passenger p4 = new Passenger("PSSN04", "amr nabli", "01110001004", "amr@gmail.com", 15.0, 0.0, newCairo, new ArrayList<>());

        Map<Passenger, Long> passengerId = new HashMap<>();
        try {
            for (Passenger p : Arrays.asList(p1, p2, p3, p4)) {
                String locName = p.getCurrentLocation() != null ? p.getCurrentLocation().getName() : null;
                long id = passengerDAO.insert(p, locName);
                passengerId.put(p, id);
                System.out.println("[DB] Insert Passenger: " + p.getName() + " -> id=" + id);
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("[DB] Insert passengers error: " + e.getMessage());
        }

        // ===================== PAYMENT OPTIONS =====================
        Option optTipsDonate = new Option();
        optTipsDonate.enableTips(true);
        optTipsDonate.giveTips(10.0);
        optTipsDonate.enableDonation(true);
        optTipsDonate.giveDonation(5.0, "Charity Egypt");

        Option optBasic = new Option();
        optBasic.enableTips(false);
        optBasic.enableDonation(false);

        System.out.println("Payment options ready.\n");

        // ========== HELPER: دالة صغيرة تطبع سطر فاصل ==========
        Runnable sep = () -> System.out.println("\n----------------------------------------\n");

        // =======================================================
        // ********************** TEST 1 *************************
        // =======================================================
        System.out.println("Test 1: Normal ride (Ahmed from Maadi -> Giza)");
        Request r1 = p1.request_ride(maadi, giza, cityMap);
        if (r1 != null) {
            long rrId = -1;
            try {
                rrId = rideRequestDAO.insert(
                        passengerId.get(p1), null,
                        r1.getOrigin().getId(), r1.getDestination().getId(),
                        Status.Pending,
                        r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                        null, false, false
                );
                System.out.println("[DB] ride_requests inserted (Pending) id=" + rrId);
            } catch (Exception e) {
                System.out.println("[DB] Insert ride_request error: " + e.getMessage());
            }

            Payment pay1 = new Payment(r1.getEstimatedPrice(), PaymentType.wallet, optTipsDonate);
            RideManager rm1 = new RideManager(allDrivers, r1, cityMap, pay1);
            rm1.createRide();

            Driver assigned1 = rm1.getCurrentDriver();
            if (assigned1 == null) {
                // no driver → cancel
                try {
                    rideRequestDAO.update(rrId, null, Status.Cancelled,
                            r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                            null, false, false);
                    System.out.println("[DB] ride_request " + rrId + " -> Cancelled (no driver)");
                } catch (Exception e) {
                    System.out.println("[DB] Update cancel error: " + e.getMessage());
                }
            } else {
                // Accept
                try {
                    rideRequestDAO.update(
                            rrId, driverId.get(assigned1), Status.Accepted,
                            r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                            Timestamp.valueOf(rm1.getAcceptanceTime()), false, false
                    );
                    System.out.println("[DB] ride_request " + rrId + " -> Accepted (driver=" + driverId.get(assigned1) + ")");
                } catch (Exception e) {
                    System.out.println("[DB] Update accept error: " + e.getMessage());
                }

                // Driver arrived
                rm1.markDriverArrived();
                try {
                    rideRequestDAO.update(
                            rrId, driverId.get(assigned1), Status.Accepted,
                            r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                            Timestamp.valueOf(rm1.getAcceptanceTime()), true, false
                    );
                } catch (Exception e) { System.out.println("[DB] Update driver_arrived error: " + e.getMessage()); }

                // Passenger arrived
                rm1.markPassengerArrived();
                try {
                    rideRequestDAO.update(
                            rrId, driverId.get(assigned1), Status.Accepted,
                            r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                            Timestamp.valueOf(rm1.getAcceptanceTime()), true, true
                    );
                } catch (Exception e) { System.out.println("[DB] Update passenger_arrived error: " + e.getMessage()); }

                // Complete + history
                rm1.setPassengerWantsToRate(true);
                rm1.setPassengerRatingValue(5);
                rm1.setDriverWantsToRate(true);
                rm1.setDriverRatingValue(5);
                rm1.completeRide();

                try {
                    rideRequestDAO.update(
                            rrId, driverId.get(assigned1), Status.Completed,
                            r1.getDistance(), r1.getEstimatedTime(), r1.getEstimatedPrice(),
                            Timestamp.valueOf(rm1.getAcceptanceTime()), true, true
                    );
                    System.out.println("[DB] ride_request " + rrId + " -> Completed");
                } catch (Exception e) {
                    System.out.println("[DB] Update completed error: " + e.getMessage());
                }

                try {
                    long rhId = rideHistoryDAO.insert(
                            rrId, driverId.get(assigned1), passengerId.get(p1),
                            p1.getLatestDriverRating(), assigned1.getLatestPassengerRating(),
                            pay1.getAmount(), pay1.getPaymentMethod(),
                            optTipsDonate.getTips(), optTipsDonate.getDonationAmount(), optTipsDonate.getDonationOrganization()
                    );
                    System.out.println("[DB] ride_history inserted id=" + rhId);
                } catch (Exception e) {
                    System.out.println("[DB] Insert ride_history error: " + e.getMessage());
                }

                // Problem report example
                try {
                    long repId = problemReportDAO.insertReport(rrId, passengerId.get(p1), driverId.get(assigned1));
                    problemReportTypeDAO.insert(repId, PROBLEM_TYPE_ID.get(ProblemType.DRIVER_BEHAVIOR), "Driver was late 5 minutes.");
                    System.out.println("[DB] problem_reports inserted id=" + repId);
                } catch (Exception e) {
                    System.out.println("[DB] Insert problem_report error: " + e.getMessage());
                }
            }
        }
        sep.run();

        // =======================================================
        // ********************** TEST 2 *************************
        // =======================================================
        System.out.println("Test 2: Low balance (Sara from Downtown -> Nasr City)");
        Request r2 = p2.request_ride(downtown, nasrCity, cityMap);
        if (r2 != null) {
            long rrId = -1;
            try {
                rrId = rideRequestDAO.insert(
                        passengerId.get(p2), null,
                        r2.getOrigin().getId(), r2.getDestination().getId(),
                        Status.Pending,
                        r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                        null, false, false
                );
                System.out.println("[DB] ride_requests inserted (Pending) id=" + rrId);
            } catch (Exception e) {
                System.out.println("[DB] Insert ride_request error: " + e.getMessage());
            }

            Payment pay2 = new Payment(r2.getEstimatedPrice(), PaymentType.credit, optBasic);
            RideManager rm2 = new RideManager(allDrivers, r2, cityMap, pay2);
            rm2.createRide();
            Driver assigned2 = rm2.getCurrentDriver();

            if (assigned2 == null) {
                try {
                    rideRequestDAO.update(rrId, null, Status.Cancelled,
                            r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                            null, false, false);
                    System.out.println("[DB] ride_request " + rrId + " -> Cancelled (no driver)");
                } catch (Exception e) {
                    System.out.println("[DB] Update cancel error: " + e.getMessage());
                }
            } else {
                try {
                    rideRequestDAO.update(rrId, driverId.get(assigned2), Status.Accepted,
                            r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                            Timestamp.valueOf(rm2.getAcceptanceTime()), false, false);
                } catch (Exception e) { System.out.println("[DB] Accept update error: " + e.getMessage()); }

                rm2.markDriverArrived();
                try {
                    rideRequestDAO.update(rrId, driverId.get(assigned2), Status.Accepted,
                            r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                            Timestamp.valueOf(rm2.getAcceptanceTime()), true, false);
                } catch (Exception e) { System.out.println("[DB] driver_arrived update error: " + e.getMessage()); }

                rm2.markPassengerArrived();
                try {
                    rideRequestDAO.update(rrId, driverId.get(assigned2), Status.Accepted,
                            r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                            Timestamp.valueOf(rm2.getAcceptanceTime()), true, true);
                } catch (Exception e) { System.out.println("[DB] passenger_arrived update error: " + e.getMessage()); }

                rm2.setPassengerWantsToRate(true);
                rm2.setPassengerRatingValue(4);
                rm2.setDriverWantsToRate(true);
                rm2.setDriverRatingValue(5);
                rm2.completeRide();

                try {
                    rideRequestDAO.update(rrId, driverId.get(assigned2), Status.Completed,
                            r2.getDistance(), r2.getEstimatedTime(), r2.getEstimatedPrice(),
                            Timestamp.valueOf(rm2.getAcceptanceTime()), true, true);
                } catch (Exception e) { System.out.println("[DB] completed update error: " + e.getMessage()); }

                try {
                    rideHistoryDAO.insert(
                            rrId, driverId.get(assigned2), passengerId.get(p2),
                            p2.getLatestDriverRating(), assigned2.getLatestPassengerRating(),
                            pay2.getAmount(), pay2.getPaymentMethod(),
                            0.0, 0.0, ""
                    );
                } catch (Exception e) { System.out.println("[DB] insert ride_history error: " + e.getMessage()); }

                try {
                    long repId = problemReportDAO.insertReport(rrId, passengerId.get(p2), driverId.get(assigned2));
                    problemReportTypeDAO.insert(repId, PROBLEM_TYPE_ID.get(ProblemType.FARE_DISPUTE), "Fare seems higher than expected.");
                } catch (Exception e) { System.out.println("[DB] report insert error: " + e.getMessage()); }
            }
        }
        sep.run();

        // ********************** TEST 3 *************************
        System.out.println("Test 3: No path (Mona from New Cairo -> Maadi)");
        Request r3 = p4.request_ride(newCairo, maadi, cityMap);
        if (r3 == null) {
            System.out.println("No available path between New Cairo and Maadi (expected).\n");
        }
        sep.run();
        // ********************** TEST 4 *************************
        System.out.println("Test 4: No drivers available (simulate)");
        Request r4 = p3.request_ride(nasrCity, downtown, cityMap);
        if (r4 != null) {
            long rrId = -1;
            try {
                rrId = rideRequestDAO.insert(
                        passengerId.get(p3), null,
                        r4.getOrigin().getId(), r4.getDestination().getId(),
                        Status.Pending,
                        r4.getDistance(), r4.getEstimatedTime(), r4.getEstimatedPrice(),
                        null, false, false
                );
            } catch (Exception e) { System.out.println("[DB] insert rr error: " + e.getMessage()); }

            RideManager rm4 = new RideManager(new ArrayList<>(), r4, cityMap, new Payment(r4.getEstimatedPrice(), PaymentType.wallet, optBasic));
            rm4.createRide();
            try {
                rideRequestDAO.update(rrId, null, Status.Cancelled,
                        r4.getDistance(), r4.getEstimatedTime(), r4.getEstimatedPrice(),
                        null, false, false);
                System.out.println("[DB] ride_request " + rrId + " -> Cancelled (no drivers)");
            } catch (Exception e) { System.out.println("[DB] update rr cancel error: " + e.getMessage()); }
        }
        sep.run();
        // ********************** TEST 5 *************************
        System.out.println("Test 5: Driver views and accepts pending ride requests");
        Queue<Request> rideQueue = new LinkedList<>();
        Request rq1 = p1.request_ride(maadi, downtown, cityMap);
        Request rq2 = p2.request_ride(downtown, giza, cityMap);
        Request rq3 = p3.request_ride(nasrCity, newCairo, cityMap);

        if (rq1 != null) rideQueue.add(rq1);
        if (rq2 != null) rideQueue.add(rq2);
        if (rq3 != null) rideQueue.add(rq3);

        d4.viewRideRequests(rideQueue);
        d4.Accept_Request(rideQueue);
        System.out.println("\nRemaining Requests After Acceptance:");
        d4.viewRideRequests(rideQueue);
        sep.run();
        // ********************** TEST 6 *************************
        System.out.println("Test 6: Small stress test (10 random rides)");
        List<Passenger> passengers = Arrays.asList(p1, p2, p3, p4);
        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            Passenger px = passengers.get(rand.nextInt(passengers.size()));
            Location start = places.get(rand.nextInt(places.size()));
            Location end = places.get(rand.nextInt(places.size()));
            while (end == start) end = places.get(rand.nextInt(places.size()));

            Request rx = px.request_ride(start, end, cityMap);
            if (rx == null) continue;

            Payment pxPay = new Payment(
                    rx.getEstimatedPrice(),
                    (i % 2 == 0) ? PaymentType.wallet : PaymentType.credit,
                    (i % 3 == 0) ? optTipsDonate : optBasic
            );

            long rrId = -1;
            try {
                rrId = rideRequestDAO.insert(
                        passengerId.get(px), null,
                        rx.getOrigin().getId(), rx.getDestination().getId(),
                        Status.Pending,
                        rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                        null, false, false
                );
            } catch (Exception e) { System.out.println("[DB] insert rr error: " + e.getMessage()); }

            RideManager rmx = new RideManager(allDrivers, rx, cityMap, pxPay);
            rmx.createRide();
            Driver assigned = rmx.getCurrentDriver();
            if (assigned == null) {
                try {
                    rideRequestDAO.update(rrId, null, Status.Cancelled,
                            rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                            null, false, false);
                } catch (Exception e) { System.out.println("[DB] update cancel error: " + e.getMessage()); }
                continue;
            }

            try {
                rideRequestDAO.update(rrId, driverId.get(assigned), Status.Accepted,
                        rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                        Timestamp.valueOf(rmx.getAcceptanceTime()), false, false);
            } catch (Exception e) { System.out.println("[DB] update accept error: " + e.getMessage()); }

            rmx.markDriverArrived();
            try {
                rideRequestDAO.update(rrId, driverId.get(assigned), Status.Accepted,
                        rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                        Timestamp.valueOf(rmx.getAcceptanceTime()), true, false);
            } catch (Exception e) { System.out.println("[DB] update driver_arrived error: " + e.getMessage()); }

            rmx.markPassengerArrived();
            try {
                rideRequestDAO.update(rrId, driverId.get(assigned), Status.Accepted,
                        rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                        Timestamp.valueOf(rmx.getAcceptanceTime()), true, true);
            } catch (Exception e) { System.out.println("[DB] update passenger_arrived error: " + e.getMessage()); }

            rmx.setPassengerWantsToRate(true);
            rmx.setPassengerRatingValue(5);
            rmx.setDriverWantsToRate(true);
            rmx.setDriverRatingValue(5);
            rmx.completeRide();

            try {
                rideRequestDAO.update(rrId, driverId.get(assigned), Status.Completed,
                        rx.getDistance(), rx.getEstimatedTime(), rx.getEstimatedPrice(),
                        Timestamp.valueOf(rmx.getAcceptanceTime()), true, true);
            } catch (Exception e) { System.out.println("[DB] update completed error: " + e.getMessage()); }

            try {
                rideHistoryDAO.insert(
                        rrId, driverId.get(assigned), passengerId.get(px),
                        px.getLatestDriverRating(), assigned.getLatestPassengerRating(),
                        pxPay.getAmount(), pxPay.getPaymentMethod(),
                        (pxPay.getOptions() != null ? pxPay.getOptions().getTips() : 0.0),
                        (pxPay.getOptions() != null ? pxPay.getOptions().getDonationAmount() : 0.0),
                        (pxPay.getOptions() != null ? pxPay.getOptions().getDonationOrganization() : "")
                );
            } catch (Exception e) { System.out.println("[DB] insert ride_history error: " + e.getMessage()); }
        }
        sep.run();
        // ********************** TEST 7 *************************
        System.out.println("Test 7: Passenger cancels a ride");
        Request cancelReq = p1.request_ride(maadi, giza, cityMap);
        if (cancelReq != null) {
            Payment cancelPay = new Payment(cancelReq.getEstimatedPrice(), PaymentType.wallet, optBasic);

            long rrId = -1;
            try {
                rrId = rideRequestDAO.insert(
                        passengerId.get(p1), null,
                        cancelReq.getOrigin().getId(), cancelReq.getDestination().getId(),
                        Status.Pending,
                        cancelReq.getDistance(), cancelReq.getEstimatedTime(), cancelReq.getEstimatedPrice(),
                        null, false, false
                );
            } catch (Exception e) { System.out.println("[DB] insert rr error: " + e.getMessage()); }

            RideManager cancelManager = new RideManager(allDrivers, cancelReq, cityMap, cancelPay);
            cancelManager.createRide();
            Driver assigned = cancelManager.getCurrentDriver();

            if (assigned != null) {
                try {
                    rideRequestDAO.update(rrId, driverId.get(assigned), Status.Accepted,
                            cancelReq.getDistance(), cancelReq.getEstimatedTime(), cancelReq.getEstimatedPrice(),
                            Timestamp.valueOf(cancelManager.getAcceptanceTime()), false, false);
                } catch (Exception e) { System.out.println("[DB] accept update error: " + e.getMessage()); }
            }

            System.out.println("\n>>> Passenger decides to cancel the ride...");
            p1.cancelRide(cancelManager);
            try {
                rideRequestDAO.update(
                        rrId,
                        (assigned != null ? driverId.get(assigned) : null),
                        Status.Cancelled,
                        cancelReq.getDistance(), cancelReq.getEstimatedTime(), cancelReq.getEstimatedPrice(),
                        null,
                        cancelManager.driverArrivedToPassenger,
                        false
                );
                System.out.println("[DB] ride_request " + rrId + " -> Cancelled (by passenger)");
            } catch (Exception e) {
                System.out.println("[DB] cancel update error: " + e.getMessage());
            }

            System.out.println("\nAfter cancellation:");
            System.out.println("Passenger Wallet: " + p1.getWalletBalance() + " EGP");
            System.out.println("Driver Wallet: " + d1.getWalletBalance() + " EGP");
        }
        sep.run();
        // ********************** TEST 8 *************************
        System.out.println("Test 8: Count Completed Rides");
        List<RideHistory> allHistories = new ArrayList<>();
        allHistories.addAll(p1.getRideHistory());
        allHistories.addAll(p2.getRideHistory());
        allHistories.addAll(p3.getRideHistory());
        allHistories.addAll(p4.getRideHistory());
        int completedCount = RideHistory.getRideCounts(allHistories);
        System.out.println(" Total completed rides in the system: " + completedCount);

        System.out.println("\n=== ALL DONE ===");
    }
}

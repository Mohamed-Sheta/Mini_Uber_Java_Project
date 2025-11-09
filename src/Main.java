import Model.*;
import services.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Mini Uber System Egypt (Refactored Version) ===\n");

        // ===================== STEP 1: Initialize Database =====================
        Request.DatabaseInitializer dbInit = new Request.DatabaseInitializer();
        Map<ProblemType, Integer> problemTypeMap = dbInit.initialize(true); // true = reset DB

        // ===================== STEP 2: Setup City Map and Data =====================
        MapGraph.CityMapSetup citySetup = new MapGraph.CityMapSetup();
        citySetup.initializeAll();

        // Extract initialized data
        MapGraph cityMap = citySetup.cityMap;
        List<Location> places = citySetup.locations;
        List<Driver> allDrivers = citySetup.drivers;
        List<Passenger> passengers = citySetup.passengers;
        Map<Driver, Long> driverId = citySetup.driverIdMap;
        Map<Passenger, Long> passengerId = citySetup.passengerIdMap;

        // Quick references
        Location downtown = places.get(0);
        Location nasrCity = places.get(1);
        Location maadi = places.get(2);
        Location giza = places.get(3);
        Location newCairo = places.get(4);

        Passenger p1 = passengers.get(0);
        Passenger p2 = passengers.get(1);
        Passenger p3 = passengers.get(2);
        Passenger p4 = passengers.get(3);

        Driver d4 = allDrivers.get(3);

        // ===================== STEP 3: Payment Options =====================
        Option optTipsDonate = new Option();
        optTipsDonate.enableTips(true);
        optTipsDonate.giveTips(10.0);
        optTipsDonate.enableDonation(true);
        optTipsDonate.giveDonation(5.0, "Charity Egypt");

        Option optBasic = new Option();
        optBasic.enableTips(false);
        optBasic.enableDonation(false);

        System.out.println("Payment options ready.\n");
        Runnable sep = () -> System.out.println("\n----------------------------------------\n");

        // =======================================================
        // ********************** TEST 1 *************************
        // =======================================================
        System.out.println("Test 1: Normal ride (Ahmed from Maadi -> Giza)");
        Request r1 = p1.request_ride(maadi, giza, cityMap);
        if (r1 != null) {
            Payment pay1 = new Payment(r1.getEstimatedPrice(), PaymentType.wallet, optTipsDonate);
            RideManager rm1 = new RideManager(allDrivers, r1, cityMap, pay1);
            rm1.setDatabaseMaps(passengerId, driverId);

            rm1.createRide();
            if (rm1.getCurrentDriver() != null) {
                rm1.markDriverArrived();
                rm1.markPassengerArrived();
                rm1.setPassengerWantsToRate(true);
                rm1.setPassengerRatingValue(5);
                rm1.setDriverWantsToRate(true);
                rm1.setDriverRatingValue(5);
                rm1.completeRide();

                // Submit problem report
                Request.submitProblemReport(rm1.getRideRequestId(), passengerId.get(p1),
                        driverId.get(rm1.getCurrentDriver()),
                        ProblemType.DRIVER_BEHAVIOR, "Driver was late 5 minutes.",
                        problemTypeMap);
            }
        }
        sep.run();

        // =======================================================
        // ********************** TEST 2 *************************
        // =======================================================
        System.out.println("Test 2: Low balance (Sara from Downtown -> Nasr City)");
        Request r2 = p2.request_ride(downtown, nasrCity, cityMap);
        if (r2 != null) {
            Payment pay2 = new Payment(r2.getEstimatedPrice(), PaymentType.credit, optBasic);
            RideManager rm2 = new RideManager(allDrivers, r2, cityMap, pay2);
            rm2.setDatabaseMaps(passengerId, driverId);

            rm2.createRide();
            if (rm2.getCurrentDriver() != null) {
                rm2.markDriverArrived();
                rm2.markPassengerArrived();
                rm2.setPassengerWantsToRate(true);
                rm2.setPassengerRatingValue(4);
                rm2.setDriverWantsToRate(true);
                rm2.setDriverRatingValue(5);
                rm2.completeRide();

                Request.submitProblemReport(rm2.getRideRequestId(), passengerId.get(p2),
                        driverId.get(rm2.getCurrentDriver()),
                        ProblemType.FARE_DISPUTE, "Fare seems higher than expected.",
                        problemTypeMap);
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
            Payment pay4 = new Payment(r4.getEstimatedPrice(), PaymentType.wallet, optBasic);
            RideManager rm4 = new RideManager(new ArrayList<>(), r4, cityMap, pay4);
            rm4.setDatabaseMaps(passengerId, driverId);
            rm4.createRide(); // Will automatically cancel if no drivers
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

            RideManager rmx = new RideManager(allDrivers, rx, cityMap, pxPay);
            rmx.setDatabaseMaps(passengerId, driverId);
            rmx.createRide();

            if (rmx.getCurrentDriver() != null) {
                rmx.markDriverArrived();
                rmx.markPassengerArrived();
                rmx.setPassengerWantsToRate(true);
                rmx.setPassengerRatingValue(5);
                rmx.setDriverWantsToRate(true);
                rmx.setDriverRatingValue(5);
                rmx.completeRide();
            }
        }
        sep.run();

        // ********************** TEST 7 *************************
        System.out.println("Test 7: Passenger cancels a ride");
        Request cancelReq = p1.request_ride(maadi, giza, cityMap);
        if (cancelReq != null) {
            Payment cancelPay = new Payment(cancelReq.getEstimatedPrice(), PaymentType.wallet, optBasic);
            RideManager cancelManager = new RideManager(allDrivers, cancelReq, cityMap, cancelPay);
            cancelManager.setDatabaseMaps(passengerId, driverId);
            cancelManager.createRide();

            System.out.println("\n>>> Passenger decides to cancel the ride...");
            p1.cancelRide(cancelManager);

            System.out.println("\nAfter cancellation:");
            System.out.println("Passenger Wallet: " + p1.getWalletBalance() + " EGP");
            if (allDrivers.size() > 0) {
                System.out.println("Driver Wallet: " + allDrivers.get(0).getWalletBalance() + " EGP");
            }
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


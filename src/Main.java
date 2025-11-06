import Model.*;
import services.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("=== Mini Uber System Egypt (Short English Version) ===\n");

        // --------------------- Locations ---------------------
        Location downtown = new Location("Downtown Cairo");
        Location nasrCity = new Location("Nasr City");
        Location maadi = new Location("Maadi");
        Location giza = new Location("Giza");
        Location newCairo = new Location("New Cairo");

        MapGraph cityMap = new MapGraph();
        List<Location> places = Arrays.asList(downtown, nasrCity, maadi, giza, newCairo);
        for (Location l : places) cityMap.addLocation(l);

        // Connect some locations
        cityMap.addEdge(downtown, nasrCity, 6.0);
        cityMap.addEdge(nasrCity, downtown, 6.0);
        cityMap.addEdge(downtown, maadi, 8.0);
        cityMap.addEdge(maadi, downtown, 8.0);
        cityMap.addEdge(maadi, giza, 5.0);
        cityMap.addEdge(giza, maadi, 5.0);
        cityMap.addEdge(nasrCity, newCairo, 10.0);
        cityMap.addEdge(newCairo, nasrCity, 10.0);
        // note: newCairo not directly connected to maadi → will test no path case

        System.out.println("MapGraph setup completed.\n");

        // --------------------- Drivers ---------------------
        List<RideHistory> emptyHistory = new ArrayList<>();
        Driver d1 = new Driver("CAR001", "Toyota Corolla", true, "SSN100", "marwan wael", "01010001000", "marwan@gmail.com", 100.0, 50.0, downtown, emptyHistory);
        Driver d2 = new Driver("CAR002", "Hyundai Verna", true, "SSN101", "c ali", "01010001001", "islam@gmail.com", 120.0, 60.0, nasrCity, emptyHistory);
        Driver d3 = new Driver("CAR003", "Kia Cerato", false, "SSN102", "amin ahmed", "01010001002", "amin@gmail.com", 90.0, 45.0, giza, emptyHistory);
        Driver d4 = new Driver("CAR004", "Nissan Sunny", true, "SSN103", "Youssef Ibrahim", "01010001003", "youssef@gmail.com", 150.0, 75.0, maadi, emptyHistory);

        List<Driver> allDrivers = Arrays.asList(d1, d2, d3, d4);
        System.out.println("Drivers created.\n");

        // --------------------- Passengers ---------------------
        Passenger p1 = new Passenger("PSSN01", "ahmed ashraf", "01110001001", "ahmed@gmail.com", 200.0, 100.0, maadi, new ArrayList<>());
        Passenger p2 = new Passenger("PSSN02", "mohamed sheta", "01110001002", "sheta@gmail.com", 40.0, 10.0, downtown, new ArrayList<>());
        Passenger p3 = new Passenger("PSSN03", "mostafa hassan", "01110001003", "mostafa@gmail.com", 500.0, 250.0, nasrCity, new ArrayList<>());
        Passenger p4 = new Passenger("PSSN04", "amr nabli", "01110001004", "amr@gmail.com", 15.0, 0.0, newCairo, new ArrayList<>());

        System.out.println("Passengers created.\n");

        // --------------------- Payment Options ---------------------
        Option optTipsDonate = new Option();
        optTipsDonate.enableTips(true);
        optTipsDonate.giveTips(10.0);
        optTipsDonate.enableDonation(true);
        optTipsDonate.giveDonation(5.0, "Charity Egypt");

        Option optBasic = new Option();
        optBasic.enableTips(false);
        optBasic.enableDonation(false);

        System.out.println("Payment options ready.\n");

        // --------------------- 1) Normal Ride ---------------------
        System.out.println("Test 1: Normal ride (Karim from Maadi -> Giza)");
        Request r1 = p1.request_ride(maadi, giza, cityMap);
        if (r1 != null) {
            Payment pay1 = new Payment(r1.getEstimatedPrice(), PaymentType.wallet, optTipsDonate);
            RideManager rm1 = new RideManager(allDrivers, r1, cityMap, pay1);
            rm1.createRide();
            rm1.markDriverArrived();
            rm1.markPassengerArrived();
            rm1.completeRide();

            Set<ProblemType> probs = new HashSet<>(Arrays.asList(ProblemType.DRIVER_BEHAVIOR));
            p1.ReportProblem(rm1, probs, "Driver was late 5 minutes.");
        }
        System.out.println();

        // --------------------- 2) Low Balance Ride ---------------------
        System.out.println("Test 2: Low balance (Sara from Downtown -> Nasr City)");
        Request r2 = p2.request_ride(downtown, nasrCity, cityMap);
        if (r2 != null) {
            Payment pay2 = new Payment(r2.getEstimatedPrice(), PaymentType.credit, optBasic);
            RideManager rm2 = new RideManager(allDrivers, r2, cityMap, pay2);
            rm2.createRide();
            rm2.markDriverArrived();
            rm2.markPassengerArrived();
            rm2.completeRide();

            Set<ProblemType> probs2 = new HashSet<>(Arrays.asList(ProblemType.FARE_DISPUTE));
            p2.ReportProblem(rm2, probs2, "Fare seems higher than expected.");
        }
        System.out.println();

        // --------------------- 3) No Path Case ---------------------
        System.out.println("Test 3: No path (Mona from New Cairo -> Maadi)");
        Request r3 = p4.request_ride(newCairo, maadi, cityMap);
        if (r3 == null) {
            System.out.println("No available path between New Cairo and Maadi (expected).\n");
        }
        System.out.println();

        // --------------------- 4) No Drivers Available ---------------------
        System.out.println("Test 4: No drivers available (simulate)");
        Request r4 = p3.request_ride(nasrCity, downtown, cityMap);
        if (r4 != null) {
            RideManager rm4 = new RideManager(new ArrayList<>(), r4, cityMap, new Payment(r4.getEstimatedPrice(), PaymentType.wallet, optBasic));
            rm4.createRide(); // should handle 'no available driver'
        }
        System.out.println();

        // --------------------- 5) Driver views & accepts requests ---------------------
        System.out.println("Test 5: Driver views and accepts pending ride requests\n");
        Queue<Request> rideQueue = new LinkedList<>();

        Request rq1 = p1.request_ride(maadi, downtown, cityMap);
        Request rq2 = p2.request_ride(downtown, giza, cityMap);
        Request rq3 = p3.request_ride(nasrCity, newCairo, cityMap);

        if (rq1 != null) rideQueue.add(rq1);
        if (rq2 != null) rideQueue.add(rq2);
        if (rq3 != null) rideQueue.add(rq3);


        d4.viewRideRequests(rideQueue);
        d4.Accept_Request(rideQueue);    // accept first

        System.out.println("\nRemaining Requests After Acceptance:");
        d4.viewRideRequests(rideQueue);
        System.out.println();
        // --------------------- 6) Small Stress Test ---------------------
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

            Payment pxPay = new Payment(rx.getEstimatedPrice(),
                    (i % 2 == 0) ? PaymentType.wallet : PaymentType.credit,
                    (i % 3 == 0) ? optTipsDonate : optBasic);

            RideManager rmx = new RideManager(allDrivers, rx, cityMap, pxPay);
            rmx.createRide();
            rmx.markDriverArrived();
            rmx.markPassengerArrived();
            rmx.completeRide();
        }

        System.out.println("\n=== All tests completed successfully ===");

        // --------------------- 7) Passenger cancels a ride ---------------------
        System.out.println("Test 7: Passenger cancels a ride\n");

        Request cancelReq = p1.request_ride(maadi, giza, cityMap);
        if (cancelReq != null) {
            Payment cancelPay = new Payment(cancelReq.getEstimatedPrice(), PaymentType.wallet, optBasic);

            RideManager cancelManager = new RideManager(allDrivers, cancelReq, cityMap, cancelPay);
            cancelManager.createRide();

            System.out.println("\n>>> Passenger decides to cancel the ride...");
            p1.cancelRide(cancelManager);

            System.out.println("\nAfter cancellation:");
            System.out.println("Passenger Wallet: " + p1.getWalletBalance() + " EGP");
            System.out.println("Driver Wallet: " + d1.getWalletBalance() + " EGP");
        }

    // --------------------- 8) Count completed rides ---------------------
        System.out.println("\nTest 8: Count Completed Rides");

    // Combine all passengers' histories into one list to simulate system-wide stats
        List<RideHistory> allHistories = new ArrayList<>();
        allHistories.addAll(p1.getRideHistory());
        allHistories.addAll(p2.getRideHistory());
        allHistories.addAll(p3.getRideHistory());
        allHistories.addAll(p4.getRideHistory());

        int completedCount = RideHistory.getRideCounts(allHistories);

        System.out.println(" Total completed rides in the system: " + completedCount);

        // --------------------- 1) Normal Ride ---------------------
        System.out.println("Test 1: Normal ride (Karim from Maadi -> Giza)");
        Request r11 = p1.request_ride(maadi, giza, cityMap);
        if (r11 != null) {
            Payment pay1 = new Payment(r11.getEstimatedPrice(), PaymentType.wallet, optTipsDonate);
            RideManager rm1 = new RideManager(allDrivers, r1, cityMap, pay1);
            rm1.createRide();

            System.out.println("\n>>> Simulating DRIVER delay...");
            rm1.checkForDelay(25);

            rm1.markDriverArrived();

            System.out.println("\n>>> Simulating PASSENGER delay...");
            rm1.driverArrivedToPassenger = true;
            rm1.checkForDelay(15);

            rm1.markPassengerArrived();
            rm1.completeRide();

            Set<ProblemType> probs = new HashSet<>(Arrays.asList(ProblemType.DRIVER_BEHAVIOR));
            p1.ReportProblem(rm1, probs, "Driver was late 5 minutes.");
        }
        System.out.println();
    }
}

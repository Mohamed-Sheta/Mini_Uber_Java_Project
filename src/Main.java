import Model.*;
import services.*;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== 🚀 Starting Ultimate Ride-Sharing Simulation ===\n");

        Random rand = new Random();

        List<Location> locations = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            locations.add(new Location("Location_" + i));
        }

        MapGraph map = new MapGraph();

        for (Location loc : locations) {
            Set<Location> connected = new HashSet<>();
            while (connected.size() < 3) {
                Location target = locations.get(rand.nextInt(locations.size()));
                if (!target.equals(loc)) connected.add(target);
            }
            for (Location target : connected) {
                double distance = 5 + rand.nextInt(50);
                map.addEdge(loc, target, distance);
            }
        }

        System.out.println("✅ 50 Locations created and linked.\n");

        List<Driver> drivers = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Location loc = locations.get(rand.nextInt(locations.size()));
            boolean active = rand.nextBoolean();
            drivers.add(new Driver("LP" + i, "CarModel" + i, active,
                    "D" + i, "Driver_" + i, "010000000" + i,
                    "driver" + i + "@email.com", 50 + rand.nextInt(200),
                    10 + rand.nextInt(50), 3 + rand.nextDouble() * 2, loc,
                    new ArrayList<>()));
        }

        System.out.println("✅ 30 Drivers created.\n");

        List<Passenger> passengers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Location loc = locations.get(rand.nextInt(locations.size()));
            passengers.add(new Passenger("P" + i, "Passenger_" + i,
                    "01111111" + i, "passenger" + i + "@email.com",
                    100 + rand.nextInt(200), 50 + rand.nextInt(100),
                    3 + rand.nextDouble() * 2, loc, new ArrayList<>()));
        }

        System.out.println("✅ 15 Passengers created.\n");


        List<Request> allRequests = new ArrayList<>();
        for (Passenger passenger : passengers) {
            int numRequests = 3 + rand.nextInt(5); // 3-7 رحلات لكل راكب
            for (int r = 0; r < numRequests; r++) {
                Location origin = passenger.getCurrentLocation();
                Location destination = locations.get(rand.nextInt(locations.size()));
                while (origin.equals(destination)) {
                    destination = locations.get(rand.nextInt(locations.size()));
                }
                Request req = passenger.request_ride(origin, destination, map);
                allRequests.add(req);
            }
        }

        System.out.println("✅ Multiple ride requests created.\n");


        List<Payment> allPayments = new ArrayList<>();
        for (Request req : allRequests) {
            Option opt = new Option();
            if (rand.nextBoolean()) {
                opt.enableTips(true);
                opt.giveTips(rand.nextInt(20));
            }
            if (rand.nextBoolean()) {
                opt.enableDonation(true);
                opt.giveDonation(rand.nextInt(10), "CharityOrg");
            }
            Payment payment = new Payment(req.getEstimatedPrice(),
                    rand.nextBoolean() ? PaymentType.wallet : PaymentType.credit,
                    opt);
            allPayments.add(payment);
        }

        System.out.println("✅ Payments with Tips & Donations ready.\n");


        List<RideManager> allRideManagers = new ArrayList<>();
        int rideCounter = 1;

        for (int i = 0; i < allRequests.size(); i++) {
            Request req = allRequests.get(i);
            Payment payment = allPayments.get(i);

            List<Driver> activeDrivers = drivers.stream().filter(Driver::isActive).collect(Collectors.toList());
            RideManager rm = new RideManager(activeDrivers, req, map, payment);
            allRideManagers.add(rm);

            System.out.println("\n--- Creating Ride #" + rideCounter + " ---");
            rm.createRide();
            rideCounter++;
        }


        rideCounter = 1;
        for (RideManager rm : allRideManagers) {
            System.out.println("\n=== Ride #" + rideCounter + " Simulation ===");
            rm.markDriverArrived();
            rm.markPassengerArrived();

            rm.setPassengerWantsToRate(rand.nextBoolean());
            rm.setDriverWantsToRate(rand.nextBoolean());

            rm.setPassengerRatingValue(1 + rand.nextInt(5));
            rm.setDriverRatingValue(1 + rand.nextInt(5));

            if (rm.getRequest().getStatus() != Status.Cancelled) {
                rm.completeRide();
            }
            rideCounter++;
        }

        System.out.println("\n✅ All rides completed with payments & ratings.\n");


        System.out.println("\n--- Testing Cancellations ---\n");
        for (int i = 0; i < 10; i++) {
            RideManager rm = allRideManagers.get(rand.nextInt(allRideManagers.size()));
            Passenger p = rm.getRequest().getPassenger();
            p.cancelRide(rm);
        }

        System.out.println("\n--- Reporting Problems ---\n");
        for (int i = 0; i < 10; i++) {
            RideManager rm = allRideManagers.get(rand.nextInt(allRideManagers.size()));
            Passenger p = rm.getRequest().getPassenger();
            Set<ProblemType> issues = new HashSet<>();
            if (rand.nextBoolean()) issues.add(ProblemType.DRIVER_LATE);
            if (rand.nextBoolean()) issues.add(ProblemType.RECKLESS_DRIVING);
            if (rand.nextBoolean()) issues.add(ProblemType.FARE_DISPUTE);
            p.ReportProblem(rm, issues, "Automated test problem for ride ID " + rm.getRequest().getRequestId());
        }


        System.out.println("\n--- Sample Profiles ---\n");
        for (int i = 0; i < 5; i++) {
            passengers.get(rand.nextInt(passengers.size())).showProfile();
            drivers.get(rand.nextInt(drivers.size())).showProfile();
        }

        System.out.println("\n--- Printing All Ride Histories ---\n");
        for (Passenger p : passengers) {
            for (RideHistory rh : p.getRideHistory()) {
                System.out.println(rh);
            }
        }

        System.out.println("\n=== Ultimate Mega Simulation Completed ===");
    }
}
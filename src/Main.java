import Model.*;
import services.*;
import java.util.*;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        System.out.println("=== Mini Uber System Full Test Start ===\n");

        // --------------------- Locations ---------------------
        Location locA = new Location("Downtown");
        Location locB = new Location("Airport");
        Location locC = new Location("Mall");
        Location locD = new Location("University");
        Location locE = new Location("Hospital");
        Location locF = new Location("Train Station");
        Location locG = new Location("Stadium");
        Location locH = new Location("Bus Station");
        Location locI = new Location("Cinema");
        Location locJ = new Location("Hotel");

        MapGraph cityMap = new MapGraph();

        cityMap.addLocation(locA);
        cityMap.addLocation(locB);
        cityMap.addLocation(locC);
        cityMap.addLocation(locD);
        cityMap.addLocation(locE);
        cityMap.addLocation(locF);
        cityMap.addLocation(locG);
        cityMap.addLocation(locH);
        cityMap.addLocation(locI);
        cityMap.addLocation(locJ);

        cityMap.addEdge(locA, locB, 15.0);
        cityMap.addEdge(locB, locA, 15.0);
        cityMap.addEdge(locA, locC, 5.0);
        cityMap.addEdge(locC, locA, 5.0);
        cityMap.addEdge(locC, locD, 10.0);
        cityMap.addEdge(locD, locC, 10.0);
        cityMap.addEdge(locD, locE, 12.0);
        cityMap.addEdge(locE, locD, 12.0);
        cityMap.addEdge(locB, locE, 20.0);
        cityMap.addEdge(locE, locB, 20.0);
        cityMap.addEdge(locC, locF, 8.0);
        cityMap.addEdge(locF, locC, 8.0);
        cityMap.addEdge(locG, locH, 7.0);
        cityMap.addEdge(locH, locG, 7.0);
        cityMap.addEdge(locI, locJ, 6.0);
        cityMap.addEdge(locJ, locI, 6.0);
        cityMap.addEdge(locF, locH, 9.0);
        cityMap.addEdge(locH, locF, 9.0);

        System.out.println("MapGraph setup completed.\n");

        // --------------------- Drivers ---------------------
        List<RideHistory> emptyHistory = new ArrayList<>();

        Driver driver1 = new Driver("ABC123", "Toyota Camry", true, "SSN001", "Ahmed", "0100000001", "ahmed@mail.com", 100.0, 50.0, locA, emptyHistory);
        Driver driver2 = new Driver("XYZ987", "Honda Civic", true, "SSN002", "Mohamed", "0100000002", "mohamed@mail.com", 80.0, 30.0, locB, emptyHistory);
        Driver driver3 = new Driver("LMN456", "Hyundai Elantra", false, "SSN003", "Sara", "0100000003", "sara@mail.com", 120.0, 60.0, locC, emptyHistory);
        Driver driver4 = new Driver("DEF789", "BMW X5", true, "SSN004", "Youssef", "0100000004", "youssef@mail.com", 200.0, 100.0, locG, emptyHistory);
        Driver driver5 = new Driver("GHI321", "Mercedes C200", true, "SSN005", "Mona", "0100000005", "mona@mail.com", 150.0, 75.0, locI, emptyHistory);

        List<Driver> allDrivers = new ArrayList<>();
        allDrivers.add(driver1);
        allDrivers.add(driver2);
        allDrivers.add(driver3);
        allDrivers.add(driver4);
        allDrivers.add(driver5);

        System.out.println("Drivers created and list populated.\n");

        // --------------------- Passengers ---------------------
        Passenger passenger1 = new Passenger("PSSN001", "Omar", "0110000001", "omar@mail.com", 200.0, 100.0, locF, new ArrayList<>());
        Passenger passenger2 = new Passenger("PSSN002", "Nour", "0110000002", "nour@mail.com", 50.0, 10.0, locD, new ArrayList<>());
        Passenger passenger3 = new Passenger("PSSN003", "Hana", "0110000003", "hana@mail.com", 500.0, 300.0, locA, new ArrayList<>());
        Passenger passenger4 = new Passenger("PSSN004", "Khaled", "0110000004", "khaled@mail.com", 100.0, 50.0, locC, new ArrayList<>());
        Passenger passenger5 = new Passenger("PSSN005", "Laila", "0110000005", "laila@mail.com", 1000.0, 500.0, locG, new ArrayList<>());

        System.out.println("Passengers created.\n");

        // --------------------- Payment Options ---------------------
        Option option1 = new Option();
        option1.enableTips(true);
        option1.enableDonation(true);
        option1.giveTips(15.0);
        option1.giveDonation(20.0, "CharityOrg");

        Option option2 = new Option();
        option2.enableTips(true);
        option2.enableDonation(false);
        option2.giveTips(10.0);

        Option option3 = new Option();
        option3.enableTips(false);
        option3.enableDonation(true);
        option3.giveDonation(50.0, "HealthFund");

        System.out.println("Payment options configured.\n");

        // --------------------- Ride Requests ---------------------
        Request ride1 = passenger1.request_ride(locF, locB, cityMap);
        Request ride2 = passenger2.request_ride(locD, locC, cityMap);
        Request ride3 = passenger3.request_ride(locA, locE, cityMap);
        Request ride4 = passenger4.request_ride(locC, locD, cityMap);
        Request ride5 = passenger5.request_ride(locG, locJ, cityMap);

        List<Request> allRides = Arrays.asList(ride1, ride2, ride3, ride4, ride5);

        // --------------------- Payments ---------------------
        List<Payment> allPayments = new ArrayList<>();
        for (int i = 0; i < allRides.size(); i++) {
            Request r = allRides.get(i);
            Option opt = (i % 3 == 0) ? option1 : ((i % 3 == 1) ? option2 : option3);
            PaymentType type = (i % 2 == 0) ? PaymentType.wallet : PaymentType.credit;
            if (r != null) {
                allPayments.add(new Payment(r.getEstimatedPrice(), type, opt));
            } else {
                allPayments.add(null);
            }
        }

        System.out.println("Payments created.\n");

        // --------------------- Ride Managers ---------------------
        List<RideManager> allManagers = new ArrayList<>();
        for (int i = 0; i < allRides.size(); i++) {
            Request r = allRides.get(i);
            Payment p = allPayments.get(i);
            if (r != null && p != null) {
                RideManager rm = new RideManager(allDrivers, r, cityMap, p);
                allManagers.add(rm);
                rm.createRide();
                rm.markDriverArrived();
                rm.markPassengerArrived();
                rm.completeRide();
            }
        }

        System.out.println("All RideManagers processed.\n");

        // --------------------- Problem Reports ---------------------
        Set<ProblemType> probs1 = new HashSet<>(Arrays.asList(ProblemType.DRIVER_BEHAVIOR, ProblemType.FARE_DISPUTE));
        passenger1.ReportProblem(allManagers.get(0), probs1, "Driver was rude and fare too high.");

        Set<ProblemType> probs2 = new HashSet<>(Collections.singletonList(ProblemType.TECHNICAL_ISSUE));
        passenger2.ReportProblem(allManagers.get(1), probs2, "AC not working.");

        Set<ProblemType> probs3 = new HashSet<>(Collections.singletonList(ProblemType.OTHER_ISSUE));
        passenger3.ReportProblem(allManagers.get(2), probs3, "Misc issue.");

        System.out.println("Problem reports submitted.\n");

        // --------------------- Stress Test ---------------------
        System.out.println("--- Stress Test: 200 Random Rides ---");
        Random rand = new Random();
        List<Passenger> passengers = Arrays.asList(passenger1, passenger2, passenger3, passenger4, passenger5);
        List<Location> locations = Arrays.asList(locA, locB, locC, locD, locE, locF, locG, locH, locI, locJ);

        for (int i = 0; i < 200; i++) {
            Passenger p = passengers.get(rand.nextInt(passengers.size()));
            Location start = locations.get(rand.nextInt(locations.size()));
            Location end = locations.get(rand.nextInt(locations.size()));
            while (end == start) {
                end = locations.get(rand.nextInt(locations.size()));
            }
            Request r = p.request_ride(start, end, cityMap);
            if (r == null) continue;
            Payment pay = new Payment(r.getEstimatedPrice(), PaymentType.wallet, option1);
            RideManager rm = new RideManager(allDrivers, r, cityMap, pay);
            rm.createRide();
            rm.markDriverArrived();
            rm.markPassengerArrived();
            rm.completeRide();
        }

        System.out.println("Stress test completed.\n");
        System.out.println("=== Mini Uber System Full Test End ===");
    }
}


import Model.*;
import services.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- 🌍 System Setup ---\n");

        Location cairo = new Location("Cairo");
        Location giza = new Location("Giza");
        Location helioplis = new Location("Heliopolis");
        Location nasrCity = new Location("Nasr City");
        Location isolated = new Location("Isolated Region");

        MapGraph map = new MapGraph();
        map.addEdge(cairo, giza, 10.5);
        map.addEdge(giza, cairo, 10.5);
        map.addEdge(cairo, helioplis, 15.0);
        map.addEdge(helioplis, cairo, 15.0);
        map.addEdge(helioplis, nasrCity, 5.2);
        map.addEdge(nasrCity, helioplis, 5.2);
        map.addEdge(giza, nasrCity, 25.0);
        map.addEdge(nasrCity, giza, 25.0);

        Driver driver1 = new Driver("ABC-123", "Toyota Corolla", true, "111111", "Ahmed", "0101001", "a@mail.com", 50.0, 100.0, 4.8, cairo, new ArrayList<>());
        Driver driver2 = new Driver("XYZ-789", "Hyundai Elantra", true, "222222", "Mohamed", "0102002", "m@mail.com", 20.0, 50.0, 4.5, giza, new ArrayList<>());

        Passenger passenger1 = new Passenger("333333", "Sarah", "0103003", "s@mail.com", 70.0, 20.0, 4.9, helioplis, new ArrayList<>());
        Passenger passenger2 = new Passenger("444444", "Omar", "0104004", "o@mail.com", 15.0, 5.0, 4.0, nasrCity, new ArrayList<>());

        List<Driver> allDrivers = Arrays.asList(driver1, driver2);

        System.out.println("\n--- 👤 Show Profile ---");
        passenger1.showProfile();
        driver1.showProfile();
        System.out.println("----------------------------------------------");

        Request request1 = passenger1.request_ride(passenger1.getCurrentLocation(), giza, map);
        Request request2 = passenger2.request_ride(passenger2.getCurrentLocation(), cairo, map);

        System.out.println("\n--- ❌ (Isolated Request Test) ---");
        Request isolatedRequest = passenger2.request_ride(passenger2.getCurrentLocation(), isolated, map);

        if (request1 == null) return;

        Payment paymentProcessor = new Payment(request1.getEstimatedPrice(), PaymentType.wallet, new Option());
        RideManager manager1 = new RideManager(allDrivers, request1, map, paymentProcessor);

        manager1.createRide();
        Driver assignedDriver = manager1.getCurrentDriver();
        if (assignedDriver == null) return;


        Queue<Request> driverRequestsQueue = new LinkedList<>();
        driverRequestsQueue.add(request1);
        driverRequestsQueue.add(request2);

        System.out.println("\n--- 🚕 Driver Methods Test ---");

        driver1.viewRideRequests(driverRequestsQueue);

        boolean accepted = driver1.Accept_Request(driverRequestsQueue);
        System.out.println("Request Accepted by Driver: " + accepted);

        System.out.println("----------------------------------------------");

        System.out.println("\n--- 🛣 Ride Progression Simulation ---");
        manager1.markDriverArrived();
        manager1.markPassengerArrived();

        Option options = paymentProcessor.getOptions();
        options.enableTips(true);
        options.enableDonation(true);
        options.giveTips(5.0);
        options.giveDonation(2.0, "Charity X");

        manager1.completeRide();

        System.out.println("\n--- ⭐ Ratings & History Test ---");

        int passengerRatingForDriver = 5;
        int driverRatingForPassenger = 4;

        manager1.setPassengerWantsToRate(true);
        manager1.setDriverWantsToRate(true);
        manager1.setPassengerRatingValue(passengerRatingForDriver);
        manager1.setDriverRatingValue(driverRatingForPassenger);


        System.out.println("Sarah's (Passenger) latest rating for Driver: " + passenger1.getLatestDriverRating() + " stars");
        System.out.println("Ahmed's (Driver) latest rating for Passenger: " + driver1.getLatestPassengerRating() + " stars");

        System.out.println("\n--- 💰 Balances After Payment ---");
        System.out.println("Sarah's (Passenger) Wallet Balance: $" + String.format("%.2f", passenger1.getWalletBalance()));
        System.out.println("Ahmed's (Driver) Wallet Balance: $" + String.format("%.2f", driver1.getWalletBalance()));

        System.out.println("\n--- ⚠️ Report Problem Test ---");

        passenger1.ReportProblem(manager1, Set.of(ProblemType.DRIVER_BEHAVIOR, ProblemType.RECKLESS_DRIVING), "Driver was using the phone and driving too fast.");

        System.out.println("\n--- 🗺 MapGraph Methods Test ---");
        double shortestDist = map.shortestDistance(helioplis, giza);
        System.out.println("Shortest distance between Heliopolis and Giza: " + String.format("%.2f", shortestDist) + " km");

        List<Location> path = map.nodes_of_road(helioplis, giza);
        System.out.print("Path between Heliopolis and Giza: ");
        path.forEach(loc -> System.out.print(loc.getName() + (path.indexOf(loc) < path.size() - 1 ? " -> " : "")));
        System.out.println();
    }
}
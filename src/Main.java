import Model.*;
import services.Payment;
import services.Request;
import services.RideManager;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // --- 1. SETUP: Locations and Map ---
        System.out.println("--- 📍 1. Setting up Locations and MapGraph ---");
        Location cairo = new Location("Cairo", 30.0444, 31.2357);
        Location giza = new Location("Giza", 30.0131, 31.2089);
        Location alex = new Location("Alexandria", 31.2001, 29.9187);
        Location newCairo = new Location("New Cairo", 30.0333, 31.4251);

        MapGraph graph = new MapGraph();
        graph.addEdge(cairo, giza, 10, 15);
        graph.addEdge(giza, alex, 200, 120);
        graph.addEdge(cairo, newCairo, 30, 40);
        graph.addEdge(newCairo, giza, 35, 45); // Bidirectional edges are not assumed, so we add a path back if needed

        // Test shortest path calculation (Dijkstra)
        List<Location> path = graph.dijkstraShortestPath(cairo, alex);
        System.out.print("Path from Cairo to Alex: ");
        path.forEach(loc -> System.out.print(loc.getName() + (path.indexOf(loc) < path.size() - 1 ? " -> " : "")));
        double rideDistance = graph.shortestDistance(cairo, alex); // Should be 210.0 km via Giza
        System.out.println("\nCalculated Shortest Distance: " + String.format("%.2f", rideDistance) + " km");


        // --- 2. SETUP: Users and Drivers ---
        System.out.println("\n--- 🧑‍🤝‍🧑 2. Creating Passenger and Driver Profiles ---");

        // Passenger
        List<RideHistory> ahmedHistory = new ArrayList<>();
        Passenger ahmed = new Passenger(
                "123456789", "Ahmed Ali", "01001234567", "a.ali@example.com",
                500.0, 200.0, 0.0, cairo, ahmedHistory // Start at Cairo
        );
        ahmed.showProfile();

        // Driver (Nearest to Cairo)
        List<RideHistory> mohamedHistory = new ArrayList<>();
        Driver mohamed = new Driver(
                "ABC-123", "Hyundai Elantra", true, // Active Driver
                "987654321", "Mohamed Sayed", "01119876543", "m.sayed@example.com",
                100.0, 0.0, 0.0, giza, mohamedHistory // Start at Giza (10km from origin)
        );
        mohamed.showProfile();

        // Other drivers (to ensure RideManager picks the nearest)
        Driver fatma = new Driver(
                "XYZ-789", "Kia Sportage", true,
                "112233445", "Fatma Hassan", "01221122334", "f.hassan@example.com",
                80.0, 0.0, 0.0, newCairo, new ArrayList<>() // 30km from origin
        );
        List<Driver> allDrivers = new ArrayList<>();
        allDrivers.add(mohamed);
        allDrivers.add(fatma);


        // --- 3. RIDE REQUEST and ASSIGNMENT ---
        System.out.println("\n--- 📝 3. Ahmed requests a ride (Cairo to Alex) ---");

        // Passenger.request_ride() method
        Request ahmedRequest = ahmed.request_ride(cairo, alex,graph);

        if (ahmedRequest == null) return;

        // RideManager methods
        RideManager rideManager = new RideManager(allDrivers, ahmedRequest, graph);
        rideManager.createRide(); // Internally calls assignNearestDriver() and updates status

        // Final check on assignment and status
        System.out.println("Current Request Status: " + ahmedRequest.getStatus()); // Should be Accepted


        // --- 4. RIDE COMPLETION and PAYMENT ---
        System.out.println("\n--- 💰 4. Simulating Ride Completion and Payment ---");

        // Assume ride is completed (Driver takes Passenger from Origin to Destination)
        ahmedRequest.updateStatus(Status.Completed);
        System.out.println("Ride Status Updated to: " + ahmedRequest.getStatus());

        // Payment processing
        double estimatedPrice = ahmedRequest.getEstimatedPrice();

        // Create an Option object and use its methods
        Option rideOptions = new Option();
        rideOptions.enableTips(true);
        rideOptions.enableDonation(true);

        // Option.giveTips() and Option.giveDonation() methods
        rideOptions.giveTips(15.0); // Ahmed gives $15 tip
        rideOptions.giveDonation(5.0, "Children's Charity"); // Ahmed gives $5 donation

        // Payment object (uses estimated price, Credit method, and options)
        Payment payment = new Payment(estimatedPrice, PaymentType.credit, rideOptions);

        // Payment.updateProcessPayment() method (handles tips/donation and processes transaction)
        payment.updateProcessPayment(ahmed, mohamed);

        // Check final balances (Person.getWalletBalance() and Person.getCreditBalance())
        System.out.println("\n--- Post-Payment Balances ---");
        System.out.println("Ahmed's New Credit Balance: $" + String.format("%.2f", ahmed.getCreditBalance())); // Should be reduced
        System.out.println("Mohamed's New Wallet Balance: $" + String.format("%.2f", mohamed.getWalletBalance())); // Should be increased


        // --- 5. RATING and RIDE HISTORY ---
        System.out.println("\n--- ⭐ 5. Mutual Rating and History Update ---");

        // Create a history entry for the completed ride
        RideHistory history = new RideHistory(mohamed, ahmed, 0, 0);

        // Passenger.RateDriver() method
        ahmed.RateDriver(history, 5); // Ahmed rates Mohamed 5 stars

        // Driver.RatePassenger() method
        mohamed.RatePassenger(history, 4); // Mohamed rates Ahmed 4 stars

        // Add history to both users
        ahmedHistory.add(history);
        mohamedHistory.add(history);

        // Check the average rating (Person.getAverageRating() method)
        System.out.println("\n--- Rating Summary ---");
        System.out.println("Ahmed's calculated rating (Driver ratings): " + String.format("%.1f", ahmed.getAverageRating()) + " stars"); // Should be 4.0
        System.out.println("Mohamed's calculated rating (Passenger ratings): " + String.format("%.1f", mohamed.getAverageRating()) + " stars"); // Should be 5.0

        // Check ride count (RideHistory.getRideCounts() method)
        System.out.println("Ahmed's Total Completed Rides: " + RideHistory.getRideCounts(ahmedHistory)); // Should be 1
    }
}
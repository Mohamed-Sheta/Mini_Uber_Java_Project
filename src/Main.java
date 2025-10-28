import Model.Location;
import Model.MapGraph;
import Model.Passenger;
import Model.Request;

public class Main {
    public static void main(String[] args) {
        Location cairo = new Location("Cairo", 30.0444, 31.2357);
        Location giza = new Location("Giza", 30.0131, 31.2089);
        Location alex = new Location("Alexandria", 31.2001, 29.9187);
        Location aswan = new Location("Aswan", 24.0889, 32.8998);

        MapGraph graph = new MapGraph();
        graph.addEdge(cairo, giza, 10, 15);
        graph.addEdge(giza, alex, 200, 120);
        graph.addEdge(cairo, alex, 220, 140);
        graph.addEdge(cairo, aswan, 870, 600);
        graph.addEdge(alex, aswan, 950, 720);

        System.out.println("\nShortest path from Cairo to Aswan:");

        var path = graph.dijkstraShortestPath(cairo, alex);
//        System.out.println(path);

        double totalDistance = graph.shortestDistance(cairo, alex);

        for (int i = 0; i < path.size(); i++) {
            System.out.print(path.get(i).getName());
            if (i < path.size() - 1) {
                System.out.print(" → ");
            }
        }

        System.out.println("\nTotal Distance: " + totalDistance + " km");

        Passenger passenger = new Passenger(
                cairo,                // current location
                "30102001",           // SSN
                "Ahmed Ashraf",       // name
                "01123456789",        // phone
                "ahmed@email.com",    // email
                20000,                // wallet balance
                5000,                 // credit balance
                4.5                   // rating
        );
        Request r1 = new Request();
        r1.p = passenger;
        r1.Origin = cairo;
        r1.destination = aswan;

        System.out.println("=== Trip Request Details ===");
        System.out.println("From: " + r1.Origin.getName());
        System.out.println("To: " + r1.destination.getName());
        System.out.println("Passenger: " + passenger.getName());
        r1.set_Distnace();
        System.out.println(r1.distance);
        r1.pymenttransaction();
    }
}
// كمان برضو تعمل time
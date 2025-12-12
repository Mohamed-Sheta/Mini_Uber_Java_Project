package services;

import Model.Edge;
import Model.Location;
import Model.Driver;
import Model.Passenger;
import Model.RideHistory;
import DAO.*;

import java.util.*;

public class MapGraph {

    public Map<Location, List<Edge>> adjacency_list;

    public MapGraph() {
        this.adjacency_list = new HashMap<>();
    }

    public static class CityMapSetup {

        public MapGraph cityMap;
        public List<Location> locations;
        public List<Driver> drivers;
        public List<Passenger> passengers;
        public Map<Driver, Long> driverIdMap;
        public Map<Passenger, Long> passengerIdMap;

        public CityMapSetup() {
            cityMap = new MapGraph();
            locations = new ArrayList<>();
            drivers = new ArrayList<>();
            passengers = new ArrayList<>();
            driverIdMap = new HashMap<>();
            passengerIdMap = new HashMap<>();
        }

        public void initializeAll() {
            initializeLocations();
            initializeEdges();
            initializeDrivers();
            initializePassengers();
            System.out.println("MapGraph + DB setup done.\n");
        }

        private void initializeLocations() {

            LocationDAO locationDAO = new LocationDAO();

            // Use the static method to get predefined locations
            locations = MapGraph.getPredefinedLocations();

            for (Location loc : locations) {
                cityMap.addLocation(loc);
            }

            try {
                // First, check which locations already exist in database
                List<LocationDAO.LocationRow> existingLocations = locationDAO.showAll();
                Set<String> existingNames = new HashSet<>();
                for (LocationDAO.LocationRow row : existingLocations) {
                    existingNames.add(row.name);
                }

                // Only insert locations that don't already exist
                int insertedCount = 0;
                int skippedCount = 0;
                for (Location loc : locations) {
                    if (!existingNames.contains(loc.getName())) {
                        long id = locationDAO.insert(loc);
                        System.out.println("[DB] Insert Location: " + loc.getName() + " -> id=" + id);
                        insertedCount++;
                    } else {
                        System.out.println("[DB] Skip Location (already exists): " + loc.getName());
                        skippedCount++;
                    }
                }
                System.out.println("[DB] Location summary: " + insertedCount + " inserted, " + skippedCount + " skipped, " + existingLocations.size() + " total in DB");
            } catch (Exception e) {
                System.out.println("[DB] Insert locations error: " + e.getMessage());
                e.printStackTrace();
            }
        }


        private void initializeEdges() {
            if (locations.size() < 10) return;

            EdgeDAO edgeDAO = new EdgeDAO();

            // All 10 locations
            Location downtown = locations.get(0);           // Downtown Cairo
            Location nasrCity = locations.get(1);           // Nasr City
            Location maadi = locations.get(2);              // Maadi
            Location giza = locations.get(3);               // Giza
            Location newCairo = locations.get(4);           // New Cairo
            Location hadaeqAlQubba = locations.get(5);      // Hadaeq Al-Qubba
            Location elKorba = locations.get(6);            // El Korba
            Location abbasiya = locations.get(7);           // Abbasiya
            Location helmeyetElZeitoun = locations.get(8);  // Helmeyet El-Zeitoun
            Location elObour = locations.get(9);            // El Obour

            // Original edges (keep existing routes)
            cityMap.addEdge(downtown, nasrCity, 6.0);
            cityMap.addEdge(nasrCity, downtown, 6.0);
            cityMap.addEdge(downtown, maadi, 8.0);
            cityMap.addEdge(maadi, downtown, 8.0);
            cityMap.addEdge(maadi, giza, 5.0);
            cityMap.addEdge(giza, maadi, 5.0);
            cityMap.addEdge(nasrCity, newCairo, 10.0);
            cityMap.addEdge(newCairo, nasrCity, 10.0);

            // New edges connecting the 5 new locations to existing network
            // Hadaeq Al-Qubba connections
            cityMap.addEdge(downtown, hadaeqAlQubba, 4.0);
            cityMap.addEdge(hadaeqAlQubba, downtown, 4.0);
            cityMap.addEdge(hadaeqAlQubba, abbasiya, 2.0);
            cityMap.addEdge(abbasiya, hadaeqAlQubba, 2.0);

            // El Korba connections
            cityMap.addEdge(nasrCity, elKorba, 5.0);
            cityMap.addEdge(elKorba, nasrCity, 5.0);
            cityMap.addEdge(elKorba, helmeyetElZeitoun, 3.0);
            cityMap.addEdge(helmeyetElZeitoun, elKorba, 3.0);

            // Abbasiya connections
            cityMap.addEdge(abbasiya, nasrCity, 4.0);
            cityMap.addEdge(nasrCity, abbasiya, 4.0);
            cityMap.addEdge(abbasiya, helmeyetElZeitoun, 3.5);
            cityMap.addEdge(helmeyetElZeitoun, abbasiya, 3.5);

            // Helmeyet El-Zeitoun connections
            cityMap.addEdge(helmeyetElZeitoun, nasrCity, 4.5);
            cityMap.addEdge(nasrCity, helmeyetElZeitoun, 4.5);

            // El Obour connections
            cityMap.addEdge(newCairo, elObour, 8.0);
            cityMap.addEdge(elObour, newCairo, 8.0);
            cityMap.addEdge(nasrCity, elObour, 12.0);
            cityMap.addEdge(elObour, nasrCity, 12.0);

            try {
                final int[] insertedCount = {0}; // Use array to allow modification from inner class

                // Helper to insert edge and catch duplicate errors
                class EdgeInserter {
                    void tryInsert(Location from, Location to, double distance) {
                        try {
                            edgeDAO.insert(from, to, distance);
                            insertedCount[0]++;
                        } catch (Exception e) {
                            // Ignore duplicate key errors, count as skipped
                            if (!e.getMessage().contains("Duplicate")) {
                                System.out.println("[DB] Edge insert warning (" + from.getName() + " -> " + to.getName() + "): " + e.getMessage());
                            }
                        }
                    }
                }
                EdgeInserter inserter = new EdgeInserter();

                // Original edges
                inserter.tryInsert(downtown, nasrCity, 6.0);
                inserter.tryInsert(nasrCity, downtown, 6.0);
                inserter.tryInsert(downtown, maadi, 8.0);
                inserter.tryInsert(maadi, downtown, 8.0);
                inserter.tryInsert(maadi, giza, 5.0);
                inserter.tryInsert(giza, maadi, 5.0);
                inserter.tryInsert(nasrCity, newCairo, 10.0);
                inserter.tryInsert(newCairo, nasrCity, 10.0);

                // New edges
                inserter.tryInsert(downtown, hadaeqAlQubba, 4.0);
                inserter.tryInsert(hadaeqAlQubba, downtown, 4.0);
                inserter.tryInsert(hadaeqAlQubba, abbasiya, 2.0);
                inserter.tryInsert(abbasiya, hadaeqAlQubba, 2.0);

                inserter.tryInsert(nasrCity, elKorba, 5.0);
                inserter.tryInsert(elKorba, nasrCity, 5.0);
                inserter.tryInsert(elKorba, helmeyetElZeitoun, 3.0);
                inserter.tryInsert(helmeyetElZeitoun, elKorba, 3.0);

                inserter.tryInsert(abbasiya, nasrCity, 4.0);
                inserter.tryInsert(nasrCity, abbasiya, 4.0);
                inserter.tryInsert(abbasiya, helmeyetElZeitoun, 3.5);
                inserter.tryInsert(helmeyetElZeitoun, abbasiya, 3.5);

                inserter.tryInsert(helmeyetElZeitoun, nasrCity, 4.5);
                inserter.tryInsert(nasrCity, helmeyetElZeitoun, 4.5);

                inserter.tryInsert(newCairo, elObour, 8.0);
                inserter.tryInsert(elObour, newCairo, 8.0);
                inserter.tryInsert(nasrCity, elObour, 12.0);
                inserter.tryInsert(elObour, nasrCity, 12.0);

                System.out.println("[DB] Successfully inserted " + insertedCount[0] + " edges");
            } catch (Exception e) {
                System.out.println("[DB] Insert edges error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void initializeDrivers() {
            if (locations.isEmpty()) return;

            DriverDAO driverDAO = new DriverDAO();

            Location downtown = locations.get(0);
            Location nasrCity = locations.get(1);
            Location maadi = locations.get(2);
            Location giza = locations.get(3);

            List<RideHistory> empty = new ArrayList<>();

            // Hash the test password (same as registration flow)
            String hashedPassword = Model.Person.hashPassword("password123");

            Driver d1 = new Driver("CAR001", "Toyota Corolla", true, "SSN100",
                    "marwan wael", "01010001000", "marwan@gmail.com",
                    100.0, downtown, empty, hashedPassword);

            Driver d2 = new Driver("CAR002", "Hyundai Verna", true, "SSN101",
                    "c ali", "01010001001", "islam@gmail.com",
                    120.0, nasrCity, empty, hashedPassword);

            Driver d3 = new Driver("CAR003", "Kia Cerato", false, "SSN102",
                    "amin ahmed", "01010001002", "amin@gmail.com",
                    90.0,  giza, empty, hashedPassword);

            Driver d4 = new Driver("CAR004", "Nissan Sunny", true, "SSN103",
                    "Youssef Ibrahim", "01010001003", "youssef@gmail.com",
                    150.0, maadi, empty, hashedPassword);

            drivers.addAll(Arrays.asList(d1, d2, d3, d4));

            try {
                for (Driver d : drivers) {
                    long id = driverDAO.insert(d, d.getCurrentLocation().getName());
                    driverIdMap.put(d, id);
                }
            } catch (Exception e) {
                System.out.println("[DB] Insert drivers error: " + e.getMessage());
            }
        }

        private void initializePassengers() {
            if (locations.isEmpty()) return;

            PassengerDAO passengerDAO = new PassengerDAO();

            Location downtown = locations.get(0);
            Location nasrCity = locations.get(1);
            Location maadi = locations.get(2);
            Location newCairo = locations.get(4);

            // Hash the test password (same as registration flow)
            String hashedPassword = Model.Person.hashPassword("password123");

            // Create passengers with hashed passwords
            Passenger p1 = new Passenger("PSSN01", "ahmed ashraf",
                    "01110001001", "ahmed@gmail.com",
                    200.0, maadi, new ArrayList<>(), hashedPassword);

            Passenger p2 = new Passenger("PSSN02", "mohamed sheta",
                    "01110001002", "sheta@gmail.com",
                    200.0, downtown, new ArrayList<>(), hashedPassword);

            Passenger p3 = new Passenger("PSSN03", "mostafa hassan",
                    "01110001003", "mostafa@gmail.com",
                    500.0, nasrCity, new ArrayList<>(), hashedPassword);

            Passenger p4 = new Passenger("PSSN04", "amr nabli",
                    "01110001004", "amr@gmail.com",
                    15.0, newCairo, new ArrayList<>(), hashedPassword);

            passengers.addAll(Arrays.asList(p1, p2, p3, p4));

            try {
                for (Passenger p : passengers) {
                    long id = passengerDAO.insert(p, p.getCurrentLocation().getName());
                    passengerIdMap.put(p, id);
                }
            } catch (Exception e) {
                System.out.println("[DB] Insert passengers error: " + e.getMessage());
            }
        }
    }

    public void addLocation(Location X) {
        adjacency_list.putIfAbsent(X, new ArrayList<>());
    }

    public void addEdge(Location from, Location to, double distance) {
        addLocation(from);
        addLocation(to);
        adjacency_list.get(from).add(new Edge(from, to, distance));
    }

    public static List<Location> getPredefinedLocations() {
        List<Location> locations = new ArrayList<>();

        Location downtown = new Location("Downtown Cairo", 30.0444, 31.2357);
        Location nasrCity = new Location("Nasr City", 30.0561, 31.3300);
        Location maadi = new Location("Maadi", 29.9603, 31.2596);
        Location giza = new Location("Giza", 30.0131, 31.2089);
        Location newCairo = new Location("New Cairo", 30.0305, 31.4913);
        Location hadaeqAlQubba = new Location("Hadaeq Al-Qubba", 30.0867, 31.3020);
        Location elKorba = new Location("El Korba", 30.1127, 31.3270);
        Location abbasiya = new Location("Abbasiya", 30.0670, 31.2759);
        Location helmeyetElZeitoun = new Location("Helmeyet El-Zeitoun", 30.1134, 31.3187);
        Location elObour = new Location("El Obour", 30.2289, 31.4553);

        locations.addAll(Arrays.asList(downtown, nasrCity, maadi, giza, newCairo,
                hadaeqAlQubba, elKorba, abbasiya, helmeyetElZeitoun, elObour));

        return locations;
    }

    public List<Location> nodes_of_road(Location start, Location target) {
        Map<Location, Double> distance = new HashMap<>();
        Map<Location, Location> previous = new HashMap<>();
        PriorityQueue<Location> pq = new PriorityQueue<>(Comparator.comparingDouble(distance::get));

        for (Location loc : adjacency_list.keySet()) {
            distance.put(loc, Double.MAX_VALUE);
        }

        distance.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            Location current = pq.poll();

            if (current.equals(target)) break;

            for (Edge edge : adjacency_list.getOrDefault(current, Collections.emptyList())) {
                Location neighbor = edge.getTo();
                double newDist = distance.get(current) + edge.getDistance();

                if (newDist < distance.get(neighbor)) {
                    distance.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        return reconstructPath(start, target, previous, distance.get(target));
    }

    private List<Location> reconstructPath(Location start, Location target, Map<Location, Location> previous, Double finalDistance) {
        List<Location> path = new ArrayList<>();

        if (finalDistance.equals(Double.MAX_VALUE)) return Collections.emptyList();

        Location step = target;
        while (step != null) {
            path.add(0, step);
            step = previous.get(step);
        }

        return path;
    }

    public double shortestDistance(Location start, Location target) {
        Map<Location, Double> distance = new HashMap<>();
        PriorityQueue<Location> pq = new PriorityQueue<>(Comparator.comparingDouble(distance::get));

        for (Location loc : adjacency_list.keySet()) {
            distance.put(loc, Double.MAX_VALUE);
        }

        distance.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            Location current = pq.poll();

            if (current.equals(target)) break;

            for (Edge edge : adjacency_list.getOrDefault(current, Collections.emptyList())) {
                Location neighbor = edge.getTo();
                double newDist = distance.get(current) + edge.getDistance();

                if (newDist < distance.get(neighbor)) {
                    distance.put(neighbor, newDist);
                    pq.add(neighbor);
                }
            }
        }

        return distance.get(target);
    }
}

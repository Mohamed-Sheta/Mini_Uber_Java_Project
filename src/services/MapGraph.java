package services;
import Model.Edge;
import Model.Location;
import Model.Driver;
import Model.Passenger;
import Model.RideHistory;
import DAO.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;
import java.util.Arrays;

public class MapGraph {
    private Map<Location, List<Edge>> adjacency_list;

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

        /**
         * Initialize everything: locations, edges, drivers, and passengers
         */
        public void initializeAll() {
            initializeLocations();
            initializeEdges();
            initializeDrivers();
            initializePassengers();
            System.out.println("MapGraph + DB setup done.\n");
        }

        private void initializeLocations() {
            LocationDAO locationDAO = new LocationDAO();

            Location downtown = new Location("Downtown Cairo");
            Location nasrCity = new Location("Nasr City");
            Location maadi = new Location("Maadi");
            Location giza = new Location("Giza");
            Location newCairo = new Location("New Cairo");

            locations.addAll(Arrays.asList(downtown, nasrCity, maadi, giza, newCairo));

            for (Location loc : locations) {
                cityMap.addLocation(loc);
            }

            try {
                for (Location loc : locations) {
                    long id = locationDAO.insert(loc);
                    loc.setId((int) id);
                    System.out.println("[DB] Insert Location: " + loc.getName() + " -> id=" + id);
                }
            } catch (Exception e) {
                System.out.println("[DB] Insert locations error: " + e.getMessage());
            }
        }

        private void initializeEdges() {
            if (locations.size() < 5) return;

            EdgeDAO edgeDAO = new EdgeDAO();
            Location downtown = locations.get(0);
            Location nasrCity = locations.get(1);
            Location maadi = locations.get(2);
            Location giza = locations.get(3);
            Location newCairo = locations.get(4);

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
        }

        private void initializeDrivers() {
            if (locations.isEmpty()) return;

            DriverDAO driverDAO = new DriverDAO();
            Location downtown = locations.get(0);
            Location nasrCity = locations.get(1);
            Location maadi = locations.get(2);
            Location giza = locations.get(3);

            List<RideHistory> emptyHistory = new ArrayList<>();

            Driver d1 = new Driver("CAR001", "Toyota Corolla", true, "SSN100", "marwan wael",
                                  "01010001000", "marwan@gmail.com", 100.0, 50.0, downtown, emptyHistory);
            Driver d2 = new Driver("CAR002", "Hyundai Verna", true, "SSN101", "c ali",
                                  "01010001001", "islam@gmail.com", 120.0, 60.0, nasrCity, emptyHistory);
            Driver d3 = new Driver("CAR003", "Kia Cerato", false, "SSN102", "amin ahmed",
                                  "01010001002", "amin@gmail.com", 90.0, 45.0, giza, emptyHistory);
            Driver d4 = new Driver("CAR004", "Nissan Sunny", true, "SSN103", "Youssef Ibrahim",
                                  "01010001003", "youssef@gmail.com", 150.0, 75.0, maadi, emptyHistory);

            drivers.addAll(Arrays.asList(d1, d2, d3, d4));

            try {
                for (Driver driver : drivers) {
                    String locName = driver.getCurrentLocation() != null ?
                                    driver.getCurrentLocation().getName() : null;
                    long id = driverDAO.insert(driver, locName);
                    driverIdMap.put(driver, id);
                    System.out.println("[DB] Insert Driver: " + driver.getName() + " -> id=" + id);
                }
                System.out.println();
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

            Passenger p1 = new Passenger("PSSN01", "ahmed ashraf", "01110001001", "ahmed@gmail.com",
                                         200.0, 100.0, maadi, new ArrayList<>());
            Passenger p2 = new Passenger("PSSN02", "mohamed sheta", "01110001002", "sheta@gmail.com",
                                         40.0, 10.0, downtown, new ArrayList<>());
            Passenger p3 = new Passenger("PSSN03", "mostafa hassan", "01110001003", "mostafa@gmail.com",
                                         500.0, 250.0, nasrCity, new ArrayList<>());
            Passenger p4 = new Passenger("PSSN04", "amr nabli", "01110001004", "amr@gmail.com",
                                         15.0, 0.0, newCairo, new ArrayList<>());

            passengers.addAll(Arrays.asList(p1, p2, p3, p4));

            try {
                for (Passenger passenger : passengers) {
                    String locName = passenger.getCurrentLocation() != null ?
                                    passenger.getCurrentLocation().getName() : null;
                    long id = passengerDAO.insert(passenger, locName);
                    passengerIdMap.put(passenger, id);
                    System.out.println("[DB] Insert Passenger: " + passenger.getName() + " -> id=" + id);
                }
                System.out.println();
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

                if (newDist < distance.getOrDefault(neighbor, Double.MAX_VALUE)) {
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
        Location step = target;

        if (finalDistance == null || finalDistance.equals(Double.MAX_VALUE)) {
            System.out.println("No path found from " + start.getName() + " to " + target.getName());
            return Collections.emptyList();
        }

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

                if (newDist < distance.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distance.put(neighbor, newDist);
                    pq.add(neighbor);
                }
            }
        }

        double finalDist = distance.getOrDefault(target, Double.MAX_VALUE);
        if (finalDist == Double.MAX_VALUE) {
            System.out.println("No path found from " + start.getName() + " to " + target.getName());
        }
        return finalDist;
    }

}
package services;
import Model.Edge;
import Model.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;


public class MapGraph {
    private Map<Location, List<Edge>> adjacency_list;

    public MapGraph() {
        this.adjacency_list = new HashMap<>();
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
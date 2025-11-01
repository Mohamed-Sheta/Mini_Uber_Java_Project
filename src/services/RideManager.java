package services;

import Model.Driver;
import Model.Location;
import Model.Status;
import Model.MapGraph;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class RideManager {

    private List<Driver> availableDrivers;
    private Request request;
    private MapGraph mapGraph;

    public RideManager(List<Driver> allDrivers, Request request, MapGraph mapGraph) {
        this.availableDrivers = allDrivers.stream()
                .filter(Driver::isActive)
                .filter(d -> d.getCurrentLocation() != null)
                .collect(Collectors.toList());

        this.request = request;
        this.mapGraph = mapGraph;
    }


    public Driver assignNearestDriver() {
        if (availableDrivers.isEmpty()) {
            System.out.println("⚠️ No active drivers available to assign.");
            return null;
        }

        Location origin = request.getOrigin();

        Driver nearestDriver = availableDrivers.stream()
                .min(Comparator.comparingDouble(driver ->
                        this.mapGraph.shortestDistance(driver.getCurrentLocation(), origin)
                ))
                .orElse(null);

        if (nearestDriver != null) {
            double distanceToOrigin = this.mapGraph
                    .shortestDistance(nearestDriver.getCurrentLocation(), origin);

            if (distanceToOrigin == Double.MAX_VALUE) {
                System.out.println("⚠️ Nearest driver found, but no valid path exists on the map.");
                return null;
            }

            int estimatedTime = request.calculateEstimatedTime(distanceToOrigin);

            System.out.println("\n🌟 Nearest Driver Assigned 🌟");
            System.out.println("   Name: " + nearestDriver.getName());
            System.out.println("   Car Model: " + nearestDriver.getCarModel());
            System.out.println("   License Plate: " + nearestDriver.getLicensePlate());
            System.out.println("---");
            System.out.println("   Distance to your location: " + String.format("%.2f", distanceToOrigin) + " km");
            System.out.println("   **Estimated Arrival Time: " + estimatedTime + " minutes**");

        } else {
            System.out.println("⚠️ No nearest driver could be determined.");
        }

        return nearestDriver;
    }


    public void createRide() {
        if (request.getStatus() != Status.Pending) {
            System.out.println("❌ Error: Ride request is not in Pending status.");
            return;
        }

        System.out.println("\n--- 🚗 Attempting to Find and Assign Driver (Request ID: "
                + request.getRequestId() + ") ---");

        Driver nearestDriver = assignNearestDriver();

        if (nearestDriver == null) {
            System.out.println("❌ Failed to create ride: No suitable active drivers found.");
            request.updateStatus(Status.Cancelled);
            return;
        }

        request.updateStatus(Status.Accepted);

        System.out.println("✅ Ride Created and Driver Assigned!");
        System.out.println("   New Request Status: " + request.getStatus());
    }
}

package services;
import Model.*;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class RideManager {

    private List<Driver> availableDrivers;
    private Request request;
    private MapGraph mapGraph;
    private Payment paymentProcessor;

    private Driver currentDriver;
    private boolean driverArrivedToPassenger = false;
    private boolean passengerArrivedToDestination = false;

    private boolean passengerWantsToRate = false;
    private boolean driverWantsToRate = false;

    public RideManager(List<Driver> allDrivers, Request request,
                       MapGraph mapGraph, Payment paymentProcessor) {

        this.availableDrivers = allDrivers.stream()
                .filter(Driver::isActive)
                .filter(d -> d.getCurrentLocation() != null)
                .collect(Collectors.toList());

        this.request = request;
        this.mapGraph = mapGraph;
        this.paymentProcessor = paymentProcessor;
    }


    // -------------------------------------------------------------------------------------------------------------------

    public Driver assignNearestDriver() {
        if (availableDrivers.isEmpty() || availableDrivers == null) {
            System.out.println("⚠ No active drivers available to assign.");
            return null;
        }

        Location origin = request.getOrigin();

        Driver nearestDriver = availableDrivers.stream()
                .min(Comparator.comparingDouble(driver ->
                        this.mapGraph.shortestDistance(driver.getCurrentLocation(), origin)))
                .orElse(null);

        if (nearestDriver != null) {
            double distanceToOrigin =
                    this.mapGraph.shortestDistance(nearestDriver.getCurrentLocation(), origin);

            if (distanceToOrigin == Double.MAX_VALUE) {
                System.out.println("⚠ Nearest driver found, but no valid path exists on the map.");
                return null;
            }

            int estimatedTime = request.calculateEstimatedTime(distanceToOrigin);

            System.out.println("\n🌟 Nearest Driver Assigned 🌟");
            System.out.println("   Name: " + nearestDriver.getName());
            System.out.println("   Car Model: " + nearestDriver.getCarModel());
            System.out.println("   License Plate: " + nearestDriver.getLicensePlate());
            System.out.println("---");
            System.out.println("   Distance to your location: "
                    + String.format("%.2f", distanceToOrigin) + " km");
            System.out.println("   Estimated Arrival Time: " + estimatedTime + " minutes");

            this.currentDriver = nearestDriver;
        }

        return nearestDriver;
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void createRide() {
        if (request.getStatus() != Status.Pending) {
            System.out.println("❌ Error: Ride request is not in Pending status.");
            return;
        }

        System.out.println("\n--- 🚗 Attempting to Find and Assign Driver ---");

        Driver nearestDriver = assignNearestDriver();

        if (nearestDriver == null) {
            System.out.println("❌ Failed to create ride: No suitable active drivers found.");
            request.updateStatus(Status.Cancelled);
            return;
        }

        request.updateStatus(Status.Accepted);

        System.out.println("✅ Ride Created and Driver Assigned!");
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void markDriverArrived() {
        this.driverArrivedToPassenger = true;
        System.out.println("🚗 Driver arrived to passenger location.");
    }

    public void markPassengerArrived() {
        this.passengerArrivedToDestination = true;
        System.out.println("📍 Passenger arrived to destination.");
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void handleDelayPenalty(Driver driver, double ridePrice, String offender) {

        final double FIXED_PENALTY = 10.0;
        Passenger passenger = request.getPassenger();

        System.out.println("⚠ Delay Penalty Applied on: " + offender);

        if (offender.equalsIgnoreCase("driver")) {

            double wallet = driver.getWalletBalance();
            double credit = driver.getCreditBalance();

            if (wallet >= FIXED_PENALTY) {
                driver.updateWalletBalance(wallet - FIXED_PENALTY);
                System.out.println("🚫 Driver fined from wallet: " + FIXED_PENALTY + " EGP");
            } else if (credit >= FIXED_PENALTY) {
                driver.updateCreditBalance(credit - FIXED_PENALTY);
                System.out.println("🚫 Driver fined from credit: " + FIXED_PENALTY + " EGP");
            } else {
                driver.updateWalletBalance(wallet - FIXED_PENALTY);
                System.out.println("⚠ Wallet not enough → forcing negative balance!");
            }

        } else if (offender.equalsIgnoreCase("passenger")) {

            double wallet = passenger.getWalletBalance();
            double credit = passenger.getCreditBalance();

            if (wallet >= FIXED_PENALTY) {
                passenger.updateWalletBalance(wallet - FIXED_PENALTY);
                System.out.println("🚫 Passenger fined from wallet: " + FIXED_PENALTY + " EGP");
            } else if (credit >= FIXED_PENALTY) {
                passenger.updateCreditBalance(credit - FIXED_PENALTY);
                System.out.println("🚫 Passenger fined from credit: " + FIXED_PENALTY + " EGP");
            } else {
                passenger.updateWalletBalance(wallet - FIXED_PENALTY);
                System.out.println("⚠ Wallet not enough → forcing negative balance!");
            }
        }

        request.updateStatus(Status.Cancelled);
        System.out.println("❌ Ride cancelled due to delay.");
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void checkForDelay(int actualArrivalTime) {
        int estimated = request.getEstimatedTime();

        if (actualArrivalTime > estimated + 10) {
            double price = request.getEstimatedPrice();

            if (!driverArrivedToPassenger) {
                System.out.println("⏱ Driver delayed arrival to passenger!");
                handleDelayPenalty(currentDriver, price, "driver");
            } else if (!passengerArrivedToDestination) {
                System.out.println("⏱ Passenger delayed ride completion!");
                handleDelayPenalty(currentDriver, price, "passenger");
            }
        }
    }


    // -------------------------------------------------------------------------------------------------------------------
    public void completeRide() {
        if (request.getStatus() == Status.Cancelled) {
            System.out.println("❌ Cannot complete a cancelled ride!");
            return;
        }

        if (!driverArrivedToPassenger || !passengerArrivedToDestination) {
            System.out.println("⚠ Ride cannot complete before arrival!");
            return;
        }

        Passenger passenger = request.getPassenger();
        Driver driver = currentDriver;

        paymentProcessor.updateProcessPayment(passenger, driver);

        request.updateStatus(Status.Completed);

        System.out.println("\n🎯 Ride Completed Successfully!");
        System.out.println("➡ Please rate each other to store history.");

        saveRideHistory();
    }

    public void setPassengerWantsToRate(boolean value) {
        this.passengerWantsToRate = value;
    }

    public void setDriverWantsToRate(boolean value) {
        this.driverWantsToRate = value;
    }

    private int passengerRatingValue = 0; 
    private int driverRatingValue = 0;
    public void setPassengerRatingValue(int value) {
        this.passengerRatingValue = value;
    }

    public void setDriverRatingValue(int value) {
        this.driverRatingValue = value;
    }

    private void saveRideHistory() {
        Passenger passenger = request.getPassenger();
        Driver driver = currentDriver;

        // ✅ Apply ratings only if flags are true AND rating > 0
        if (passengerWantsToRate && passengerRatingValue > 0) {
            passenger.RateDriver(passengerRatingValue);
        }

        if (driverWantsToRate && driverRatingValue > 0) {
            driver.RatePassenger(driverRatingValue);
        }

        int driverRatingFromPassenger = passenger.getLatestDriverRating();
        int passengerRatingFromDriver = driver.getLatestPassengerRating();

        // ✅ Display messages based on flags
        if (!passengerWantsToRate && !driverWantsToRate) {
            System.out.println("ℹ No ratings provided — history saved without ratings.");
        } else if (passengerWantsToRate && !driverWantsToRate) {
            System.out.println("⭐ Passenger rated the driver (" + driverRatingFromPassenger + " stars)");
        } else if (!passengerWantsToRate && driverWantsToRate) {
            System.out.println("⭐ Driver rated the passenger (" + passengerRatingFromDriver + " stars)");
        } else {
            System.out.println("⭐ Both sides rated each other!");
        }

        RideHistory history = new RideHistory(
                driver,
                passenger,
                driverRatingFromPassenger,
                passengerRatingFromDriver
        );

        passenger.getRideHistory().add(history);
        driver.getRideHistory().add(history);

        System.out.println("✅ RideHistory Saved! (ID: " + history.getHistoryId() + ")");
    }

    public Request getRequest() {
        return this.request;
    }

    public Driver getCurrentDriver() {
        return this.currentDriver;
    }

    public Payment getPaymentProcessor() {
        return paymentProcessor;
    }
}
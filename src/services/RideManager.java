package services;
import Model.*;
import DAO.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

public class RideManager {

    private List<Driver> availableDrivers;
    private Request request;
    private MapGraph mapGraph;
    private Payment paymentProcessor;

    private Driver currentDriver;

    public boolean driverArrivedToPassenger = false;
    private boolean passengerArrivedToDestination = false;

    private boolean passengerWantsToRate = false;
    private boolean driverWantsToRate = false;

    private LocalDateTime acceptanceTime;

    // Database handling
    private RideRequestDAO rideRequestDAO;
    private RideHistoryDAO rideHistoryDAO;
    private long rideRequestId = -1;
    private Map<Passenger, Long> passengerIdMap;
    private Map<Driver, Long> driverIdMap;

    private int passengerRatingValue = 0;
    private int driverRatingValue = 0;

    public RideManager(List<Driver> allDrivers, Request request,
                       MapGraph mapGraph, Payment paymentProcessor) {

        this.availableDrivers = allDrivers.stream()
                .filter(Driver::isActive)
                .filter(d -> d.getCurrentLocation() != null)
                .collect(Collectors.toList());

        this.request = request;
        this.mapGraph = mapGraph;
        this.paymentProcessor = paymentProcessor;

        this.rideRequestDAO = new RideRequestDAO();
        this.rideHistoryDAO = new RideHistoryDAO();
    }

    public void setDatabaseMaps(Map<Passenger, Long> passengerIdMap, Map<Driver, Long> driverIdMap) {
        this.passengerIdMap = passengerIdMap;
        this.driverIdMap = driverIdMap;
    }

    // -------------------------------------------------------------------------------------------------------------------

    public Driver assignNearestDriver() {
        if (availableDrivers.isEmpty() || availableDrivers == null) {
            System.out.println(" No active drivers available to assign.");
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
                System.out.println(" Nearest driver found, but no valid path exists on the map.");
                return null;
            }

            int estimatedTime = request.calculateEstimatedTime(distanceToOrigin);

            System.out.println("\n Nearest Driver Assigned ");
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
            System.out.println(" Error: Ride request is not in Pending status.");
            return;
        }

        if (passengerIdMap != null && driverIdMap != null) {
            insertRideRequestToDB();
        }

        System.out.println("\n---  Attempting to Find and Assign Driver ---");

        Driver nearestDriver = assignNearestDriver();

        if (nearestDriver == null) {
            System.out.println(" Failed to create ride: No suitable active drivers found.");
            request.updateStatus(Status.Cancelled);
            if (rideRequestId != -1) {
                updateRideRequestInDB(Status.Cancelled, null, false, false);
            }
            return;
        }

        request.updateStatus(Status.Accepted);
        this.acceptanceTime = LocalDateTime.now();
        System.out.println(" Ride Created and Driver Assigned!");
        System.out.println(" Acceptance Time: " + acceptanceTime);

        if (rideRequestId != -1) {
            updateRideRequestInDB(Status.Accepted, acceptanceTime, false, false);
        }
    }

    private void insertRideRequestToDB() {
        try {
            rideRequestId = rideRequestDAO.insert(
                passengerIdMap.get(request.getPassenger()), null,
                request.getOrigin().getId(), request.getDestination().getId(),
                Status.Pending,
                request.getDistance(), request.getEstimatedTime(), request.getEstimatedPrice(),
                null, false, false
            );
            System.out.println("[DB] ride_requests inserted (Pending) id=" + rideRequestId);
        } catch (Exception e) {
            System.out.println("[DB] Insert ride_request error: " + e.getMessage());
        }
    }

    private void updateRideRequestInDB(Status status, LocalDateTime acceptTime,
                                       boolean driverArrived, boolean passengerArrived) {
        try {
            Long driverId = currentDriver != null ? driverIdMap.get(currentDriver) : null;
            Timestamp timestamp = acceptTime != null ? Timestamp.valueOf(acceptTime) : null;

            rideRequestDAO.update(
                rideRequestId, driverId, status,
                request.getDistance(), request.getEstimatedTime(), request.getEstimatedPrice(),
                timestamp, driverArrived, passengerArrived
            );
            System.out.println("[DB] ride_request " + rideRequestId + " -> " + status);
        } catch (Exception e) {
            System.out.println("[DB] Update ride_request error: " + e.getMessage());
        }
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void markDriverArrived() {
        this.driverArrivedToPassenger = true;
        System.out.println(" Driver arrived to passenger location.");

        if (rideRequestId != -1 && acceptanceTime != null) {
            updateRideRequestInDB(Status.Accepted, acceptanceTime, true, false);
        }
    }

    public void markPassengerArrived() {
        this.passengerArrivedToDestination = true;
        System.out.println(" Passenger arrived to destination.");

        if (rideRequestId != -1 && acceptanceTime != null) {
            updateRideRequestInDB(Status.Accepted, acceptanceTime, true, true);
        }
    }


    // -------------------------------------------------------------------------------------------------------------------

    public void handleDelayPenalty(Driver driver, String offender) {

        Passenger passenger = request.getPassenger();

        System.out.println(" Delay Penalty Applied on: " + offender);

        double fineAmount = 10.0;
        if (offender.equalsIgnoreCase("driver")) {

            double wallet = driver.getWalletBalance();
            double credit = driver.getCreditBalance();

            if (wallet >= fineAmount) {
                driver.updateWalletBalance(wallet - fineAmount);
                System.out.println(" Driver fined from wallet: " + fineAmount + " EGP");
            } else if (credit >= fineAmount) {
                driver.updateCreditBalance(credit - fineAmount);
                System.out.println(" Driver fined from credit: " + fineAmount + " EGP");
            } else {
                driver.updateWalletBalance(wallet - fineAmount);
                System.out.println(" Wallet not enough → forcing negative balance!");
            }

        } else if (offender.equalsIgnoreCase("passenger")) {

            double wallet = passenger.getWalletBalance();
            double credit = passenger.getCreditBalance();

            if (wallet >= fineAmount) {
                passenger.updateWalletBalance(wallet - fineAmount);
                System.out.println(" Passenger fined from wallet: " + 10 + " EGP");
            } else if (credit >= fineAmount) {
                passenger.updateCreditBalance(credit - fineAmount);
                System.out.println(" Passenger fined from credit: " + fineAmount + " EGP");
            } else {
                passenger.updateWalletBalance(wallet - fineAmount);
                System.out.println(" Wallet not enough → forcing negative balance!");
            }
        }

        request.updateStatus(Status.Cancelled);
        System.out.println(" Ride cancelled due to delay.");
    }

    // -------------------------------------------------------------------------------------------------------------------

    public void checkForDelay(int actualArrivalTime) {
        if (request == null) {
            System.out.println("Cannot check delay — request is null!");
            return;
        }

        if (currentDriver == null) {
            System.out.println("Cannot check delay — no driver assigned yet!");
            return;
        }
        int estimated = request.getEstimatedTime();
        if (!driverArrivedToPassenger && actualArrivalTime > estimated + 10) {
            System.out.println(" Driver delayed arrival to passenger!");
            handleDelayPenalty(currentDriver, "driver");
            return;
        }
        if (driverArrivedToPassenger
                && request.getStatus() == Status.Accepted
                && actualArrivalTime > 10) {

            System.out.println(" Passenger did not show up after 10 minutes!");
            handleDelayPenalty(currentDriver, "passenger");
        }
    }
    // -------------------------------------------------------------------------------------------------------------------
    public void completeRide() {
        if (request.getStatus() == Status.Cancelled) {
            System.out.println(" Cannot complete a cancelled ride!");
            return;
        }

        if (!driverArrivedToPassenger || !passengerArrivedToDestination) {
            System.out.println(" Ride cannot complete before arrival!");
            return;
        }

        Passenger passenger = request.getPassenger();
        Driver driver = currentDriver;

        paymentProcessor.updateProcessPayment(passenger, driver);

        request.updateStatus(Status.Completed);

        System.out.println("\n Ride Completed Successfully!");
        System.out.println("Please rate each other to store history.");

        saveRideHistory();

        if (rideRequestId != -1 && acceptanceTime != null) {
            updateRideRequestInDB(Status.Completed, acceptanceTime, true, true);
        }

        if (rideRequestId != -1 && passengerIdMap != null && driverIdMap != null) {
            insertRideHistoryToDB();
        }
    }

    private void insertRideHistoryToDB() {
        try {
            Passenger passenger = request.getPassenger();
            Driver driver = currentDriver;
            Option options = paymentProcessor.getOptions();

            long rhId = rideHistoryDAO.insert(
                rideRequestId, driverIdMap.get(driver), passengerIdMap.get(passenger),
                driver.getLatestPassengerRating(), // passenger_rating = Driver's rating OF passenger
                passenger.getLatestDriverRating(), // driver_rating = Passenger's rating OF driver
                paymentProcessor.getAmount(), paymentProcessor.getPaymentMethod(),
                options != null ? options.getTips() : 0.0,
                options != null ? options.getDonationAmount() : 0.0,
                options != null ? options.getDonationOrganization() : ""
            );
            System.out.println("[DB] ride_history inserted id=" + rhId);
        } catch (Exception e) {
            System.out.println("[DB] Insert ride_history error: " + e.getMessage());
        }
    }
    
    // -------------------------------------------------------------------------------------------------------------------

    public void setPassengerWantsToRate(boolean value) {
        this.passengerWantsToRate = value;
    }

    public void setDriverWantsToRate(boolean value) {
        this.driverWantsToRate = value;
    }

    // -------------------------------------------------------------------------------------------------------------------

    public void setPassengerRatingValue(int value) {
        this.passengerRatingValue = value;
    }

    public void setDriverRatingValue(int value) {
        this.driverRatingValue = value;
    }
    
    // -------------------------------------------------------------------------------------------------------------------

    private void saveRideHistory() {
        if (request == null) {
            System.out.println("Cannot save ride history — request is null!");
            return;
        }
        Passenger passenger = request.getPassenger();
        Driver driver = currentDriver;

        if (driver == null) {
            System.out.println("Cannot save ride history — no driver assigned to this ride!");
            return;
        }

        if (passengerWantsToRate && passengerRatingValue > 0) {
            passenger.RateDriver(passengerRatingValue);
        }

        if (driverWantsToRate && driverRatingValue > 0) {
            driver.RatePassenger(driverRatingValue);
        }

        int driverRatingFromPassenger = passenger.getLatestDriverRating();
        int passengerRatingFromDriver = driver.getLatestPassengerRating();

        if (!passengerWantsToRate && !driverWantsToRate) {
            System.out.println("ℹ No ratings provided — history saved without ratings.");
        } else if (passengerWantsToRate && !driverWantsToRate) {
            System.out.println(" Passenger rated the driver (" + driverRatingFromPassenger + " stars)");
        } else if (!passengerWantsToRate && driverWantsToRate) {
            System.out.println(" Driver rated the passenger (" + passengerRatingFromDriver + " stars)");
        } else {
            System.out.println(" Both sides rated each other!");
        }

        RideHistory history = new RideHistory(
                driver,
                passenger,
                driverRatingFromPassenger,
                passengerRatingFromDriver,
                request
        );

        passenger.getRideHistory().add(history);
        driver.getRideHistory().add(history);

        System.out.println(" RideHistory Saved! (ID: " + history.getHistoryId() + ")");
    }
    
    // -------------------------------------------------------------------------------------------------------------------

    public Request getRequest() {
        return this.request;
    }

    public Driver getCurrentDriver() {
        return this.currentDriver;
    }

    public Payment getPaymentProcessor() {
        return paymentProcessor;
    }

    public LocalDateTime getAcceptanceTime() {
        return acceptanceTime;
    }

    public long getRideRequestId() {
        return rideRequestId;
    }
}


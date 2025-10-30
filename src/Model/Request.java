package com.mycompany.uper;

public class Request {

    private static int requestCounter = 1;
    private int requestId;
    private Passenger passenger;
    private Location origin;
    private Location destination;
    private Status status;
    private double distance;
    private int estimatedTime;
    private Payment payment;

    public Request(Passenger passenger, Location origin, Location destination,
                   double distance, int estimatedTime) {

        this.requestId = requestCounter++;
        this.passenger = passenger;
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.estimatedTime = estimatedTime;
        this.status = Status.Pending;
        this.payment = null;
    }

    public Request getRequest() {
        return this;
    }

    public int getRequestId() {
        return requestId;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location getDestination() {
        return destination;
    }

    public Status getStatus() {
        return status;
    }

    public double getDistance() {
        return distance;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public Payment getPayment() {
        return payment;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    // ✅ Updated to use Status ENUM
//    public void PaymentTransaction(Payment payment, Driver driver) {
//        if (payment != null) {
//            this.payment = payment;
//
//            boolean success = payment.processPayment(passenger, driver);
//
//            if (success) {
//                this.status = Status.Completed;
//                System.out.println("✅ Payment processed successfully for Request ID: " + requestId);
//            } else {
//                this.status = Status.PaymentFailed;
//                System.out.println("❌ Payment failed for Request ID: " + requestId);
//            }
//        }
//    }

    @Override
    public String toString() {
        return "Request{" +
                "requestId=" + requestId +
                ", passenger=" + passenger.getName() +
                ", origin=" + origin.getName() +
                ", destination=" + destination.getName() +
                ", status='" + status + '\'' +
                ", distance=" + distance +
                ", estimatedTime=" + estimatedTime +
                ", payment=" + (payment != null ? payment.getPaymentDetails() : "Not Paid") +
                '}';
    }
}
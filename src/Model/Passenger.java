package com.mycompany.uper;

import java.util.List;

public class Passenger extends Person {

    public Passenger(String userSSN, String name, String phoneNumber, String email, double walletBalance, double creditBalance, double accountRating, Location currentLocation, List<RideHistory> rideHistory) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating, currentLocation, rideHistory);
    }

    public void setWalletBalance(double walletBalance) {
        updateWalletBalance(walletBalance);
    }

    public void setCreditBalance(double creditBalance) {
        updateCreditBalance(creditBalance);
    }

    public void RateDriver(RideHistory hist, int rating) {
        if (rating >=1 && rating <=5) {
            hist.setDriverRating(rating);
            System.out.println("Driver rated with: " + rating + " stars");
        }
        else{
            System.out.println("invalid rating must be between 1 and 5");
        }
    }

    @Override
    public void showProfile() {
        System.out.println("Passenger Profile:");
        System.out.println("Name: " + getName());
        System.out.println("SSN: " + getUserSSN());
        System.out.println("Phone: " + getPhoneNumber());
        System.out.println("Email: " + getEmail());
        System.out.println("Wallet Balance: " + getWalletBalance());
        System.out.println("Credit Balance: " + getCreditBalance());
        System.out.println("Rating: " + getAccountRating());
    }
}



//    public ProblemReport ReportProblem(RideManager manager, Set<ProblemType> types, String details) {
//
//        if (manager == null || !manager.getRequest().getPassenger().equals(this)) {
//            System.out.println("ERROR: Cannot report problem for a ride not linked to this passenger.");
//            return null;
//        }
//
//        ProblemReport report = new ProblemReport(
//            manager,
//            types,
//            details
//        );
//
//        System.out.println("\n✅ Report Submitted!");
//        System.out.println("   Report ID: " + report.getReportId());
//        System.out.println("   Manager Linked.");
//
//        return report;
//    }
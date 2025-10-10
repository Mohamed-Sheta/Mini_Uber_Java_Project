/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.uper_project;

/**
 *
 * @author Mohamed
 */
public class Passenger extends Person{
//    private Location Currentlocation , Destination;
    public Passenger(String User_SSN, String name, String PhoneNumber, String Email, float WalletBalance, float creditBalance, int AccountRating) {
        super(User_SSN, name, PhoneNumber, Email, WalletBalance, creditBalance, AccountRating);
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
    
    public String getUser_SSN() {
        return User_SSN;
    }

    public void setUser_SSN(String User_SSN) {
        this.User_SSN = User_SSN;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String PhoneNumber) {
        this.PhoneNumber = PhoneNumber;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String Email) {
        this.Email = Email;
    }

    public int getAccountRating() {
        return AccountRating;
    }

    public void setAccountRating(int AccountRating) {
        this.AccountRating = AccountRating;
    }
    
    public float getWalletBalance() {
        return WalletBalance;
    }

    public void setWalletBalance(float WalletBalance) {
        this.WalletBalance = WalletBalance;
    }

    public float getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(float creditBalance) {
        this.creditBalance = creditBalance;
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.uper_project;

/**
 *
 * @author Mohamed
 */
public class Driver extends Person{
    protected String licensePlate;
    protected String carModel;
    protected boolean active;
//    protected Location currentLocation;

    public Driver(String licensePlate, String carModel, boolean active, String User_SSN, String name, String PhoneNumber, String Email, float WalletBalance, float creditBalance, int AccountRating) {
        super(User_SSN, name, PhoneNumber, Email, WalletBalance, creditBalance, AccountRating);
        this.licensePlate = licensePlate;
        this.carModel = carModel;
        this.active = active;
    }


    public double getWalletBalance() {
        return WalletBalance;
    }

    public double getCreditBalance() {
        return creditBalance;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getCarModel() {
        return carModel;
    }

    
//    public Location getCurrentLocation() {
//        return currentLocation;
//    }
//
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.uper_project;

/**
 *
 * @author Mohamed
 */
public class Person {
    protected String User_SSN , name , PhoneNumber , Email;
    protected float WalletBalance , creditBalance;
    int AccountRating;
    // ArrayList<RideHistory> history = new ArrayList<RideHistory>();
    
    public Person(String User_SSN, String name, String PhoneNumber, String Email, float WalletBalance, float creditBalance, int AccountRating) {
            this.User_SSN = User_SSN;
            this.name = name;
            this.PhoneNumber = PhoneNumber;
            this.Email = Email;
            this.WalletBalance = WalletBalance;
            this.creditBalance = creditBalance;
            this.AccountRating = AccountRating;
        }
    
    public void ShowProfile(){
        System.out.println("UserName : "+ name);
        System.out.println("Personal SSN : "+ User_SSN);
        System.out.println("Email : "+ Email);
        System.out.println("Phone Number : "+ PhoneNumber);
        System.out.println("Wallet Balance : "+ WalletBalance);
        System.out.println("Credit Balance : "+ creditBalance);
        System.out.println("Account Rating : "+ AccountRating);
    }
}
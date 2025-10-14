/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.mycompany.uper_project;

/**
 *
 * @author Mohamed
 */
public enum ProblemType {

    // -----Issues related to the Driver-----
    DRIVER_BEHAVIOR,    // Inappropriate conduct or language by the driver
    DRIVER_LATE,         // Driver arrived late for pickup
    RECKLESS_DRIVING,   // Unsafe or aggressive driving

    // -----Issues related to the Vehicle-----
    VEHICLE_CLEANLINESS,    // Poor cleanliness of the car
    TECHNICAL_ISSUE,        // Faulty A/C, broken lights, etc.

    // -----Financial and Administrative Issues-----
    FARE_DISPUTE,       // Disagreement over the fare amount or incorrect charges
    ACCOUNT_ISSUE,      // Problems with account balance or payment method

    // -----Other types-----
    OTHER_ISSUE     // Any other issue not covered by the above categories
}
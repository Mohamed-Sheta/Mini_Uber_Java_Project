package com.mycompany.uper;

public class Option {
    private double tips;
    private double donationAmount;
    private String donationOrganization;

    private boolean isTipsEnabled = false;
    private boolean isDonationEnabled = false;
    private boolean isRateEnabled = false;

    public Option() {
        tips = 0;
        donationAmount = 0;
        donationOrganization = "";
    }


    public void enableRating(boolean key) {
        this.isRateEnabled = key;
    }
    public void enableTips(boolean key) {
        this.isTipsEnabled = key;
    }

    public void enableDonation(boolean key) {
        this.isDonationEnabled = key;
    }

    public void giveDonation(double amount, String organization) {
        if (!isDonationEnabled) {
            System.out.println("❌ Donations are currently disabled.");
            return;
        }
        
        if (amount < 1) {
            System.out.println("❌ Invalid donation amount.");
            return;
        }
        
        this.donationAmount = amount;
        this.donationOrganization = organization == null ? "" : organization;
        
        System.out.println("✅ Donation added: " + amount + " to " + this.donationOrganization);
        System.out.println("ℹ️ Final deduction will happen during payment processing.");
    }
    
    public void giveTips(double amount) {
        if (!isTipsEnabled) {
            System.out.println("Tips feature is disabled.");
            return;
        }
        if (amount < 0) {
            System.out.println("⚠️ Invalid tips amount.");
            return;
        }
        this.tips = amount;
    }


    public double getTips() { return tips; }
    public double getDonationAmount() { return donationAmount; }
    public String getDonationOrganization() { return donationOrganization; }
}
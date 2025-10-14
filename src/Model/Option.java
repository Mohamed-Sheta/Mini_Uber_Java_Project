package Model;

public class Option {
    private float tips;
    private float donationAmount;
    private String donationOrganization;

    private boolean isTipsEnabled = false;
    private boolean isDonationEnabled = false;
    private boolean isRateEnabled = false;

    public void giveDonation(Passenger passenger, double amount, String organization, String source) {
        if (!isDonationEnabled) {
            System.out.println("❌ Donations are currently disabled.");
            return;
        }

        if (amount <1) {
            System.out.println("❌ Invalid donation amount.");
            return;
        }

        double total = passenger.getWalletBalance() + passenger.getCreditBalance();
        if (total < amount) {
            System.out.println("❌ Donation failed: Insufficient balance.");
            System.out.println("Available total balance: " + total);
            return;
        }

        passenger.reduceAmount(amount, source);

        this.donationAmount = (float) amount;
        this.donationOrganization = organization;

        System.out.println("✅ " + passenger.getName() + " donated " + amount + " to " + organization);
        System.out.println("Remaining wallet: " + passenger.getWalletBalance());
        System.out.println("Remaining credit: " + passenger.getCreditBalance());
    }


    public void enableTips(boolean key) {
        this.isTipsEnabled = key;
    }

    public void enableDonation(boolean key) {
        this.isDonationEnabled = key;
    }

    public void setTipsAmount(float amount) {
        if (isTipsEnabled) {
            this.tips = amount;
        } else {
            System.out.println("Tips feature is disabled.");
        }
    }

    public void enableRating(boolean key) {
        this.isRateEnabled = key;
    }

    public float getTips() { return tips; }
    public float getDonationAmount() { return donationAmount; }
    public String getDonationOrganization() { return donationOrganization; }
}

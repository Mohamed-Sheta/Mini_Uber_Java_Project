import Model.*;
import services.*;
import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.draw.LineSeparator;
public class Main {

    public static void generateInvoicePdf(String id, String name, double amount) {
        try {
            File dir = new File("resources/invoices");
            if (!dir.exists()) dir.mkdirs();

            String filename = "resources/invoices/invoice_" + id + ".pdf";
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();

            Font regularFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, BaseColor.BLACK);
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Font amountFont = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD, BaseColor.BLACK);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);

            File logoFile = new File("resources/Logo.jpg");
            if (logoFile.exists()) {
                Image logo = Image.getInstance(logoFile.getAbsolutePath());
                logo.scaleToFit(120, 120);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(new Paragraph(" "));
            }

            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            Paragraph datePara = new Paragraph(dateTime, regularFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingAfter(20);
            document.add(datePara);

            Paragraph title = new Paragraph("Your Trip Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph subtitle = new Paragraph("Thanks for riding with us, " + name, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            LineSeparator line = new LineSeparator();
            line.setLineColor(BaseColor.LIGHT_GRAY);
            document.add(new Chunk(line));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{1, 1});
            totalTable.setSpacingBefore(10);
            totalTable.setSpacingAfter(30);

            PdfPCell labelCell = new PdfPCell(new Phrase("Total", labelFont));
            labelCell.setBorder(PdfPCell.NO_BORDER);
            labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalTable.addCell(labelCell);

            PdfPCell amountCell = new PdfPCell(new Phrase(String.format("%.2f EGP", amount), amountFont));
            amountCell.setBorder(PdfPCell.NO_BORDER);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amountCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalTable.addCell(amountCell);

            document.add(totalTable);

            document.add(new Chunk(line));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{2, 1});
            summaryTable.setSpacingBefore(10);

            PdfPCell baseFareLabel = new PdfPCell(new Phrase("Base Fare", sectionFont));
            baseFareLabel.setBorder(PdfPCell.NO_BORDER);
            baseFareLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            baseFareLabel.setPaddingBottom(8);
            summaryTable.addCell(baseFareLabel);

            PdfPCell baseFareAmount = new PdfPCell(new Phrase(String.format("%.2f EGP", amount), sectionFont));
            baseFareAmount.setBorder(PdfPCell.NO_BORDER);
            baseFareAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            baseFareAmount.setPaddingBottom(8);
            summaryTable.addCell(baseFareAmount);

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total Amount", sectionFont));
            totalLabel.setBorder(PdfPCell.NO_BORDER);
            totalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            totalLabel.setPaddingTop(8);
            summaryTable.addCell(totalLabel);

            PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.format("%.2f EGP", amount), sectionFont));
            totalAmountCell.setBorder(PdfPCell.NO_BORDER);
            totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalAmountCell.setPaddingTop(8);
            summaryTable.addCell(totalAmountCell);

            document.add(summaryTable);

            document.close();
            System.out.println("Invoice PDF saved: " + filename);
        } catch (Exception e) {
            System.err.println("PDF generation failed: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        // =====================================================================
        // PASSWORD MIGRATION: Uncomment the lines below to migrate existing
        // plain-text passwords to SHA-256 hashes (safe to run multiple times)
        // =====================================================================
        // migratePasswords();
        // testMigratedLogin();
        // return; // Uncomment to only run migration without starting full system
        // =====================================================================

        System.out.println("=== Mini Uber System Egypt (Refactored Version) ===\n");

        Request.DatabaseInitializer dbInit = new Request.DatabaseInitializer();
        Map<ProblemType, Integer> problemTypeMap = dbInit.initialize(true); // true = reset DB

        MapGraph.CityMapSetup citySetup = new MapGraph.CityMapSetup();
        citySetup.initializeAll();

        MapGraph cityMap = citySetup.cityMap;
        List<Location> places = citySetup.locations;
        List<Driver> allDrivers = citySetup.drivers;
        List<Passenger> passengers = citySetup.passengers;
        Map<Driver, Long> driverId = citySetup.driverIdMap;
        Map<Passenger, Long> passengerId = citySetup.passengerIdMap;

        Location downtown = places.get(0);
        Location nasrCity = places.get(1);
        Location maadi = places.get(2);
        Location giza = places.get(3);
        Location newCairo = places.get(4);

        Passenger p1 = passengers.get(0);
        Passenger p2 = passengers.get(1);
        Passenger p3 = passengers.get(2);
        Passenger p4 = passengers.get(3);

        Driver d4 = allDrivers.get(3);

        Option optTipsDonate = new Option();
        optTipsDonate.enableTips(true);
        optTipsDonate.giveTips(10.0);
        optTipsDonate.enableDonation(true);
        optTipsDonate.giveDonation(5.0, "Charity Egypt");

        Option optBasic = new Option();
        optBasic.enableTips(false);
        optBasic.enableDonation(false);

        System.out.println("Payment options ready.\n");
        Runnable sep = () -> System.out.println("\n----------------------------------------\n");

        System.out.println("Test 1: Normal ride (Ahmed from Maadi -> Giza)");
        Request r1 = p1.request_ride(maadi, giza, cityMap);
        if (r1 != null) {
            Payment pay1 = new Payment(r1.getEstimatedPrice(), PaymentType.wallet, optTipsDonate);
            RideManager rm1 = new RideManager(allDrivers, r1, cityMap, pay1);
            rm1.setDatabaseMaps(passengerId, driverId);

            rm1.createRide();
            if (rm1.getCurrentDriver() != null) {
                rm1.markDriverArrived();
                rm1.markPassengerArrived();
                rm1.setPassengerWantsToRate(true);
                rm1.setPassengerRatingValue(5);
                rm1.setDriverWantsToRate(true);
                rm1.setDriverRatingValue(5);
                rm1.completeRide();

                Long pId = passengerId.get(p1);
                Long dId = driverId.get(rm1.getCurrentDriver());
                if (pId != null && dId != null) {
                    Request.submitProblemReport(rm1.getRideRequestId(), pId,
                            dId,
                            ProblemType.DRIVER_BEHAVIOR, "Driver was late 5 minutes.",
                            problemTypeMap);
                } else {
                    System.out.println("ERROR: Cannot submit problem report - passenger or driver ID not found in database.");
                }
            }
        }
        sep.run();

        System.out.println("Test 2: Low balance (Sara from Downtown -> Nasr City)");
        Request r2 = p2.request_ride(downtown, nasrCity, cityMap);
        if (r2 != null) {
            Payment pay2 = new Payment(r2.getEstimatedPrice(), PaymentType.credit, optBasic);
            RideManager rm2 = new RideManager(allDrivers, r2, cityMap, pay2);
            rm2.setDatabaseMaps(passengerId, driverId);

            rm2.createRide();
            if (rm2.getCurrentDriver() != null) {
                rm2.markDriverArrived();
                rm2.markPassengerArrived();
                rm2.setPassengerWantsToRate(true);
                rm2.setPassengerRatingValue(4);
                rm2.setDriverWantsToRate(true);
                rm2.setDriverRatingValue(5);
                rm2.completeRide();

                Long pId2 = passengerId.get(p2);
                Long dId2 = driverId.get(rm2.getCurrentDriver());
                if (pId2 != null && dId2 != null) {
                    Request.submitProblemReport(rm2.getRideRequestId(), pId2,
                            dId2,
                            ProblemType.FARE_DISPUTE, "Fare seems higher than expected.",
                            problemTypeMap);
                } else {
                    System.out.println("ERROR: Cannot submit problem report - passenger or driver ID not found in database.");
                }
            }
        }
        sep.run();

        System.out.println("Test 3: No path (Mona from New Cairo -> Maadi)");
        Request r3 = p4.request_ride(newCairo, maadi, cityMap);
        if (r3 == null) {
            System.out.println("No available path between New Cairo and Maadi (expected).\n");
        }
        sep.run();

        System.out.println("Test 4: No drivers available (simulate)");
        Request r4 = p3.request_ride(nasrCity, downtown, cityMap);
        if (r4 != null) {
            Payment pay4 = new Payment(r4.getEstimatedPrice(), PaymentType.wallet, optBasic);
            RideManager rm4 = new RideManager(new ArrayList<>(), r4, cityMap, pay4);
            rm4.setDatabaseMaps(passengerId, driverId);
            rm4.createRide();
        }
        sep.run();

        System.out.println("Test 5: Driver views and accepts pending ride requests");
        Queue<Request> rideQueue = new LinkedList<>();
        Request rq1 = p1.request_ride(maadi, downtown, cityMap);
        Request rq2 = p2.request_ride(downtown, giza, cityMap);
        Request rq3 = p3.request_ride(nasrCity, newCairo, cityMap);

        if (rq1 != null) rideQueue.add(rq1);
        if (rq2 != null) rideQueue.add(rq2);
        if (rq3 != null) rideQueue.add(rq3);

        d4.viewRideRequests(rideQueue);
        d4.Accept_Request(rideQueue);
        System.out.println("\nRemaining Requests After Acceptance:");
        d4.viewRideRequests(rideQueue);
        sep.run();

        System.out.println("Test 6: Small stress test (10 random rides)");
        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            Passenger px = passengers.get(rand.nextInt(passengers.size()));
            Location start = places.get(rand.nextInt(places.size()));
            Location end = places.get(rand.nextInt(places.size()));
            while (end == start) end = places.get(rand.nextInt(places.size()));

            Request rx = px.request_ride(start, end, cityMap);
            if (rx == null) continue;

            Payment pxPay = new Payment(
                    rx.getEstimatedPrice(),
                    (i % 2 == 0) ? PaymentType.wallet : PaymentType.credit,
                    (i % 3 == 0) ? optTipsDonate : optBasic
            );

            RideManager rmx = new RideManager(allDrivers, rx, cityMap, pxPay);
            rmx.setDatabaseMaps(passengerId, driverId);
            rmx.createRide();

            if (rmx.getCurrentDriver() != null) {
                rmx.markDriverArrived();
                rmx.markPassengerArrived();
                rmx.setPassengerWantsToRate(true);
                rmx.setPassengerRatingValue(5);
                rmx.setDriverWantsToRate(true);
                rmx.setDriverRatingValue(5);
                rmx.completeRide();
            }
        }
        sep.run();

        System.out.println("Test 7: Passenger cancels a ride");
        Request cancelReq = p1.request_ride(maadi, giza, cityMap);
        if (cancelReq != null) {
            Payment cancelPay = new Payment(cancelReq.getEstimatedPrice(), PaymentType.wallet, optBasic);
            RideManager cancelManager = new RideManager(allDrivers, cancelReq, cityMap, cancelPay);
            cancelManager.setDatabaseMaps(passengerId, driverId);
            cancelManager.createRide();

            System.out.println("\n>>> Passenger decides to cancel the ride...");
            p1.cancelRide(cancelManager);

            System.out.println("\nAfter cancellation:");
            System.out.println("Passenger Wallet: " + p1.getWalletBalance() + " EGP");
            if (allDrivers.size() > 0) {
                System.out.println("Driver Wallet: " + allDrivers.get(0).getWalletBalance() + " EGP");
            }
        }
        sep.run();

        System.out.println("Test 8: Count Completed Rides");
        List<RideHistory> allHistories = new ArrayList<>();
        allHistories.addAll(p1.getRideHistory());
        allHistories.addAll(p2.getRideHistory());
        allHistories.addAll(p3.getRideHistory());
        allHistories.addAll(p4.getRideHistory());
        int completedCount = RideHistory.getRideCounts(allHistories);
        System.out.println(" Total completed rides in the system: " + completedCount);
        sep.run();

        System.out.println("Test 9: Generate PDF Invoice");
        generateInvoicePdf("INV001", "Ahmed Ali", 150.50);

        System.out.println("\n=== ALL DONE ===");
    }
}


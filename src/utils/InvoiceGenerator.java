package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.*;
import java.time.*;
import java.time.format.*;


public class InvoiceGenerator {

    // Uber-style Dark Theme Color Palette
    private static final BaseColor DARK_BACKGROUND = new BaseColor(18, 18, 18);      // Near black background
    private static final BaseColor CARD_BACKGROUND = new BaseColor(28, 28, 30);      // Dark card background
    private static final BaseColor PRIMARY_TEXT = new BaseColor(255, 255, 255);      // Pure white for primary text
    private static final BaseColor SECONDARY_TEXT = new BaseColor(174, 174, 178);    // Light gray for secondary text
    private static final BaseColor ACCENT_GREEN = new BaseColor(48, 209, 88);        // Green for discounts/savings
    private static final BaseColor BRAND_ACCENT = new BaseColor(0, 188, 212);        // Cyan/teal brand accent
    private static final BaseColor DIVIDER_DARK = new BaseColor(44, 44, 46);         // Subtle dark divider
    private static final BaseColor HIGHLIGHT_BG = new BaseColor(38, 38, 40);         // Highlighted section background

    public static String generateInvoicePdf(String invoiceId, String passengerName, double amount) throws Exception {
        // Create invoices directory if it doesn't exist
        File dir = new File("resources/invoices");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = "resources/invoices/invoice_" + invoiceId + ".pdf";

        // Set A4 page size with margins optimized for dark theme
        Rectangle pageSize = new Rectangle(PageSize.A4);
        Document document = new Document(pageSize, 40, 40, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // Create dark background for entire page
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.setColorFill(DARK_BACKGROUND);
        canvas.rectangle(0, 0, pageSize.getWidth(), pageSize.getHeight());
        canvas.fill();

        // Define Uber-style typography hierarchy
        Font brandFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BRAND_ACCENT);
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, PRIMARY_TEXT);
        Font dateFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, SECONDARY_TEXT);
        Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, SECONDARY_TEXT);
        Font totalAmountFont = new Font(Font.FontFamily.HELVETICA, 48, Font.BOLD, PRIMARY_TEXT);
        Font sectionHeaderFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, PRIMARY_TEXT);
        Font bodyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, PRIMARY_TEXT);
        Font bodySecondaryFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, SECONDARY_TEXT);
        Font discountFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, ACCENT_GREEN);
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, SECONDARY_TEXT);

        // ===== HEADER SECTION =====
        // Add MiniGo logo (centered)
        File logoFile = new File("resources/Logo-removebg-preview.png");
        boolean logoAdded = false;
        if (logoFile.exists()) {
            try {
                Image logo = Image.getInstance(logoFile.getAbsolutePath());
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                logoAdded = true;
            } catch (Exception e) {
                System.out.println("Logo file found but could not be loaded, using text branding");
            }
        }

        if (!logoAdded) {
            // Fallback: MiniGo text branding
            Paragraph brandName = new Paragraph("MiniGo", brandFont);
            brandName.setAlignment(Element.ALIGN_CENTER);
            brandName.setSpacingAfter(5);
            document.add(brandName);
        }

        document.add(new Paragraph(" ", footerFont)); // Spacer

        // Trip Receipt title
        Paragraph title = new Paragraph("Trip Receipt", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8);
        document.add(title);

        // Date and time
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy • HH:mm"));
        Paragraph datePara = new Paragraph(dateTime, dateFont);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(30);
        document.add(datePara);

        // ===== TOTAL SECTION (Uber-style prominent display) =====
        // Container with dark card background
        PdfPTable totalContainer = new PdfPTable(1);
        totalContainer.setWidthPercentage(100);
        totalContainer.setSpacingBefore(10);
        totalContainer.setSpacingAfter(10);

        // Inner content: "Total" label + large amount
        PdfPTable totalContent = new PdfPTable(1);
        totalContent.setWidthPercentage(100);

        // "Total" label
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", totalLabelFont));
        totalLabelCell.setBorder(PdfPCell.NO_BORDER);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalLabelCell.setPaddingTop(20);
        totalLabelCell.setPaddingBottom(5);
        totalLabelCell.setBackgroundColor(CARD_BACKGROUND);
        totalContent.addCell(totalLabelCell);

        // Large total amount
        PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.format("%.2f EGP", amount), totalAmountFont));
        totalAmountCell.setBorder(PdfPCell.NO_BORDER);
        totalAmountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalAmountCell.setPaddingBottom(20);
        totalAmountCell.setBackgroundColor(CARD_BACKGROUND);
        totalContent.addCell(totalAmountCell);

        // Wrap in container
        PdfPCell containerCell = new PdfPCell(totalContent);
        containerCell.setBorder(PdfPCell.NO_BORDER);
        containerCell.setBackgroundColor(CARD_BACKGROUND);
        containerCell.setPadding(0);
        totalContainer.addCell(containerCell);

        document.add(totalContainer);

        // Accent divider line (brand color)
        LineSeparator accentLine = new LineSeparator();
        accentLine.setLineColor(BRAND_ACCENT);
        accentLine.setLineWidth(2f);
        Chunk accentChunk = new Chunk(accentLine);
        Paragraph accentPara = new Paragraph(accentChunk);
        accentPara.setSpacingAfter(25);
        document.add(accentPara);

        // ===== FARE BREAKDOWN SECTION =====
        // Section header
        Paragraph fareHeader = new Paragraph("Fare Breakdown", sectionHeaderFont);
        fareHeader.setSpacingAfter(15);
        document.add(fareHeader);

        // Breakdown table (2 columns: label | amount)
        PdfPTable fareTable = new PdfPTable(2);
        fareTable.setWidthPercentage(100);
        fareTable.setWidths(new float[]{3, 1});
        fareTable.setSpacingAfter(20);

        // Trip Fare
        addFareRow(fareTable, "Trip Fare", amount, bodyFont, bodyFont, false);

        // Subtotal (same as trip fare for now)
        addFareRow(fareTable, "Subtotal", amount, bodySecondaryFont, bodySecondaryFont, false);

        // Note: If you have wait time, discounts, promotions, or rounding adjustments,
        // add them here using addFareRow() method

        document.add(fareTable);

        // Subtle divider
        addDarkDivider(document);

        // ===== PAYMENT SECTION =====
        Paragraph paymentHeader = new Paragraph("Payment", sectionHeaderFont);
        paymentHeader.setSpacingAfter(15);
        document.add(paymentHeader);

        // Payment details table
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        paymentTable.setWidths(new float[]{3, 1});
        paymentTable.setSpacingAfter(25);

        // Payment method (Wallet)
        addFareRow(paymentTable, "Wallet", amount, bodyFont, bodyFont, false);

        // Payment time
        String paymentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        PdfPCell timeLabel = new PdfPCell(new Phrase("Payment time", bodySecondaryFont));
        timeLabel.setBorder(PdfPCell.NO_BORDER);
        timeLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        timeLabel.setPaddingBottom(8);
        paymentTable.addCell(timeLabel);

        PdfPCell timeValue = new PdfPCell(new Phrase(paymentTime, bodySecondaryFont));
        timeValue.setBorder(PdfPCell.NO_BORDER);
        timeValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        timeValue.setPaddingBottom(8);
        paymentTable.addCell(timeValue);

        document.add(paymentTable);

        // Final divider
        addDarkDivider(document);

        // ===== FOOTER SECTION =====
        document.add(new Paragraph(" ", footerFont));

        // Thank you message
        Paragraph thankYou = new Paragraph("Thank you for riding with MiniGo, " + passengerName + "!", bodySecondaryFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        thankYou.setSpacingAfter(15);
        document.add(thankYou);

        // Invoice ID
        Paragraph invoiceIdPara = new Paragraph("Invoice ID: " + invoiceId, footerFont);
        invoiceIdPara.setAlignment(Element.ALIGN_CENTER);
        document.add(invoiceIdPara);

        document.close();
        System.out.println("✓ Uber-style dark theme invoice generated: " + filename);

        return filename;
    }

    // ===== HELPER METHODS =====

    /**
     * Add a fare row to the breakdown table (label on left, amount on right)
     */
    private static void addFareRow(PdfPTable table, String label, double amount,
                                   Font labelFont, Font amountFont, boolean isDiscount) {
        // Label cell
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPaddingBottom(8);
        table.addCell(labelCell);

        // Amount cell (use green font for discounts)
        String amountText = isDiscount ?
            String.format("-%.2f EGP", Math.abs(amount)) :
            String.format("%.2f EGP", amount);
        Font displayFont = isDiscount ?
            new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(48, 209, 88)) :
            amountFont;

        PdfPCell amountCell = new PdfPCell(new Phrase(amountText, displayFont));
        amountCell.setBorder(PdfPCell.NO_BORDER);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPaddingBottom(8);
        table.addCell(amountCell);
    }

    /**
     * Add a subtle dark divider line
     */
    private static void addDarkDivider(Document document) throws DocumentException {
        LineSeparator divider = new LineSeparator();
        divider.setLineColor(new BaseColor(44, 44, 46));
        divider.setLineWidth(0.5f);
        Paragraph dividerPara = new Paragraph(new Chunk(divider));
        dividerPara.setSpacingBefore(5);
        dividerPara.setSpacingAfter(20);
        document.add(dividerPara);
    }
}


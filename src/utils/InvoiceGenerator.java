package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.*;
import java.time.*;
import java.time.format.*;

public class InvoiceGenerator {


    public static String generateInvoicePdf(String invoiceId, String passengerName, double amount) throws Exception {
        // Create invoices directory if it doesn't exist
        File dir = new File("resources/invoices");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = "resources/invoices/invoice_" + invoiceId + ".pdf";
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();

        // Define fonts
        Font regularFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, BaseColor.BLACK);
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
        Font amountFont = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD, BaseColor.BLACK);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);

        // Add logo if exists
        File logoFile = new File("resources/Logo.jpg");
        if (logoFile.exists()) {
            Image logo = Image.getInstance(logoFile.getAbsolutePath());
            logo.scaleToFit(120, 120);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
            document.add(new Paragraph(" "));
        }

        // Add date and time
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        Paragraph datePara = new Paragraph(dateTime, regularFont);
        datePara.setAlignment(Element.ALIGN_RIGHT);
        datePara.setSpacingAfter(20);
        document.add(datePara);

        // Add title
        Paragraph title = new Paragraph("Your Trip Receipt", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Add subtitle with passenger name
        Paragraph subtitle = new Paragraph("Thanks for riding with us, " + passengerName, subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Add separator line
        LineSeparator line = new LineSeparator();
        line.setLineColor(BaseColor.LIGHT_GRAY);
        document.add(new Chunk(line));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // Add total amount table (large display)
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

        // Add separator line
        document.add(new Chunk(line));
        document.add(new Paragraph(" "));

        // Add summary breakdown
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{2, 1});
        summaryTable.setSpacingBefore(10);

        // Base Fare row
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

        // Total Amount row
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

        // Add invoice ID at bottom
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph invoiceIdPara = new Paragraph("Invoice ID: " + invoiceId, regularFont);
        invoiceIdPara.setAlignment(Element.ALIGN_CENTER);
        document.add(invoiceIdPara);

        document.close();
        System.out.println("Invoice PDF saved: " + filename);

        return filename;
    }
}


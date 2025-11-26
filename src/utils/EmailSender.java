package utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * Email sender utility for sending PDF invoices
 */
public class EmailSender {

    // ============================================================
    // 📧 EMAIL CONFIGURATION - USE YOUR PERSONAL GMAIL FOR TESTING
    // ============================================================
    //
    // TO USE YOUR OWN GMAIL ACCOUNT:
    // 1. Replace SENDER_EMAIL with your Gmail address
    // 2. Generate App Password for your account:
    //    - Go to: https://myaccount.google.com/apppasswords
    //    - Enable 2-Step Verification if not already enabled
    //    - Generate App Password for "Mail"
    //    - Copy the 16-character code (remove spaces)
    // 3. Replace SENDER_PASSWORD with your App Password
    // 4. Rebuild project and test
    //
    // Example:
    // private static final String SENDER_EMAIL = "yourname@gmail.com";
    // private static final String SENDER_PASSWORD = "abcdefghijklmnop";
    // ============================================================
    
    // Gmail SMTP configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    // ⚠️ UPDATE THESE WITH YOUR GMAIL CREDENTIALS:
    private static final String SENDER_EMAIL = "minigorides.official@gmail.com"; // ← Your Gmail address
    private static final String SENDER_PASSWORD = "dpuq boji fuuf nyly";

    // ============================================================

    /**
     * Send email with PDF attachment
     *
     * @param recipientEmail Recipient email address
     * @param recipientName Recipient name
     * @param subject Email subject
     * @param body Email body
     * @param pdfFilePath Path to PDF file to attach
     * @return true if email sent successfully, false otherwise
     */
    public static boolean sendInvoiceEmail(String recipientEmail, String recipientName,
                                          String subject, String body, String pdfFilePath) {

        System.out.println("[EMAIL] Attempting to send invoice to: " + recipientEmail);

        // Check if email credentials are configured
        if (SENDER_EMAIL.equals("your-email@gmail.com") || SENDER_PASSWORD.equals("your-app-password-here")) {
            System.out.println("[EMAIL] ⚠️ Email credentials not configured");
            System.out.println("[EMAIL] ℹ️ To enable email sending:");
            System.out.println("[EMAIL]    1. Edit src/utils/EmailSender.java");
            System.out.println("[EMAIL]    2. Replace SENDER_EMAIL with your Gmail address");
            System.out.println("[EMAIL]    3. Replace SENDER_PASSWORD with your Gmail App Password");
            System.out.println("[EMAIL]    4. See PERSONAL_GMAIL_SETUP.md for detailed instructions");
            return false;
        }

        // Configure mail properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // SSL trust configuration to fix certificate validation issues
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.smtp.ssl.checkserveridentity", "false");

        // Create authenticator
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        };

        try {
            // Create session
            Session session = Session.getInstance(props, auth);

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "MiniGO Egypt"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);

            // Create multipart message for text + attachment
            Multipart multipart = new MimeMultipart();

            // Add text body
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body, "UTF-8", "html");
            multipart.addBodyPart(textPart);

            // Add PDF attachment if file exists
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(pdfFile);
                attachmentPart.setFileName(pdfFile.getName());
                multipart.addBodyPart(attachmentPart);
                System.out.println("[EMAIL] PDF attachment added: " + pdfFile.getName());
            } else {
                System.err.println("[EMAIL] WARNING: PDF file not found: " + pdfFilePath);
            }

            // Set content
            message.setContent(multipart);

            // Send email
            Transport.send(message);

            System.out.println("[EMAIL] ✅ Invoice email sent successfully to " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EMAIL] ❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate HTML email body for ride invoice
     */
    public static String generateInvoiceEmailBody(String passengerName, double amount, String invoiceId) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "                 color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
            "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }\n" +
            "        .amount { font-size: 32px; font-weight: bold; color: #667eea; margin: 20px 0; }\n" +
            "        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>🚗 MiniGO Egypt</h1>\n" +
            "            <p>Your Trip Receipt</p>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <p>Dear " + passengerName + ",</p>\n" +
            "            <p>Thank you for riding with MiniGO! Here is your trip receipt.</p>\n" +
            "            \n" +
            "            <div style=\"text-align: center;\">\n" +
            "                <p style=\"color: #666;\">Total Amount</p>\n" +
            "                <div class=\"amount\">" + String.format("%.2f", amount) + " EGP</div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <p>Invoice ID: <strong>" + invoiceId + "</strong></p>\n" +
            "            <p>Your detailed invoice is attached as a PDF file.</p>\n" +
            "            \n" +
            "            <hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">\n" +
            "            \n" +
            "            <p style=\"color: #666; font-size: 14px;\">\n" +
            "                If you have any questions about this receipt, please contact our support team.\n" +
            "            </p>\n" +
            "            \n" +
            "            <div style=\"text-align: center;\">\n" +
            "                <p style=\"margin-top: 30px;\">\n" +
            "                    <strong>Thank you for choosing MiniGO!</strong><br>\n" +
            "                    Safe travels! 🛣️\n" +
            "                </p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"footer\">\n" +
            "            <p>© 2025 MiniGO Egypt. All rights reserved.</p>\n" +
            "            <p>This is an automated message, please do not reply.</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";
    }
}


package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * EmailService — all outgoing emails for FinTech Portal.
 *
 * ── SETUP (do this once) ──────────────────────────────────────────────
 * 1. Go to myaccount.google.com → Security → App passwords
 * 2. Create one named "FinTech", copy the 16-char password
 * 3. Paste it into APP_PASSWORD below (never commit it to git)
 * ─────────────────────────────────────────────────────────────────────
 *
 * Emails provided:
 *   sendVerificationEmail()   — 6-digit code after registration
 *   sendPasswordResetEmail()  — 6-digit code for forgot password
 *   sendLoginNotification()   — security alert after each login
 */
public class EmailService {

    private static final String SENDER_EMAIL = "yasmine912003@gmail.com";
    private static final String APP_PASSWORD = "lnpn eawa dwhw basw";
    private static final String SENDER_NAME  = "FinTech Portal";

    // brand colours — match your existing UI
    private static final String PRIMARY  = "#1E3A8A";
    private static final String DARK     = "#111827";
    private static final String LIGHT_BG = "#F3F4F6";

    // ── session ───────────────────────────────────────────────────────

    private static Session buildSession() {
        Properties p = new Properties();
        p.put("mail.smtp.host",              "smtp.gmail.com");
        p.put("mail.smtp.port",              "587");
        p.put("mail.smtp.auth",              "true");
        p.put("mail.smtp.starttls.enable",   "true");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout",           "10000");

        return Session.getInstance(p, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
    }

    // ── internal send ─────────────────────────────────────────────────

    private static void send(String to, String subject, String html) {
        // Run on a background thread so the UI never freezes waiting for SMTP
        new Thread(() -> {
            try {
                MimeMessage msg = new MimeMessage(buildSession());
                msg.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                msg.setSubject(subject, "UTF-8");

                MimeBodyPart body = new MimeBodyPart();
                body.setContent(html, "text/html; charset=UTF-8");

                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(body);
                msg.setContent(mp);

                Transport.send(msg);
                System.out.println("[Email] ✓ Sent to " + to);
            } catch (Exception e) {
                System.err.println("[Email] ✗ Failed to " + to + ": " + e.getMessage());
            }
        }).start();
    }

    // ── public methods ────────────────────────────────────────────────

    /** Call after successful registration — user enters this code to verify. */
    public static void sendVerificationEmail(String toEmail, String userName, String code) {
        send(toEmail, "Your FinTech Portal verification code",
                verificationHtml(userName, code));
    }

    /** Call when user submits forgot-password form — user enters this to reset. */
    public static void sendPasswordResetEmail(String toEmail, String userName, String code) {
        send(toEmail, "Reset your FinTech Portal password",
                passwordResetHtml(userName, code));
    }

    /** Call after every successful login as a security notification. */
    public static void sendLoginNotification(String toEmail, String userName) {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm"));
        send(toEmail, "New sign-in to your FinTech Portal account",
                loginNotifHtml(userName, time));
    }

    // ── HTML templates ────────────────────────────────────────────────

    private static String wrap(String content) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:" + LIGHT_BG + ";"
                + "font-family:Arial,sans-serif;'>"
                + "<div style='max-width:560px;margin:40px auto;'>"
                + "<div style='background:" + PRIMARY + ";padding:28px;text-align:center;"
                + "border-radius:12px 12px 0 0;'>"
                + "<h1 style='color:white;margin:0;font-size:22px;'>💳 FinTech Portal</h1></div>"
                + "<div style='background:white;padding:36px;border-radius:0 0 12px 12px;"
                + "box-shadow:0 4px 12px rgba(0,0,0,0.08);'>"
                + content
                + "<p style='color:" + DARK + ";font-size:15px;margin-top:28px;'>"
                + "— The FinTech Portal team</p></div>"
                + "<p style='text-align:center;color:#9CA3AF;font-size:12px;margin-top:16px;'>"
                + "© " + LocalDateTime.now().getYear() + " FinTech Portal. All rights reserved.</p>"
                + "</div></body></html>";
    }

    private static String codeBox(String code) {
        return "<div style='background:" + LIGHT_BG + ";border:2px dashed " + PRIMARY + ";"
                + "border-radius:8px;text-align:center;padding:24px;margin:28px 0;'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:8px;color:"
                + PRIMARY + ";'>" + code + "</span></div>";
    }

    private static String verificationHtml(String name, String code) {
        return wrap(
                "<h2 style='color:" + DARK + ";margin-top:0;'>Verify your account</h2>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>Hi <strong>" + name + "</strong>,</p>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>Use the code below to activate "
                        + "your account. It expires in <strong>30 minutes</strong>.</p>"
                        + codeBox(code)
                        + "<p style='color:#6B7280;font-size:13px;'>If you didn't create an account, "
                        + "ignore this email.</p>"
        );
    }

    private static String passwordResetHtml(String name, String code) {
        return wrap(
                "<h2 style='color:" + DARK + ";margin-top:0;'>Password reset request</h2>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>Hi <strong>" + name + "</strong>,</p>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>We received a request to reset "
                        + "your password. Use the code below — it expires in <strong>15 minutes</strong>.</p>"
                        + codeBox(code)
                        + "<p style='color:#DC2626;font-size:14px;'>⚠ If you didn't request this, "
                        + "change your password immediately.</p>"
        );
    }

    private static String loginNotifHtml(String name, String time) {
        return wrap(
                "<h2 style='color:" + DARK + ";margin-top:0;'>New sign-in detected</h2>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>Hi <strong>" + name + "</strong>,</p>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>A new sign-in to your account "
                        + "was detected.</p>"
                        + "<div style='background:" + LIGHT_BG + ";border-left:4px solid " + PRIMARY + ";"
                        + "padding:14px 18px;border-radius:4px;margin:20px 0;'>"
                        + "<p style='margin:0;color:" + DARK + ";'><strong>Time:</strong> " + time + "</p></div>"
                        + "<p style='color:" + DARK + ";font-size:15px;'>If this wasn't you, "
                        + "change your password immediately.</p>"
        );
    }
}

package com.kyoto.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // --- STORE EMAILS (SINGLE ITEM - Legacy/Fallback) ---
    public void sendOrderConfirmation(String toEmail, String orderId, String productName, double total) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your.kyoto.email@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Kyoto Music: Order Confirmation (" + orderId + ")");
        String body = "Thank you for your purchase!\n\nOrder ID: " + orderId + "\nItem: " + productName + "\nTotal Paid: $" + String.format("%.2f", total) + "\n\nYour support keeps the music alive. Enjoy!\n- The Kyoto Team";
        message.setText(body);
        mailSender.send(message);
    }

    public void sendArtistNotification(String artistEmail, String productName, double price) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your.kyoto.email@gmail.com");
        message.setTo(artistEmail);
        message.setSubject("Ka-Ching! You made a sale on Kyoto 🎵");
        String body = "Great news!\n\nA fan just purchased your product: " + productName + ".\nRevenue: $" + String.format("%.2f", price) + "\n\nKeep up the amazing work!\n- The Kyoto Team";
        message.setText(body);
        mailSender.send(message);
    }

    // --- CART EMAILS (MULTI-ITEM) ---
    public void sendCartOrderConfirmation(String toEmail, String orderId, String itemsList, double total) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your.kyoto.email@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Kyoto Music: Order Confirmation (" + orderId + ")");
        String body = "Thank you for your purchase!\n\nOrder ID: " + orderId + "\n\nItems Ordered:\n" + itemsList + "\nTotal Paid (incl. delivery): $" + String.format("%.2f", total) + "\n\nYour support keeps the music alive. Enjoy!\n- The Kyoto Team";
        message.setText(body);
        mailSender.send(message);
    }

    // --- EVENT TICKET EMAILS ---
    public void sendTicketConfirmation(String toEmail, String orderId, String eventTitle, int quantity, double total, String ticketCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your.kyoto.email@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Kyoto Music: Your Tickets for " + eventTitle + " 🎟️");
        String body = "Get ready for the show!\n\nOrder ID: " + orderId + "\nEvent: " + eventTitle + "\nQuantity: " + quantity + " Ticket(s)\nTotal Paid: $" + String.format("%.2f", total) + "\n\n🎫 YOUR TICKET CODE: " + ticketCode + "\n\nPlease present this code at the door for entry.\n- The Kyoto Team";
        message.setText(body);
        mailSender.send(message);
    }

    public void sendArtistTicketNotification(String artistEmail, String eventTitle, int quantity, double totalAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your.kyoto.email@gmail.com");
        message.setTo(artistEmail);
        message.setSubject("Ticket Sold! 🎟️ - " + eventTitle);
        String body = "Great news!\n\nA fan just bought " + quantity + " ticket(s) to your upcoming event: " + eventTitle + ".\nRevenue: $" + String.format("%.2f", totalAmount) + "\n\nKeep up the momentum!\n- The Kyoto Team";
        message.setText(body);
        mailSender.send(message);
    }
}
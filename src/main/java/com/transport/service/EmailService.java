package com.transport.service;

import com.transport.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(User user, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Account Verification - Car Rental System");
        message.setText("Dear " + user.getFirstName() + ",\n\n" +
                "Please click the following link to verify your account:\n" +
                "http://localhost:8080/verify?token=" + token + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Best regards,\nCar Rental Team");

        mailSender.send(message);
    }

    public void sendBookingConfirmation(User user, Long bookingId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Booking Confirmation - Car Rental System");
        message.setText("Dear " + user.getFirstName() + ",\n\n" +
                "Your booking #" + bookingId + " has been confirmed.\n\n" +
                "Please complete the payment within 24 hours to secure your reservation.\n\n" +
                "Best regards,\nCar Rental Team");

        mailSender.send(message);
    }
}
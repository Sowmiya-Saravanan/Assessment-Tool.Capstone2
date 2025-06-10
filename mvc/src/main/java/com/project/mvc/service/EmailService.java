package com.project.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendClassInvitationEmail(String toEmail, String className, String classCode, String educatorEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Invitation to Join Class: " + className);
            helper.setFrom("noreply@assesscraft.com");

            String emailContent = "<h2>Class Invitation</h2>" +
                                 "<p>Dear Student,</p>" +
                                 "<p>You have been invited to join the class <strong>" + className + "</strong> by educator <strong>" + educatorEmail + "</strong>.</p>" +
                                 "<p>Please use the following class code to join the class on AssessCraft:</p>" +
                                 "<h3>Class Code: " + classCode + "</h3>" +
                                 "<p>Steps to join:</p>" +
                                 "<ol>" +
                                 "<li>Log in to your AssessCraft student account.</li>" +
                                 "<li>Go to the Student Dashboard.</li>" +
                                 "<li>Enter the class code in the 'Join a Class' section.</li>" +
                                 "</ol>" +
                                 "<p>We look forward to seeing you in class!</p>" +
                                 "<p>Best regards,<br>AssessCraft Team</p>";

            helper.setText(emailContent, true); // true indicates HTML content
            mailSender.send(message);
            logger.info("Class invitation email sent to: {} for class: {}", toEmail, className);
        } catch (MessagingException e) {
            logger.error("Failed to send class invitation email to: {}. Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
package com.company.amsbackend.application.impl;

import com.company.amsbackend.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.hr.email}")
    private String hrEmail;

    @Override
    public void sendInviteEmail(String toEmail, String employeeName, String temporaryPassword) {
        log.info("START sendInviteEmail | toEmail: {} | employeeName: {}", toEmail, employeeName);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to MetaUpSpce AMS");
            helper.setText(
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                            "<p>Dear " + employeeName + ",</p>" +
                            "<p>Welcome to <strong>MetaUpSpace LLP</strong>!</p>" +
                            "<p>Your account for the <strong>MetaUpSpace AMS portal</strong> has been successfully created.</p>" +
                            "<p><strong>Login Credentials:</strong><br>" +
                            "Email: <strong>" + toEmail + "</strong><br>" +
                            "Password: <strong>" + temporaryPassword + "</strong></p>" +
                            "<p>Please log in at your earliest convenience and update your password for security purposes.</p>" +
                            "<p>Best regards,<br>" +
                            "HR Team<br>" +
                            "MetaUpSpace LLP</p>" +
                            "</body>" +
                            "</html>",
                    true
            );

            mailSender.send(message);
            log.info("Invite email sent | toEmail: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invite email | toEmail: {} | error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send invite email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendMonthlyTimesheetReport(String toEmail, String ccMail, String employeeName, String month, int year, byte[] excelData, String filename) {
        log.info("START sendMonthlyTimesheetReport | toEmail: {} | ccMail: {} | employeeName: {} | month: {} | year: {}", toEmail, ccMail, employeeName, month, year);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setCc(ccMail);
            helper.setSubject(String.format("Monthly Timesheet for %s - %s %d", employeeName, month, year));
            helper.setText(
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif;'>" +
                            "<h2>Monthly Timesheet Report</h2>" +
                            "<p>Hello!</p>" +
                            "<p>Please find attached the monthly timesheet for <strong>" + employeeName + "</strong> for " +
                            month + " " + year + ".</p>" +
                            "<p>Best regards,<br>MetaUpSpace AMS</p>" +
                            "</body>" +
                            "</html>",
                    true
            );

            helper.addAttachment(filename, new ByteArrayResource(excelData));

            mailSender.send(message);
            log.info("Monthly timesheet report sent | employeeName: {} | month: {} | year: {}", employeeName, month, year);
        } catch (MessagingException e) {
            log.error("Failed to send monthly timesheet report | employeeName: {} | error: {}", employeeName, e.getMessage(), e);
            throw new RuntimeException("Failed to send monthly timesheet report: " + e.getMessage(), e);
        }
    }
}
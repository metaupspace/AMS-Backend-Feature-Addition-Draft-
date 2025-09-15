package com.company.amsbackend.application.service;

public interface EmailService {
    void sendInviteEmail(String toEmail, String employeeName, String temporaryPassword);
    void sendMonthlyTimesheetReport(String toEmail, String ccMail, String employeeName, String month, int year, byte[] excelData, String filename);
}
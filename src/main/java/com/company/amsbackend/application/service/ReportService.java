package com.company.amsbackend.application.service;

import java.time.YearMonth;

public interface ReportService {
    byte[] generateEmployeeMonthlyTimesheet(String employeeId, YearMonth yearMonth);
}
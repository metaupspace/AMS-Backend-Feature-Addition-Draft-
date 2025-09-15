package com.company.amsbackend.application.service;

import java.time.YearMonth;

public interface ScheduledReportService {
    void generateAndSendMonthlyReports();
    void generateAndSendMonthlyReportsForMonth(YearMonth yearMonth);
}
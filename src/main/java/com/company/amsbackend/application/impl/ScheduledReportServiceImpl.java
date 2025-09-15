package com.company.amsbackend.application.impl;

import com.company.amsbackend.application.service.EmailService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.application.service.ReportService;
import com.company.amsbackend.application.service.ScheduledReportService;
import com.company.amsbackend.domain.entity.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportServiceImpl implements ScheduledReportService {

    private final EmailService emailService;
    private final EmployeeService employeeService;
    private final ReportService reportService;

    @Value("${app.hr.email}")
    private String hrEmail;

    @Scheduled(cron = "0 5 12 1 * ?" , zone = "Asia/Kolkata")
    @Override
    public void generateAndSendMonthlyReports() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("Scheduled: Generating monthly reports | month: {}", previousMonth);

        generateAndSendMonthlyReportsForMonth(previousMonth);
    }

    @Override
    public void generateAndSendMonthlyReportsForMonth(YearMonth yearMonth) {
        log.info("START generateAndSendMonthlyReportsForMonth | yearMonth: {}", yearMonth);

        try {
            List<Employee> employees = employeeService.getAllEmployees();
            log.info("Employees found | count: {}", employees.size());

            String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            int year = yearMonth.getYear();

            for (Employee employee : employees) {
                try {
                    if (!employee.isActive()) {
                        log.info("Skipping inactive employee | employeeId: {}", employee.getEmployeeId());
                        continue;
                    }

                    log.info("Generating timesheet | employeeId: {}", employee.getEmployeeId());
                    byte[] excelData = reportService.generateEmployeeMonthlyTimesheet(
                            employee.getEmployeeId(), yearMonth);

                    String sanitizedName = employee.getName().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                    String filename = String.format("%s_timesheet_%s_%d.xlsx",
                            sanitizedName, monthName.toLowerCase(), year);

                    emailService.sendMonthlyTimesheetReport(
                            hrEmail,
                            employee.getEmail(),
                            employee.getName(),
                            monthName,
                            year,
                            excelData,
                            filename);

                    log.info("Timesheet sent | employeeId: {}", employee.getEmployeeId());
                } catch (Exception e) {
                    log.error("Error for employee | employeeId: {} | error: {}", employee.getEmployeeId(), e.getMessage(), e);
                }
            }

            log.info("END generateAndSendMonthlyReportsForMonth | yearMonth: {}", yearMonth);
        } catch (Exception e) {
            log.error("Error while generating monthly reports | yearMonth: {} | error: {}", yearMonth, e.getMessage(), e);
        }
    }
}
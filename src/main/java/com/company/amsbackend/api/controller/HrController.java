package com.company.amsbackend.api.controller;

import com.company.amsbackend.api.dto.DailyActivityDto;
import com.company.amsbackend.api.dto.EmployeeCreateRequest;
import com.company.amsbackend.api.dto.EmployeeSummaryResponse;
import com.company.amsbackend.api.dto.MonthlyReportRequest;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.application.service.ReportService;
import com.company.amsbackend.application.service.ScheduledReportService;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.enums.EmployeeRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;
    private final ScheduledReportService scheduledReportService;

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    @PostMapping("/employee")
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody EmployeeCreateRequest req, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Create employee request | email: {} | IP: {} | Device: {}", req.getEmail(), ip, userAgent);

        String uniqueEmployeeId = employeeService.generateUniqueEmployeeId();

        EmployeeRole role = EmployeeRole.valueOf(req.getRole());
        Employee employee = Employee.builder()
                .employeeId(uniqueEmployeeId)
                .name(req.getName())
                .email(req.getEmail())
                .contact(req.getContact())
                .role(role)
                .position(req.getPosition())
                .address(req.getAddress())
                .build();
        var saved = employeeService.createEmployee(employee, req.getPassword());

        log.info("Employee created | employeeId: {} | email: {} | IP: {} | Device: {}", saved.getEmployeeId(), saved.getEmail(), ip, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeSummaryResponse>> getAllEmployees(HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Get all employees request | IP: {} | Device: {}", ip, userAgent);

        List<EmployeeSummaryResponse> employees = employeeService.getAllEmployeesWithSummary();
        log.info("All employees fetched | count: {} | IP: {} | Device: {}", employees.size(), ip, userAgent);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/activity/daily")
    public ResponseEntity<List<DailyActivityDto>> getDailyActivity(@RequestParam String date, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Get daily activity | date: {} | IP: {} | Device: {}", date, ip, userAgent);

        LocalDate targetDate = LocalDate.parse(date);
        List<DailyActivityDto> activities = attendanceService.getDailyActivities(targetDate);
        log.info("Daily activity fetched | date: {} | activitiesCount: {} | IP: {} | Device: {}", date, activities.size(), ip, userAgent);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/reports/employee/{employeeId}/timesheet")
    public ResponseEntity<Resource> downloadEmployeeTimesheet(
            @PathVariable String employeeId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest httpRequest) {

        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Download timesheet | employeeId: {} | year: {} | month: {} | IP: {} | Device: {}", employeeId, year, month, ip, userAgent);

        Employee employee = employeeService.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        YearMonth yearMonth = YearMonth.of(year, month);

        byte[] excelData = reportService.generateEmployeeMonthlyTimesheet(employeeId, yearMonth);

        String monthName = Month.of(month).toString().toLowerCase();
        String sanitizedName = employee.getName().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String filename = String.format("%s_timesheet_%s_%d.xlsx", sanitizedName, monthName, year);

        log.info("Timesheet generated | employeeId: {} | filename: {} | IP: {} | Device: {}", employeeId, filename, ip, userAgent);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelData.length)
                .body(new ByteArrayResource(excelData));
    }

    @PostMapping("/reports/generate-monthly")
    public ResponseEntity<Map<String, String>> generateMonthlyReports(@RequestBody MonthlyReportRequest request, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Manual monthly report generation | year: {} | month: {} | IP: {} | Device: {}", request.getYear(), request.getMonth(), ip, userAgent);

        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());

        new Thread(() -> {
            try {
                scheduledReportService.generateAndSendMonthlyReportsForMonth(yearMonth);
                log.info("Monthly report generation completed | yearMonth: {} | IP: {} | Device: {}", yearMonth, ip, userAgent);
            } catch (Exception e) {
                log.error("Error during async report generation | IP: {} | Device: {} | Error: {}", ip, userAgent, e.getMessage(), e);
            }
        }).start();

        log.info("Monthly report generation started | yearMonth: {} | IP: {} | Device: {}", yearMonth, ip, userAgent);
        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Monthly report generation has been started. Reports will be emailed to HR shortly.",
                        "yearMonth", yearMonth.toString()
                ));
    }

    @PostMapping("/{employeeId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateEmployee(@PathVariable String employeeId, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Deactivate employee | employeeId: {} | IP: {} | Device: {}", employeeId, ip, userAgent);
        if(employeeService.deactivateEmployee(employeeId)) {
            log.info("Employee deactivated | employeeId: {} | IP: {} | Device: {}", employeeId, ip, userAgent);
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "message", "Employee Account Deactivated Successfully"
                    ));
        }else{
            log.warn("Failed to deactivate employee | employeeId: {} | IP: {} | Device: {}", employeeId, ip, userAgent);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Could not deactivate employee."
                    ));
        }
    }
}
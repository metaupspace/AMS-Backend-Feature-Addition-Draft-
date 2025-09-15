package com.company.amsbackend.api.controller;

import com.company.amsbackend.api.dto.PasswordChangeRequest;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    @GetMapping("/profile")
    public ResponseEntity<Employee> getProfile(Authentication auth, HttpServletRequest httpRequest) {
        String email = auth.getName();
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Profile fetch | email: {} | IP: {} | Device: {}", email, ip, userAgent);

        Employee employee = employeeService.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Profile not found | email: {} | IP: {} | Device: {}", email, ip, userAgent);
                    return new DomainException("Employee not found");
                });

        log.info("Profile fetch success | employeeId: {} | email: {} | IP: {} | Device: {}", employee.getEmployeeId(), email, ip, userAgent);
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication auth,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {

        String email = auth.getName();
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Password change request | email: {} | IP: {} | Device: {}", email, ip, userAgent);

        employeeService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());

        log.info("Password change success | email: {} | IP: {} | Device: {}", email, ip, userAgent);
        return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
    }
}
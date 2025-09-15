package com.company.amsbackend.api.controller;

import com.company.amsbackend.api.dto.*;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.infrastructure.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getIp(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        log.info("Login attempt | email: {} | IP: {} | Device: {}", request.getEmail(), ip, userAgent);

        var employeeOpt = employeeService.findByEmail(request.getEmail());
        if (employeeOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), employeeOpt.get().getPasswordHash()) || !employeeOpt.get().isActive()) {
            log.warn("Login failed | email: {} | IP: {} | Device: {}", request.getEmail(), ip, userAgent);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var employee = employeeOpt.get();
        var token = jwtUtils.generateJwtToken(employee.getEmail(), employee.getRole().name());
        log.info("Login success | employeeId: {} | email: {} | IP: {} | Device: {}", employee.getEmployeeId(), employee.getEmail(), ip, userAgent);
        return ResponseEntity.ok(new AuthResponse(token, employee.getRole().name(), employee.getEmployeeId(), employee.getEmail()));
    }
}
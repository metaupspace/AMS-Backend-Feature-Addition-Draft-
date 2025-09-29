package com.company.amsbackend.api.controller;

import com.company.amsbackend.api.dto.AttendanceEditRequestDto;

import com.company.amsbackend.api.dto.PasswordChangeRequest;
import com.company.amsbackend.application.service.AttendanceEditRequestService;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.AttendanceEditRequest;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.enums.RequestStatus;
import com.company.amsbackend.domain.exception.AttendanceNotFoundException;
import com.company.amsbackend.domain.exception.DomainException;
import com.company.amsbackend.domain.exception.EmployeeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


import java.util.*;

@Slf4j
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceservice;
    


    private final AttendanceEditRequestService attendanceEditRequestService;

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty())
            ip = request.getRemoteAddr();
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

        log.info("Profile fetch success | employeeId: {} | email: {} | IP: {} | Device: {}", employee.getEmployeeId(),
                email, ip, userAgent);
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

    @PostMapping("/attendance-requests")
    public ResponseEntity<AttendanceEditRequestDto> createAttendanceRequest(
        Authentication auth,
        @Valid @RequestBody AttendanceEditRequestDto requestDto,
        HttpServletRequest httpRequest) {
    
    String email = auth.getName();
    String ip = getIp(httpRequest);
    String userAgent = getUserAgent(httpRequest);
    
    
    
    try {
        requestDto.setEmployeeId(employeeService.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Employee not found for email: {} | IP: {} | Device: {}",
                            email, ip, userAgent);
                    return new EmployeeNotFoundException(email);
                }).getEmployeeId());

        log.info("Employee ID set to: {}", requestDto.getEmployeeId());
        
        AttendanceEditRequestDto editAttendanceRequest = attendanceEditRequestService.createRequest(requestDto);
        
        try{
            Attendance attendance = attendanceservice.findById(requestDto.getAttendanceId())
                                    .orElseThrow(()-> new AttendanceNotFoundException(requestDto.getAttendanceId()));
            attendance.setEditRequestStatus(RequestStatus.PENDING);
            System.out.println(editAttendanceRequest.getId());
            attendance.setEditRequestId(editAttendanceRequest.getId());

            attendanceservice.save(attendance);
            log.info("Attendance record {} marked with PENDING edit request status", 
                    requestDto.getAttendanceId());
        }
        catch(Exception e){
            log.error("failed to update attendance request for ID {} | Error {}", requestDto.getAttendanceId(), e.getMessage());
        }
        
        
        return ResponseEntity.ok(editAttendanceRequest);
        
    } catch (Exception e) {
        log.error("=== ATTENDANCE EDIT REQUEST ERROR ===");
        log.error("Error creating attendance request | Email: {} | IP: {} | Device: {} | Error: {}", 
                email, ip, userAgent, e.getMessage(), e);
        throw e;
    }
}

    @GetMapping("/my-requests")
    public ResponseEntity<List<AttendanceEditRequest>> getMyRequests(Authentication auth,
            HttpServletRequest httpRequest) {
        String email = auth.getName();
        log.info("Fetch attendance edit requests | email: {} | IP: {}", email, getIp(httpRequest));

       
        String employeeId = employeeService.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFoundException(email))
                .getEmployeeId();

        List<AttendanceEditRequest> requests = attendanceEditRequestService.getRequestsByEmployeeId(employeeId);

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<AttendanceEditRequest> getRequestById(
            Authentication auth,
            @PathVariable String requestId,
            HttpServletRequest httpRequest) {

        String email = auth.getName();
        log.info("Fetch specific attendance edit request | employee: {} | requestId: {} | IP: {}", email, requestId,
                getIp(httpRequest));

        AttendanceEditRequest request = attendanceEditRequestService.getRequestById(requestId);

        return ResponseEntity.ok(request);
    }

}
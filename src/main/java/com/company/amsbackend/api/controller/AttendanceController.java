package com.company.amsbackend.api.controller;

import com.company.amsbackend.api.dto.AbsenceReportDto;
import com.company.amsbackend.api.dto.AgendaCompletionDto;
import com.company.amsbackend.api.dto.AgendaWithStatusDto;
import com.company.amsbackend.api.dto.AttendanceEditRequestDto;
import com.company.amsbackend.api.dto.AttendanceResponseDto;
import com.company.amsbackend.api.dto.CheckInRequest;
import com.company.amsbackend.api.dto.CheckOutRequest;
import com.company.amsbackend.application.service.AttendanceEditRequestService;
import com.company.amsbackend.application.service.AttendanceService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.AttendanceEditRequest;
import com.company.amsbackend.domain.enums.RequestStatus;
import com.company.amsbackend.domain.exception.AttendanceNotFoundException;
import com.company.amsbackend.domain.exception.EmployeeNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

        private final AttendanceService attendanceService;
        private final AttendanceEditRequestService attendanceEditRequestService;
        private final EmployeeService employeeService;

        private String getIp(HttpServletRequest request) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty())
                        ip = request.getRemoteAddr();
                return ip;
        }

        private String getUserAgent(HttpServletRequest request) {
                return request.getHeader("User-Agent");
        }

        @PostMapping("/check-in")
        public ResponseEntity<AttendanceResponseDto> checkIn(@RequestBody CheckInRequest request,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Received check-in | employeeId: {} | IP: {} | Device: {} | Location: {} | Agendas: {}",
                                request.getEmployeeId(), ip, userAgent, request.getLocation(), request.getAgendas());
                AttendanceResponseDto response = attendanceService.checkIn(
                                request.getEmployeeId(),
                                request.getAgendas(),
                                request.getLocation());
                log.info("Check-in processed | employeeId: {} | AttendanceId: {}",
                                request.getEmployeeId(),
                                response.getAttendance() != null ? response.getAttendance().getId() : "null");
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{attendanceId}/agendas")
        public ResponseEntity<List<AgendaWithStatusDto>> getAgendas(@PathVariable String attendanceId,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get agendas | attendanceId: {} | IP: {} | Device: {}", attendanceId, ip, userAgent);
                List<AgendaWithStatusDto> agendas = attendanceService.getAgendasForAttendance(attendanceId);
                return ResponseEntity.ok(agendas);
        }

        @PostMapping("/check-out")
        public ResponseEntity<AttendanceResponseDto> checkOut(@RequestBody CheckOutRequest request,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Received check-out | employeeId: {} | IP: {} | Device: {} | Remark: {} | Reference: {}",
                                request.getEmployeeId(), ip, userAgent, request.getRemark(),
                                request.getReferenceLink());

                Map<String, Boolean> agendaCompletions = request.getAgendaCompletions().stream()
                                .collect(Collectors.toMap(
                                                AgendaCompletionDto::getAgendaId,
                                                AgendaCompletionDto::isComplete));

                AttendanceResponseDto response = attendanceService.checkOut(
                                request.getEmployeeId(),
                                agendaCompletions,
                                request.getRemark(),
                                request.getReferenceLink());
                log.info("Check-out processed | employeeId: {} | AttendanceId: {}",
                                request.getEmployeeId(),
                                response.getAttendance() != null ? response.getAttendance().getId() : "null");
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{employeeId}/active")
        public ResponseEntity<AttendanceResponseDto> getActiveSession(@PathVariable String employeeId,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get active session | employeeId: {} | IP: {} | Device: {}", employeeId, ip, userAgent);
                AttendanceResponseDto response = attendanceService.getActiveSession(employeeId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{employeeId}/records")
        public ResponseEntity<List<Attendance>> getRecords(
                        @PathVariable String employeeId,
                        @RequestParam(required = false) String from,
                        @RequestParam(required = false) String to,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get attendance records | employeeId: {} | from: {} | to: {} | IP: {} | Device: {}",
                                employeeId, from, to, ip, userAgent);

                LocalDate fromDate = (from != null) ? LocalDate.parse(from) : null;
                LocalDate toDate = (to != null) ? LocalDate.parse(to) : null;

                List<Attendance> records = attendanceService.getAttendancesForEmployee(employeeId, fromDate, toDate);
                return ResponseEntity.ok(records);
        }

        @GetMapping("/monthly/{employeeId}")
        public ResponseEntity<List<Attendance>> getMonthlyRecords(
                        @PathVariable String employeeId,
                        @RequestParam int year,
                        @RequestParam int month,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get monthly records | employeeId: {} | year: {} | month: {} | IP: {} | Device: {}",
                                employeeId, year, month, ip, userAgent);

                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.plusMonths(1).minusDays(1);

                List<Attendance> records = attendanceService.getAttendancesForEmployee(employeeId, startDate, endDate);
                return ResponseEntity.ok(records);
        }

        @GetMapping("/daily/{employeeId}")
        public ResponseEntity<List<Attendance>> getDailyAttendanceByEmployee(
                        @PathVariable String employeeId,
                        @RequestParam String date,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get daily attendance | employeeId: {} | date: {} | IP: {} | Device: {}",
                                employeeId, date, ip, userAgent);

                LocalDate targetDate = LocalDate.parse(date);
                List<Attendance> records = attendanceService.getAttendancesForEmployee(
                                employeeId, targetDate, targetDate);

                return ResponseEntity.ok(records);
        }

        @GetMapping("/monthly/absent/{employeeId}")
        public ResponseEntity<AbsenceReportDto> getMonthlyAbsenceReport(
                        @PathVariable String employeeId,
                        @RequestParam int year,
                        @RequestParam int month,
                        HttpServletRequest httpRequest) {
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Get monthly absence report | employeeId: {} | year: {} | month: {} | IP: {} | Device: {}",
                                employeeId, year, month, ip, userAgent);

                AbsenceReportDto response = attendanceService.getMonthlyAbsenceReport(employeeId, year, month);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/attendance-requests")
        public ResponseEntity<AttendanceEditRequestDto> createAttendanceRequest(
                        Authentication auth,
                        @Valid @RequestBody AttendanceEditRequestDto requestDto,
                        HttpServletRequest httpRequest) {

                String email = auth.getName();
                String ip = getIp(httpRequest);
                String userAgent = getUserAgent(httpRequest);
                log.info("Create attendance edit request | email: {} | IP: {} | Device: {}", email, ip, userAgent);
                try {
                        requestDto.setEmployeeId(employeeService.findByEmail(email)
                                        .orElseThrow(() -> {
                                                log.error("Employee not found for email: {} | IP: {} | Device: {}",
                                                                email, ip, userAgent);
                                                return new EmployeeNotFoundException(email);
                                        }).getEmployeeId());

                        log.info("Employee ID set to: {}", requestDto.getEmployeeId());

                        AttendanceEditRequestDto editAttendanceRequest = attendanceEditRequestService
                                        .createRequest(requestDto);

                        try {
                                Attendance attendance = attendanceService.findById(requestDto.getAttendanceId())
                                                .orElseThrow(() -> new AttendanceNotFoundException(
                                                                requestDto.getAttendanceId()));
                                attendance.setEditRequestStatus(RequestStatus.PENDING);
                                System.out.println(editAttendanceRequest.getId());
                                attendance.setEditRequestId(editAttendanceRequest.getId());

                                attendanceService.save(attendance);
                                log.info("Attendance record {} marked with PENDING edit request status",
                                                requestDto.getAttendanceId());
                        } catch (Exception e) {
                                log.error("failed to update attendance request for ID {} | Error {}",
                                                requestDto.getAttendanceId(), e.getMessage());
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

}
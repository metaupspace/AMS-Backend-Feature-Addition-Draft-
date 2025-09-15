package com.company.amsbackend.application.impl;

import com.company.amsbackend.api.dto.EmployeeSummaryResponse;
import com.company.amsbackend.application.service.EmailService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.domain.entity.Attendance;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.infrastructure.repository.AttendanceRepository;
import com.company.amsbackend.infrastructure.repository.EmployeeRepository;
import com.company.amsbackend.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public Employee createEmployee(Employee employee, String password) {
        log.info("START createEmployee | email: {}", employee.getEmail());
        if (employeeRepository.findByEmail(employee.getEmail()).isPresent()) {
            log.warn("Employee with email already exists | email: {}", employee.getEmail());
            throw new DomainException("Employee with email already exists");
        }
        employee.setActive(true);
        employee.setPasswordHash(passwordEncoder.encode(password));
        if (employee.getEmployeeId() == null) {
            employee.setEmployeeId(generateUniqueEmployeeId());
        }

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created | employeeId: {} | email: {}", savedEmployee.getEmployeeId(), savedEmployee.getEmail());

        try {
            emailService.sendInviteEmail(employee.getEmail(), employee.getName(), password);
            log.info("Invite email sent | email: {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Failed to send invite email | email: {} | error: {}", employee.getEmail(), e.getMessage(), e);
        }

        log.info("END createEmployee | employeeId: {}", savedEmployee.getEmployeeId());
        return savedEmployee;
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        log.debug("findByEmail called | email: {}", email);
        return employeeRepository.findByEmail(email);
    }

    @Override
    public Optional<Employee> findByEmployeeId(String employeeId) {
        log.debug("findByEmployeeId called | employeeId: {}", employeeId);
        return employeeRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<Employee> getAllEmployees() {
        log.debug("getAllEmployees called");
        return employeeRepository.findAll();
    }

    @Override
    public List<EmployeeSummaryResponse> getAllEmployeesWithSummary() {
        log.info("START getAllEmployeesWithSummary");
        long startTime = System.currentTimeMillis();

        List<Employee> employees = employeeRepository.findAll();
        if (employees.isEmpty()) {
            log.info("No employees found");
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);

        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDateTime todayEndDateTime = today.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Attendance> allMonthlyAttendances = attendanceRepository.findAttendanceSummaryBetween(
                monthStartDateTime, todayEndDateTime);

        log.debug("Monthly attendance records found | count: {}", allMonthlyAttendances.size());

        Set<String> validEmployeeIds = employees.stream()
                .map(Employee::getEmployeeId)
                .collect(Collectors.toSet());

        Map<String, List<Attendance>> attendancesByEmployee = allMonthlyAttendances.stream()
                .filter(attendance -> attendance.getEmployeeId() != null && validEmployeeIds.contains(attendance.getEmployeeId()))
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));

        List<EmployeeSummaryResponse> summaries = employees.parallelStream()
                .map(employee -> buildEmployeeSummary(employee, attendancesByEmployee, weekStart, monthStart, today))
                .sorted(Comparator.comparing(EmployeeSummaryResponse::getEmployeeId))
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("END getAllEmployeesWithSummary | count: {} | durationMs: {}", employees.size(), (endTime - startTime));
        return summaries;
    }

    private EmployeeSummaryResponse buildEmployeeSummary(Employee employee,
                                                         Map<String, List<Attendance>> attendancesByEmployee,
                                                         LocalDate weekStart, LocalDate monthStart, LocalDate today) {

        List<Attendance> employeeAttendances = attendancesByEmployee.getOrDefault(
                employee.getEmployeeId(), Collections.emptyList());

        List<Attendance> completedSessions = employeeAttendances.stream()
                .filter(a -> a.getCheckInTime() != null && a.getCheckOutTime() != null && !a.isActiveSession())
                .toList();

        long weeklyMinutes = completedSessions.stream()
                .filter(a -> !a.getCheckInTime().toLocalDate().isBefore(weekStart))
                .mapToLong(this::calculateMinutesWorked)
                .sum();

        long monthlyMinutes = completedSessions.stream()
                .mapToLong(this::calculateMinutesWorked)
                .sum();

        List<LocalDate> presentDates = completedSessions.stream()
                .map(a -> a.getCheckInTime().toLocalDate()).distinct().sorted(LocalDate::compareTo).collect(Collectors.toList());

        int totalWorkingDaysThisMonth = today.getDayOfMonth();

        return EmployeeSummaryResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .email(employee.getEmail())
                .contact(employee.getContact())
                .role(employee.getRole())
                .active(employee.isActive())
                .currentWeekMinutes(weeklyMinutes)
                .currentMonthMinutes(monthlyMinutes)
                .presentDaysThisMonth(presentDates.size())
                .totalWorkingDaysThisMonth(totalWorkingDaysThisMonth)
                .presentDates(presentDates)
                .build();
    }

    private long calculateMinutesWorked(Attendance attendance) {
        if (attendance.getMinutesWorked() != null && attendance.getMinutesWorked() > 0) {
            return attendance.getMinutesWorked();
        }
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            return Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
        }
        return 0;
    }

    @Override
    @Transactional
    public boolean deactivateEmployee(String employeeId) {
        log.info("START deactivateEmployee | employeeId: {}", employeeId);
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new DomainException("Employee not found"));
        employee.setActive(false);
        Employee employeeSaved = employeeRepository.save(employee);
        log.info("Employee deactivated | employeeId: {}", employeeId);
        return (employeeSaved.getEmployeeId().equals(employeeId) && !employeeSaved.isActive());
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        log.info("START changePassword | email: {}", email);
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("Employee not found"));

        if (!passwordEncoder.matches(currentPassword, employee.getPasswordHash())) {
            log.warn("Incorrect current password | email: {}", email);
            throw new DomainException("Current password is incorrect");
        }

        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        log.info("Password changed | email: {}", email);
    }

    @Override
    public String generateUniqueEmployeeId() {
        log.info("START generateUniqueEmployeeId");
        String prefix = "EMP-";
        int count = (int) employeeRepository.count() + 1;
        String empId = prefix + String.format("%04d", count);

        while (employeeRepository.findByEmployeeId(empId).isPresent()) {
            count++;
            empId = prefix + String.format("%04d", count);
        }
        log.info("Generated unique employeeId: {}", empId);
        return empId;
    }
}
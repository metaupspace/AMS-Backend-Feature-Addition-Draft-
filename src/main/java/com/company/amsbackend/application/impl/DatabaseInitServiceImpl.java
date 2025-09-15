package com.company.amsbackend.application.impl;

import com.company.amsbackend.application.service.DatabaseInitService;
import com.company.amsbackend.application.service.EmployeeService;
import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.enums.EmployeeRole;
import com.company.amsbackend.infrastructure.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class DatabaseInitServiceImpl implements DatabaseInitService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @Value("${app.hr.email}")
    private String hrEmail;

    @Value("${app.hr.name}")
    private String hrName;

    @Value("${app.hr.contact}")
    private String hrContact;

    @PostConstruct
    @Override
    public void initializeDatabase() {
        // Create default HR user if not exists
        if (employeeRepository.findByEmail(hrEmail).isEmpty()) {
            Employee hrUser = Employee.builder()
                    .employeeId("HR-0001")
                    .name(hrName)
                    .email(hrEmail)
                    .contact(hrContact)
                    .role(EmployeeRole.HR)
                    .active(true)
                    .build();

            try {
                employeeService.createEmployee(hrUser, "hr123");
                System.out.println("Default HR user created successfully");
            } catch (Exception e) {
                System.err.println("Failed to create default HR user: " + e.getMessage());
            }
        }
    }
}
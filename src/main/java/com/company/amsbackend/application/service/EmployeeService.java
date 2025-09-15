package com.company.amsbackend.application.service;

import com.company.amsbackend.api.dto.EmployeeSummaryResponse;
import com.company.amsbackend.domain.entity.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {
    Employee createEmployee(Employee employee, String password);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmployeeId(String employeeId);
    List<Employee> getAllEmployees();
    List<EmployeeSummaryResponse> getAllEmployeesWithSummary();
//    void updateEmployee(Employee employee);
    boolean deactivateEmployee(String employeeId);
    void changePassword(String email, String currentPassword, String newPassword);
    String generateUniqueEmployeeId();
}
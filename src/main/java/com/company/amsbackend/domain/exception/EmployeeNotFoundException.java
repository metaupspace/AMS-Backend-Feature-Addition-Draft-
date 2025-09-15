package com.company.amsbackend.domain.exception;

public class EmployeeNotFoundException extends DomainException {
    public EmployeeNotFoundException(String employeeId) {
        super("Employee not found with ID: " + employeeId);
    }
}
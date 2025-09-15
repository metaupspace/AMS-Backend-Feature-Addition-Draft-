package com.company.amsbackend.api.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class EmployeeCreateRequest {
    // employeeId removed, will be auto-generated
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Contact is required")
    private String contact;

    @NotNull(message = "Role is required")
    private String role;

    // Not mandatory
    private String position;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Password is required")
    private String password;
}
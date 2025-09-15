package com.company.amsbackend.domain.entity;

import com.company.amsbackend.domain.enums.EmployeeRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "employees")
@CompoundIndex(def = "{'email': 1}", unique = true)
@CompoundIndex(def = "{'employeeId': 1}", unique = true)
public class Employee {
    @Id
    private String id;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Contact is required")
    private String contact;

    @NotNull(message = "Role is required")
    private EmployeeRole role;

    private String position; // can be null

    @NotBlank(message = "Address is required")
    private String address;

    private String passwordHash;
    private boolean active;
}
package com.company.amsbackend.api.dto;

import com.company.amsbackend.domain.entity.Employee;
import com.company.amsbackend.domain.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponse {
    private String employeeId;
    private String name;
    private String email;
    private String contactNumber;
    private EmployeeRole role;
    private String position;
    private String address;
    private boolean active;

    // Static method to convert Employee entity to EmployeeResponse DTO
    public static EmployeeResponse fromEntity(Employee employee) {
        return EmployeeResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .email(employee.getEmail())
                .contactNumber(employee.getContact())
                .role(employee.getRole())
                .position(employee.getPosition())
                .address(employee.getAddress())
                .active(employee.isActive())
                .build();
    }
}